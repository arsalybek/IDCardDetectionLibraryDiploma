package kz.kbtu.idcarddetectionlibrary.utils

import android.content.Context
import android.graphics.RectF
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import com.google.android.gms.common.images.Size
import kz.kbtu.idcarddetectionlibrary.R
import kz.kbtu.idcarddetectionlibrary.camera.CameraSizePair
import kz.kbtu.idcarddetectionlibrary.camera.GraphicOverlay

/** Utility class to retrieve shared preferences. */
object PreferenceUtils {

  private fun isAutoSearchEnabled(context: Context): Boolean =
    getBooleanPref(context, R.string.pref_key_enable_auto_search, true)

  private fun isMultipleObjectsMode(context: Context): Boolean =
    getBooleanPref(context, R.string.pref_key_object_detector_enable_multiple_objects, false)

  fun isClassificationEnabled(context: Context): Boolean =
    getBooleanPref(context, R.string.pref_key_object_detector_enable_classification, false)

  fun saveStringPreference(context: Context, @StringRes prefKeyId: Int, value: String?) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putString(context.getString(prefKeyId), value)
      .apply()
  }

  fun getIdFrontSideImagePath(context: Context): String? = getStringPref(context, R.string.pref_key_id_front_side_path, "")

  fun getIdBackSideImagePath(context: Context): String? = getStringPref(context, R.string.pref_key_id_back_side_path, "")

  fun cleanPreviousSavedImagePaths(context: Context) {
    saveStringPreference(context, R.string.pref_key_id_front_side_path, "")
    saveStringPreference(context, R.string.pref_key_id_back_side_path, "")
  }

  fun getConfirmationTimeMs(context: Context): Int =
    when {
      isMultipleObjectsMode(context) -> 300
      isAutoSearchEnabled(context) ->
        getIntPref(context, R.string.pref_key_confirmation_time_in_auto_search, 1500)
      else -> getIntPref(context, R.string.pref_key_confirmation_time_in_manual_search, 500)
    }

  fun getBarcodeReticleBox(overlay: GraphicOverlay): RectF {
    val context = overlay.context
    val overlayWidth = overlay.width.toFloat()
    val overlayHeight = overlay.height.toFloat()
    val boxWidth =
      overlayWidth * getIntPref(context, R.string.pref_key_barcode_reticle_width, 80) / 100
    val boxHeight =
      overlayHeight * getIntPref(context, R.string.pref_key_barcode_reticle_height, 35) / 100
    val cx = overlayWidth / 2
    val cy = overlayHeight / 2
    return RectF(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2)
  }

  private fun getIntPref(context: Context, @StringRes prefKeyId: Int, defaultValue: Int): Int {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val prefKey = context.getString(prefKeyId)
    return sharedPreferences.getInt(prefKey, defaultValue)
  }

  fun getUserSpecifiedPreviewSize(context: Context): CameraSizePair? {
    return try {
      val previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size)
      val pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size)
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
      CameraSizePair(
        Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)!!),
        Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)!!)
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun getBooleanPref(
    context: Context,
    @StringRes prefKeyId: Int,
    defaultValue: Boolean
  ): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(context.getString(prefKeyId), defaultValue)


  private fun getStringPref(
    context: Context,
    @StringRes prefKeyId: Int,
    defaultValue: String
  ): String? =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getString(context.getString(prefKeyId), defaultValue)
}
