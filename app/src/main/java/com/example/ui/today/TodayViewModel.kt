package com.example.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DailyPlanResult
import com.example.data.local.Task
import com.example.data.repo.DailyPlanRepository
import com.example.data.repo.TaskRepository
import com.example.util.TimezoneManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class TodayUiStats(
    val doneCount: Int = 0,
    val leftCount: Int = 0,
    val deadlineCount: Int = 0
)

data class TodayUiState(
    val nextUpTask: Task? = null,
    val stats: TodayUiStats = TodayUiStats(),
    val dailyPlan: DailyPlanResult? = null,
    val isGeneratingPlan: Boolean = false,
    val planError: String? = null,
    val userZone: ZoneId = ZoneId.systemDefault()
)

class TodayViewModel(
    private val taskRepository: TaskRepository,
    private val dailyPlanRepository: DailyPlanRepository
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _planError = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = TimezoneManager.userZoneIdFlow.flatMapLatest { userZone ->
        val todayStr = LocalDate.now(userZone).toString()
        combine(
            taskRepository.allTasks,
            dailyPlanRepository.getPlanForDate(todayStr),
            _isGenerating,
            _planError
        ) { tasks, plan, isGenerating, planError ->
            val pendingTasks = tasks.filter { it.status == Task.STATUS_PENDING }

            // Find next-up task: earliest pending task due in future, or earliest overdue
            val nextUp = pendingTasks
                .filter { it.dueAt != null }
                .sortedBy { it.dueAt }
                .firstOrNull() ?: pendingTasks.firstOrNull()

            val doneCount = tasks.count { it.status == Task.STATUS_DONE }
            val leftCount = pendingTasks.size
            val deadlineCount = pendingTasks.count { it.isDeadline }

            TodayUiState(
                nextUpTask = nextUp,
                stats = TodayUiStats(doneCount, leftCount, deadlineCount),
                dailyPlan = plan,
                isGeneratingPlan = isGenerating,
                planError = planError,
                userZone = userZone
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    fun planMyDay() {
        viewModelScope.launch {
            _isGenerating.value = true
            _planError.value = null
            val now = System.currentTimeMillis()
            val futureLimit = now + (24L * 60L * 60L * 1000L) // Next 24 hours
            val tasks = taskRepository.getPendingTasksInRange(now - (3L * 3600L * 1000L), futureLimit)

            val result = dailyPlanRepository.generateDailyPlan(
                tasks = tasks,
                workingHours = "12:00 to 22:00"
            )

            _isGenerating.value = false
            if (result.isFailure) {
                _planError.value = result.exceptionOrNull()?.message ?: "Failed to generate plan. Please try again."
            }
        }
    }

    fun acceptPlan() {
        viewModelScope.launch {
            val userZone = TimezoneManager.getUserZoneId()
            val todayStr = LocalDate.now(userZone).toString()
            dailyPlanRepository.acceptPlan(todayStr)
        }
    }

    fun toggleBlock(blockIndex: Int) {
        viewModelScope.launch {
            val userZone = TimezoneManager.getUserZoneId()
            val todayStr = LocalDate.now(userZone).toString()
            dailyPlanRepository.toggleBlockCompletion(todayStr, blockIndex)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }

    fun quickAddTask(title: String, isDeadline: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.insertTask(
                Task(
                    title = title.trim(),
                    dueAt = System.currentTimeMillis() + (2 * 3600 * 1000L), // In 2 hours by default
                    isDeadline = isDeadline,
                    source = Task.SOURCE_USER
                )
            )
        }
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val dailyPlanRepository: DailyPlanRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayViewModel(taskRepository, dailyPlanRepository) as T
        }
    }
}
