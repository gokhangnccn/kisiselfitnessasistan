import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AntrenmanViewModel : ViewModel() {
    private val _isRoutineActive = MutableLiveData<Boolean>()
    val isRoutineActive: LiveData<Boolean> get() = _isRoutineActive

    fun startRoutine() {
        _isRoutineActive.value = true
    }

    fun finishRoutine() {
        _isRoutineActive.value = false
    }
}

