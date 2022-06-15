package kz.kbtu.objectdetectionlibrary.viewExtensions

import android.view.View

fun View.enabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
    this.alpha = if(isEnabled) 1F else 0.4F
}
