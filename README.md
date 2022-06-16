# IDCardDetectionLibraryDiploma

<img src="https://github.com/arsalybek/IDCardDetectionLibraryDiploma/blob/master/screenshot_1.jpeg" width="311" height="640">
<img src="https://github.com/arsalybek/IDCardDetectionLibraryDiploma/blob/master/screenshot_2.jpeg" width="311" height="640">
<img src="https://github.com/arsalybek/IDCardDetectionLibraryDiploma/blob/master/screenshot_3.jpeg" width="311" height="640">


Gradle
------
```
dependencies {
    ...
    implementation 'com.github.arsalybek:IDCardDetectionLibraryDiploma:1.0.5'
}
```

Usage
-----
```
extend kz.kbtu.idcarddetectionlibrary.HintActivity:

- fun onPermissionGranted() {
  // override this method to define action, when camera permission for app was granted
}

extend kz.kbtu.idcarddetectionlibrary.ObjectDetectionActivity:

- fun onImagesConfirmed(pathList: List<String>) {
  // override this method to retrieve confirmed image paths
}
- fun setDetectionFlowType(flowType: FlowType) {
  // public enum FlowType { SINGLE_SIDE, DOUBLE_SIDE }
  
  // override this method to set detection flow as single or double image required
}
-fun setCustomModelPath(path: String) {
  // override this method to set path for custom model to classify detected objects
}
```
