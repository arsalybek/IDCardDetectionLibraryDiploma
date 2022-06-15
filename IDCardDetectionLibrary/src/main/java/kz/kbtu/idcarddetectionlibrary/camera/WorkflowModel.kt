package kz.kbtu.idcarddetectionlibrary.camera

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kz.kbtu.idcarddetectionlibrary.objectDetection.DetectedObjectInfo
import kz.kbtu.idcarddetectionlibrary.productsearch.SearchedObject

/** View model for handling application workflow based on camera preview.  */
class WorkflowModel(application: Application) : AndroidViewModel(application) {

    val workflowState = MutableLiveData<WorkflowState>()
    val objectToSearch = MutableLiveData<DetectedObjectInfo>()
    val searchedObject = MutableLiveData<SearchedObject>()

    private val objectIdsToSearch = HashSet<Int>()

    var isCameraLive = false
        private set

    private var confirmedObject: DetectedObjectInfo? = null

    private val context: Context
        get() = getApplication<Application>().applicationContext

    /**
     * State set of the application workflow.
     */
    enum class WorkflowState {
        NOT_STARTED,
        DETECTING,
        FOUND,
        CONFIRMED
    }

    @MainThread
    fun setWorkflowState(workflowState: WorkflowState) {
        if (workflowState != WorkflowState.CONFIRMED) {
            confirmedObject = null
        }
        this.workflowState.value = workflowState
    }

    fun markCameraLive() {
        isCameraLive = true
        objectIdsToSearch.clear()
    }

    fun markCameraFrozen() {
        isCameraLive = false
    }

    fun onSearchCompleted(detectedObject: DetectedObjectInfo) {
        val lConfirmedObject = confirmedObject
        if (detectedObject != lConfirmedObject) {
            return
        }

        objectIdsToSearch.remove(detectedObject.objectId)
        searchedObject.value = SearchedObject(context.resources, lConfirmedObject)
    }

    @MainThread
    fun confirmingObject(confirmingObject: DetectedObjectInfo, progress: Float) {
        val isConfirmed = progress.compareTo(1f) == 0
        if (isConfirmed) {
            confirmedObject = confirmingObject
        }
    }

    fun setObjectToSearch() {
        objectToSearch.value = confirmedObject!!
    }
}
