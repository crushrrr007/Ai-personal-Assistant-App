package com.example.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.Task
import com.example.ui.theme.extendedColors
import com.example.ui.util.DateTimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DayOption {
    TODAY,
    TOMORROW,
    CUSTOM,
    NONE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskSheet(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, dueAt: Long?, isDeadline: Boolean, remindBeforeMin: Int, estimatedMin: Int?) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var isDeadline by remember(task) { mutableStateOf(task?.isDeadline ?: false) }
    var remindBeforeMin by remember(task) { mutableIntStateOf(task?.remindBeforeMin ?: 30) }
    var estimatedMin by remember(task) { mutableStateOf(task?.estimatedMin) }

    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val tomorrow = remember { today.plusDays(1) }

    var selectedDayOption by remember(task) {
        val initialOption = if (task?.dueAt == null) {
            DayOption.TOMORROW
        } else {
            val taskDate = DateTimeUtils.toLocalDate(task.dueAt)
            when (taskDate) {
                today -> DayOption.TODAY
                tomorrow -> DayOption.TOMORROW
                else -> DayOption.CUSTOM
            }
        }
        mutableStateOf(initialOption)
    }

    var customDate by remember(task) {
        mutableStateOf(
            task?.dueAt?.let { DateTimeUtils.toLocalDate(it) } ?: tomorrow
        )
    }

    var selectedTime by remember(task) {
        mutableStateOf(
            task?.dueAt?.let { DateTimeUtils.toLocalTime(it) } ?: LocalTime.of(18, 0)
        )
    }

    var hasDueTime by remember(task) {
        mutableStateOf(task?.dueAt != null || task == null)
    }

    // Lead time dropdown options
    val leadTimeOptions = remember {
        listOf(
            0 to "At time of event",
            15 to "15 minutes before",
            30 to "30 minutes before",
            60 to "1 hour before",
            120 to "2 hours before",
            240 to "4 hours before",
            1440 to "1 day before"
        )
    }
    var leadTimeExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_edit_task_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (task == null) "New task" else "Edit task",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_task_button")
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What needs to be done?") },
                placeholder = { Text("e.g. Submit lab assignment") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Day section
            Text(
                text = "When",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDayOption == DayOption.TODAY && hasDueTime,
                    onClick = {
                        hasDueTime = true
                        selectedDayOption = DayOption.TODAY
                    },
                    label = { Text("Today") },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = selectedDayOption == DayOption.TOMORROW && hasDueTime,
                    onClick = {
                        hasDueTime = true
                        selectedDayOption = DayOption.TOMORROW
                    },
                    label = { Text("Tomorrow") },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = selectedDayOption == DayOption.CUSTOM && hasDueTime,
                    onClick = {
                        hasDueTime = true
                        selectedDayOption = DayOption.CUSTOM
                        val dpd = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                customDate = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            customDate.year,
                            customDate.monthValue - 1,
                            customDate.dayOfMonth
                        )
                        dpd.show()
                    },
                    label = {
                        val label = if (selectedDayOption == DayOption.CUSTOM) {
                            customDate.format(DateTimeFormatter.ofPattern("MMM d"))
                        } else {
                            "Pick date"
                        }
                        Text(label)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = !hasDueTime,
                    onClick = {
                        hasDueTime = false
                        selectedDayOption = DayOption.NONE
                    },
                    label = { Text("No date") },
                    shape = CircleShape
                )
            }

            AnimatedVisibility(visible = hasDueTime) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Time selection
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickTimes = listOf(
                            LocalTime.of(9, 0) to "9:00 AM",
                            LocalTime.of(14, 0) to "2:00 PM",
                            LocalTime.of(18, 0) to "6:00 PM"
                        )

                        quickTimes.forEach { (time, label) ->
                            FilterChip(
                                selected = selectedTime.hour == time.hour && selectedTime.minute == time.minute,
                                onClick = { selectedTime = time },
                                label = { Text(label) },
                                shape = CircleShape
                            )
                        }

                        // Custom time picker
                        val isCustomTime = quickTimes.none { it.first.hour == selectedTime.hour && it.first.minute == selectedTime.minute }
                        FilterChip(
                            selected = isCustomTime,
                            onClick = {
                                val tpd = TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        selectedTime = LocalTime.of(hourOfDay, minute)
                                    },
                                    selectedTime.hour,
                                    selectedTime.minute,
                                    false
                                )
                                tpd.show()
                            },
                            label = { Text(selectedTime.format(timeFormatter)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = CircleShape
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reminder lead time dropdown
                    Text(
                        text = "Reminder nudge",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = leadTimeExpanded,
                        onExpandedChange = { leadTimeExpanded = it }
                    ) {
                        val currentLeadTimeLabel = leadTimeOptions.firstOrNull { it.first == remindBeforeMin }?.second ?: "$remindBeforeMin min before"
                        OutlinedTextField(
                            value = currentLeadTimeLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = leadTimeExpanded) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = leadTimeExpanded,
                            onDismissRequest = { leadTimeExpanded = false }
                        ) {
                            leadTimeOptions.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        remindBeforeMin = minutes
                                        leadTimeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Deadline tag toggle & Estimated time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isDeadline,
                    onClick = { isDeadline = !isDeadline },
                    label = { Text("Deadline") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.extendedColors.deadlineBadgeContainer,
                        selectedLabelColor = MaterialTheme.extendedColors.deadlineBadge
                    )
                )

                // Estimated minutes chips
                val estOptions = listOf(30 to "30m", 60 to "1h", 120 to "2h")
                estOptions.forEach { (mins, label) ->
                    FilterChip(
                        selected = estimatedMin == mins,
                        onClick = { estimatedMin = if (estimatedMin == mins) null else mins },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = CircleShape
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    val finalDueAt = if (hasDueTime) {
                        val targetDate = when (selectedDayOption) {
                            DayOption.TODAY -> today
                            DayOption.TOMORROW -> tomorrow
                            DayOption.CUSTOM -> customDate
                            DayOption.NONE -> null
                        }
                        targetDate?.let { DateTimeUtils.toEpochMillis(it, selectedTime) }
                    } else {
                        null
                    }

                    onSave(title, finalDueAt, isDeadline, remindBeforeMin, estimatedMin)
                },
                enabled = title.isNotBlank(),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_task_button")
            ) {
                Text("Save task", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
