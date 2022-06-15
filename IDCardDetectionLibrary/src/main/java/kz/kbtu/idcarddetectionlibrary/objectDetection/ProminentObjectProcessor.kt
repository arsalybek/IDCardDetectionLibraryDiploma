package kz.kbtu.idcarddetectionlibrary.objectDetection

import android.graphics.RectF
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kz.kbtu.idcarddetectionlibrary.R
import kz.kbtu.idcarddetectionlibrary.camera.CameraReticleAnimator
import kz.kbtu.idcarddetectionlibrary.camera.FrameProcessorBase
import kz.kbtu.idcarddetectionlibrary.camera.GraphicOverlay
import kz.kbtu.idcarddetectionlibrary.camera.WorkflowModel
import kz.kbtu.idcarddetectionlibrary.utils.InputInfo
import kz.kbtu.idcarddetectionlibrary.utils.PreferenceUtils
import java.io.IOException

/** A processor to run object detector in prominent object only mode.  */
class ProminentObjectProcessor(
    graphicOverlay: GraphicOverlay,
    private val workflowModel: WorkflowModel,
    private val customModelPath: String? = null
) :
    FrameProcessorBase<List<DetectedObject>>() {

    private val detector: ObjectDetector
    private val confirmationController: ObjectConfirmationController =
        ObjectConfirmationController(graphicOverlay)
    private val cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)
    private val reticleOuterRingRadius: Int = graphicOverlay
        .resources
        .getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)

    init {
        val options: ObjectDetectorOptionsBase
        val isClassificationEnabled =
            PreferenceUtils.isClassificationEnabled(graphicOverlay.context)
        if (customModelPath != null) {
            val localModel = LocalModel.Builder()
                .setAssetFilePath(customModelPath)
                .build()
            options = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
        } else {
            val optionsBuilder = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            if (isClassificationEnabled) {
                optionsBuilder.enableClassification()
            }
            options = optionsBuilder.build()
        }

        this.detector = ObjectDetection.getClient(options)
    }

    override fun stop() {
        super.stop()
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close object detector!", e)
        }
    }

    override fun detectInImage(image: InputImage): Task<List<DetectedObject>> {
        return detector.process(image)
    }

    @MainThread
    override fun onSuccess(
        inputInfo: InputInfo,
        results: List<DetectedObject>,
        graphicOverlay: GraphicOverlay
    ) {
        var objects = results
        if (!workflowModel.isCameraLive) {
            return
        }

        if (PreferenceUtils.isClassificationEnabled(graphicOverlay.context)) {
            val qualifiedObjects = ArrayList<DetectedObject>()
            qualifiedObjects.addAll(objects)
            objects = qualifiedObjects
        }

        val objectIndex = 0
        val hasValidObjects = objects.isNotEmpty() &&
                (customModelPath == null || DetectedObjectInfo.hasValidLabels(objects[objectIndex]))

        graphicOverlay.clear()
        if (!hasValidObjects) {
            graphicOverlay.add(ObjectReticleGraphic(graphicOverlay, cameraReticleAnimator))
            cameraReticleAnimator.start()
            confirmationController.reset()
            workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTING)
        } else {
            if (objectBoxOverlapsConfirmationReticle(
                    graphicOverlay,
                    objects[0]
                ) && detectedValidObjects(objects[0].labels)
            ) {
                cameraReticleAnimator.cancel()
                graphicOverlay.add(
                    ObjectReticleGraphic(
                        graphicOverlay,
                        cameraReticleAnimator,
                        true
                    )
                )
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.FOUND)
                confirmationController.confirming(objects[0].trackingId)
                workflowModel.confirmingObject(
                    DetectedObjectInfo(objects[0], objectIndex, inputInfo),
                    confirmationController.progress
                )
            } else {
                graphicOverlay.add(ObjectReticleGraphic(graphicOverlay, cameraReticleAnimator))
                cameraReticleAnimator.start()
                confirmationController.reset()
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTING)
            }
        }
        graphicOverlay.invalidate()
    }

    private fun objectBoxOverlapsConfirmationReticle(
        graphicOverlay: GraphicOverlay,
        visionObject: DetectedObject
    ): Boolean {
        val boxRect = graphicOverlay.translateRect(visionObject.boundingBox)
        val reticleCenterX = graphicOverlay.width / 2f
        val reticleCenterY = graphicOverlay.height / 2f
        val reticleRect = RectF(
            reticleCenterX - reticleOuterRingRadius,
            reticleCenterY - reticleOuterRingRadius,
            reticleCenterX + reticleOuterRingRadius,
            reticleCenterY + reticleOuterRingRadius
        )
        return reticleRect.intersect(boxRect)
    }

    private fun detectedValidObjects(labels: List<DetectedObject.Label>) =
        labels.find { it.text == "Driver's license" } != null

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Object detection failed!", e)
    }

    companion object {
        private const val TAG = "ProminentObjProcessor"
    }
}
