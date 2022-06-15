package kz.kbtu.idcarddetectionlibrary.productsearch

import android.content.res.Resources
import android.graphics.Bitmap
import kz.kbtu.idcarddetectionlibrary.R
import kz.kbtu.idcarddetectionlibrary.objectDetection.DetectedObjectInfo
import kz.kbtu.idcarddetectionlibrary.utils.Utils

/** Hosts the detected object info and its search result.  */
class SearchedObject(
    resources: Resources,
    private val detectedObject: DetectedObjectInfo
) {

    private val objectThumbnailCornerRadius: Int =
        resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius)
    private var objectThumbnail: Bitmap? = null

    @Synchronized
    fun getObjectThumbnail(): Bitmap = objectThumbnail ?: let {
        Utils.getCornerRoundedBitmap(detectedObject.getBitmap(), objectThumbnailCornerRadius)
            .also { objectThumbnail = it }
    }
}
