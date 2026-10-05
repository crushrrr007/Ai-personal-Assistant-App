package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.Task
import com.example.data.repo.TaskRepository
import com.example.ui.util.DateTimeUtils
import com.example.util.TimezoneManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class TaskFilterTab {
    TODAY,
    UPCOMING,
    DONE
}

data class TaskGroup(
    val header: String,
    val tasks: List<Task>
)

data class TasksUiState(
    val selectedFilter: TaskFilterTab = TaskFilterTab.TODAY,
    val groupedTasks: List<TaskGroup> = emptyList(),
    val totalPendingCount: Int = 0,
    val isAddEditSheetOpen: Boolean = false,
    val editingTask: Task? = null,
    val userZone: ZoneId = ZoneId.systemDefault()
)

sealed interface TaskUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : TaskUiEvent
}

class TasksViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TaskFilterTab.TODAY)
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _editingTask = MutableStateFlow<Task?>(null)
    private val _eventFlow = MutableSharedFlow<TaskUiEvent>()
    val eventFlow: SharedFlow<TaskUiEvent> = _eventFlow.asSharedFlow()

    private var recentlyDeletedTask: Task? = null
    private var recentlyCompletedTask: Task? = null

    val uiState: StateFlow<TasksUiState> = combine(
        repository.allTasks,
        _selectedFilter,
        _isAddEditSheetOpen,
        _editingTask,
        TimezoneManager.userZoneIdFlow
    ) { allTasks, filter, isSheetOpen, editingTask, userZone ->
        val today = LocalDate.now(userZone)

        val pendingTasks = allTasks.filter { it.status == Task.STATUS_PENDING }

        val filteredTasks = when (filter) {
            TaskFilterTab.TODAY -> {
                pendingTasks.filter { task ->
                    val dueAt = task.dueAt
                    if (dueAt != null) {
                        val taskDate = DateTimeUtils.toLocalDate(dueAt, userZone)
                        taskDate.isEqual(today) || taskDate.isBefore(today) // include today & overdue
                    } else {
                        false
                    }
                }
            }
            TaskFilterTab.UPCOMING -> {
                pendingTasks.filter { task ->
                    val dueAt = task.dueAt
                    if (dueAt != null) {
                        val taskDate = DateTimeUtils.toLocalDate(dueAt, userZone)
                        taskDate.isAfter(today)
                    } else {
                        true // unscheduled tasks belong in upcoming backlog
                    }
                }
            }
            TaskFilterTab.DONE -> {
                allTasks.filter { it.status == Task.STATUS_DONE }
            }
        }

        // Group tasks by day using user's active timezone
        val groupedList = if (filter == TaskFilterTab.DONE) {
            if (filteredTasks.isNotEmpty()) {
                listOf(TaskGroup(header = "Completed", tasks = filteredTasks))
            } else {
                emptyList()
            }
        } else {
            filteredTasks.groupBy { task ->
                DateTimeUtils.getGroupHeader(task.dueAt, userZone)
            }.map { (header, list) ->
                TaskGroup(header = header, tasks = list)
            }
        }

        TasksUiState(
            selectedFilter = filter,
            groupedTasks = groupedList,
            totalPendingCount = pendingTasks.size,
            isAddEditSheetOpen = isSheetOpen,
            editingTask = editingTask,
            userZone = userZone
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun setFilter(filter: TaskFilterTab) {
        _selectedFilter.value = filter
    }

    fun openAddTaskSheet() {
        _editingTask.value = null
        _isAddEditSheetOpen.value = true
    }

    fun openEditTaskSheet(task: Task) {
        _editingTask.value = task
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _editingTask.value = null
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            recentlyCompletedTask = task
            repository.toggleTaskCompletion(task)

            val actionMessage = if (task.status == Task.STATUS_DONE) "Marked as pending" else "Task completed"
            _eventFlow.emit(
                TaskUiEvent.ShowSnackbar(
                    message = actionMessage,
                    actionLabel = "Undo",
                    onAction = { undoLastAction() }
                )
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            recentlyDeletedTask = task
            repository.deleteTask(task)

            _eventFlow.emit(
                TaskUiEvent.ShowSnackbar(
                    message = "\"${task.title}\" deleted",
                    actionLabel = "Undo",
                    onAction = { undoLastAction() }
                )
            )
        }
    }

    private fun undoLastAction() {
        viewModelScope.launch {
            recentlyDeletedTask?.let {
                repository.insertTask(it)
                recentlyDeletedTask = null
                return@launch
            }
            recentlyCompletedTask?.let {
                repository.toggleTaskCompletion(it)
                recentlyCompletedTask = null
            }
        }
    }

    fun saveTask(
        title: String,
        dueAt: Long?,
        isDeadline: Boolean,
        remindBeforeMin: Int,
        estimatedMin: Int?
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val currentEditing = _editingTask.value
            if (currentEditing != null) {
                repository.updateTask(
                    currentEditing.copy(
                        title = title.trim(),
                        dueAt = dueAt,
                        isDeadline = isDeadline,
                        remindBeforeMin = remindBeforeMin,
                        estimatedMin = estimatedMin
                    )
                )
            } else {
                repository.insertTask(
                    Task(
                        title = title.trim(),
                        dueAt = dueAt,
                        isDeadline = isDeadline,
                        remindBeforeMin = remindBeforeMin,
                        estimatedMin = estimatedMin,
                        status = Task.STATUS_PENDING,
                        source = Task.SOURCE_USER
                    )
                )
            }
            closeAddEditSheet()
        }
    }

    companion object {
        fun provideFactory(repository: TaskRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TasksViewModel(repository) as T
                }
            }
    }
}
