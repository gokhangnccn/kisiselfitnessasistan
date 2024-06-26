package viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import model.Egzersiz

class RoutineViewModel : ViewModel() {

    private val _isRoutineActive = MutableLiveData(false)
    val isRoutineActive: LiveData<Boolean> get() = _isRoutineActive

    private val _elapsedTime = MutableLiveData<Long>()
    val elapsedTime: LiveData<Long> get() = _elapsedTime

    private val _finishRoutineEvent = MutableLiveData<Unit>()
    val finishRoutineEvent: LiveData<Unit> get() = _finishRoutineEvent

    private var startTime: Long = 0

    private val _selectedRoutineExercises = MutableLiveData<List<Egzersiz>>()
    val selectedRoutineExercises: LiveData<List<Egzersiz>> = _selectedRoutineExercises


    // Add a property to store the current routine ID
    var currentRoutineId: String? = null
        private set

    init {
        _isRoutineActive.value = false
        _elapsedTime.value = 0L
    }

    fun startRoutine(routineId: String) {
        _isRoutineActive.value = true
        startTime = System.currentTimeMillis()
        currentRoutineId = routineId
        viewModelScope.launch {
            while (_isRoutineActive.value == true) {
                _elapsedTime.postValue(System.currentTimeMillis() - startTime)
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun resumeRoutine() {
        _isRoutineActive.value = true
        startTime = System.currentTimeMillis() - (_elapsedTime.value ?: 0L)
        viewModelScope.launch {
            while (_isRoutineActive.value == true) {
                _elapsedTime.postValue(System.currentTimeMillis() - startTime)
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun updateSelectedRoutineExercises(exercises: List<Egzersiz>) {
        _selectedRoutineExercises.value = exercises
    }
    fun stopRoutine() {
        _isRoutineActive.value = false
    }

    fun triggerFinishRoutine() {
        _finishRoutineEvent.value = Unit
    }

    fun isRoutineActive(): Boolean {
        return _isRoutineActive.value == true
    }
}

