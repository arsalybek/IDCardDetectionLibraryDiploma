package kz.kbtu.idcarddetectionlibrary

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kz.kbtu.idcarddetectionlibrary.utils.PreferenceUtils
import kz.kbtu.idcarddetectionlibrary.utils.Utils

abstract class HintActivity : AppCompatActivity() {

    private var gotoButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hint)

        PreferenceUtils.cleanPreviousSavedImagePaths(applicationContext)
        gotoButton = findViewById(R.id.buttonGoto)
    }

    override fun onResume() {
        super.onResume()

        if (!Utils.allPermissionsGranted(this))
            gotoButton?.text = getString(R.string.give_camera_permission)
        else gotoButton?.text = getString(R.string.go_to)
        gotoButton?.setOnClickListener {
            if (!Utils.allPermissionsGranted(this)) {
                Utils.requestRuntimePermissions(this)
            } else {
                //startActivity(Intent(this, ObjectDetectionActivity::class.java))
                onPermissionGranted()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Utils.REQUEST_CODE_PHOTO_LIBRARY &&
            resultCode == Activity.RESULT_OK
        ) {
            startActivity(Intent(this, ObjectDetectionActivity::class.java))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    abstract fun onPermissionGranted()
}