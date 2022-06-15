package kz.kbtu.idcarddetectionlibrary

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.common.base.Objects
import kz.kbtu.idcarddetectionlibrary.camera.CameraSource
import kz.kbtu.idcarddetectionlibrary.camera.CameraSourcePreview
import kz.kbtu.idcarddetectionlibrary.camera.GraphicOverlay
import kz.kbtu.idcarddetectionlibrary.camera.WorkflowModel
import kz.kbtu.idcarddetectionlibrary.camera.WorkflowModel.WorkflowState
import kz.kbtu.idcarddetectionlibrary.objectDetection.ProminentObjectProcessor
import kz.kbtu.idcarddetectionlibrary.productsearch.FlowType
import kz.kbtu.idcarddetectionlibrary.utils.FileHelper
import kz.kbtu.idcarddetectionlibrary.utils.PreferenceUtils
import kz.kbtu.objectdetectionlibrary.viewExtensions.enabled
import java.io.ByteArrayOutputStream
import java.io.IOException


abstract class ObjectDetectionActivity : AppCompatActivity(), OnClickListener {
    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var flashButton: View? = null
    private var promptChip: Chip? = null
    private var promptChipAnimator: AnimatorSet? = null
    private var takePhotoButton: ExtendedFloatingActionButton? = null
    private var buttonsLayout: LinearLayout? = null
    private var takePhotoAgainButton: AppCompatButton? = null
    private var confirmPhotoButton: AppCompatButton? = null
    private var searchButtonAnimator: AnimatorSet? = null
    private var workflowModel: WorkflowModel? = null
    private var currentWorkflowState: WorkflowState? = null
    private var objectThumbnailForBottomSheet: Bitmap? = null

    private var filePath: String? = null
    private var currentFlowType: FlowType = FlowType.DOUBLE_SIDE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_live_object)
        setDetectionFlowType(FlowType.DOUBLE_SIDE)
        preview = findViewById(R.id.camera_preview)
        graphicOverlay = findViewById<GraphicOverlay>(R.id.camera_preview_graphic_overlay).apply {
            setOnClickListener(this@ObjectDetectionActivity)
            cameraSource = CameraSource(this)
        }
        promptChip = findViewById(R.id.bottom_prompt_chip)
        promptChipAnimator =
            (AnimatorInflater.loadAnimator(
                this,
                R.animator.bottom_prompt_chip_enter
            ) as AnimatorSet).apply {
                setTarget(promptChip)
            }
        takePhotoButton = findViewById<ExtendedFloatingActionButton>(R.id.take_photo_button).apply {
            setOnClickListener(this@ObjectDetectionActivity)
        }
        takePhotoButton?.enabled(false)
        buttonsLayout = findViewById(R.id.buttons_layout)
        takePhotoAgainButton = findViewById<AppCompatButton>(R.id.take_again_button).apply {
            setOnClickListener(this@ObjectDetectionActivity)
        }
        confirmPhotoButton = findViewById<AppCompatButton>(R.id.confirm_button).apply {
            setOnClickListener(this@ObjectDetectionActivity)
        }
        searchButtonAnimator =
            (AnimatorInflater.loadAnimator(
                this,
                R.animator.search_button_enter
            ) as AnimatorSet).apply {
                setTarget(takePhotoButton)
            }
        findViewById<View>(R.id.close_button).setOnClickListener(this)
        flashButton = findViewById<View>(R.id.flash_button).apply {
            setOnClickListener(this@ObjectDetectionActivity)
        }
        setUpWorkflowModel()
    }

    override fun onResume() {
        super.onResume()

        workflowModel?.markCameraFrozen()
        currentWorkflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(
            ProminentObjectProcessor(
                graphicOverlay!!, workflowModel!!,
                CUSTOM_MODEL_PATH
            )
        )
        workflowModel?.setWorkflowState(WorkflowState.DETECTING)
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.take_photo_button -> {
                stopCameraPreview()
                workflowModel?.setWorkflowState(WorkflowState.CONFIRMED)
            }
            R.id.close_button -> onBackPressed()
            R.id.flash_button -> {
                if (flashButton?.isSelected == true) {
                    flashButton?.isSelected = false
                    cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                } else {
                    flashButton?.isSelected = true
                    cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                }
            }
            R.id.take_again_button -> {
                workflowModel?.setWorkflowState(WorkflowState.DETECTING)
                PreferenceUtils.cleanPreviousSavedImagePaths(applicationContext)
            }
            R.id.confirm_button -> {
                workflowModel?.setObjectToSearch()
                if (currentFlowType == FlowType.DOUBLE_SIDE && (isFrontSideImageSaved() == false || isBackSideImageSaved() == false)) {
                    workflowModel?.setWorkflowState(WorkflowState.DETECTING)
                } else {
                    onImagesConfirmed()
                }
            }
        }
    }

    private fun startCameraPreview() {
        val cameraSource = this.cameraSource ?: return
        val workflowModel = this.workflowModel ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                preview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        if (workflowModel?.isCameraLive == true) {
            workflowModel!!.markCameraFrozen()
            flashButton?.isSelected = false
            preview?.stop()
        }
    }


    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java).apply {

            workflowState.observe(this@ObjectDetectionActivity, Observer { workflowState ->
                if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                    return@Observer
                }
                currentWorkflowState = workflowState
                Log.d(TAG, "Current workflow state: ${workflowState.name}")

                stateChangeInManualSearchMode(workflowState)
            })

            objectToSearch.observe(this@ObjectDetectionActivity) { detectObject ->
                workflowModel?.onSearchCompleted(detectObject)
            }

            searchedObject.observe(this@ObjectDetectionActivity, Observer { searchedObject ->
                objectThumbnailForBottomSheet = searchedObject.getObjectThumbnail()
                val stream = ByteArrayOutputStream()
                objectThumbnailForBottomSheet?.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val byteArray: ByteArray = stream.toByteArray()
                filePath = FileHelper.writeImageFileToDisk(applicationContext, byteArray)
                Log.e("searchedObject_observe", "hi")
                if (PreferenceUtils.getIdFrontSideImagePath(applicationContext)
                        ?.isEmpty() == true
                ) PreferenceUtils.saveStringPreference(
                    applicationContext,
                    R.string.pref_key_id_front_side_path,
                    filePath
                )
                else PreferenceUtils.saveStringPreference(
                    applicationContext,
                    R.string.pref_key_id_back_side_path,
                    filePath
                )
                takePhotoButton?.visibility = View.VISIBLE
            })
        }
    }

    private fun stateChangeInManualSearchMode(workflowState: WorkflowState) {
        val wasPromptChipGone = promptChip?.visibility == View.GONE

        when (workflowState) {
            WorkflowState.DETECTING -> {
                promptChip?.visibility = View.VISIBLE
                promptChip?.text =
                    if (PreferenceUtils.getIdFrontSideImagePath(applicationContext)
                            ?.isEmpty() == true
                    ) getString(R.string.prompt_point_to_front_side) else getString(R.string.prompt_point_to_back_side)
                takePhotoButton?.visibility = View.VISIBLE
                takePhotoButton?.enabled(false)
                startCameraPreview()
                buttonsLayout?.visibility = View.GONE
            }
            WorkflowState.CONFIRMED -> {
                promptChip?.visibility = View.GONE
                buttonsLayout?.visibility = View.VISIBLE
                takePhotoButton?.visibility = View.GONE
                takePhotoButton?.enabled(false)

                stopCameraPreview()
            }
            WorkflowState.FOUND -> {
                takePhotoButton?.enabled(true)
                vibratePhone()
            }
            else -> {
                promptChip?.visibility = View.GONE
                takePhotoButton?.visibility = View.VISIBLE
                takePhotoButton?.enabled(false)
            }
        }

        val shouldPlayPromptChipEnteringAnimation =
            wasPromptChipGone && promptChip?.visibility == View.VISIBLE
        promptChipAnimator?.let {
            if (shouldPlayPromptChipEnteringAnimation && !it.isRunning) it.start()
        }
    }

    private fun isFrontSideImageSaved() =
        PreferenceUtils.getIdFrontSideImagePath(applicationContext)?.isNotEmpty()

    private fun isBackSideImageSaved() =
        PreferenceUtils.getIdBackSideImagePath(applicationContext)?.isNotEmpty()

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }

    abstract fun onImagesConfirmed()

    fun setDetectionFlowType(flowType: FlowType) {
        currentFlowType = flowType
    }

    companion object {
        private const val TAG = "CustomModelODActivity"
        private const val CUSTOM_MODEL_PATH = "custom_models/object_labeler.tflite"
    }
}
