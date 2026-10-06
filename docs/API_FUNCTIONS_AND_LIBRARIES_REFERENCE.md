# 📚 Functions & Libraries Technical Reference Manual
**Project Title**: AI Personal Coordinator & Smart Task Manager  
**Purpose**: Complete, detailed reference of all external libraries and core functions organized by architectural layer with signatures, parameters, return types, and code call examples for your presentation and viva.

---

## 📌 Table of Contents
1. [Master Libraries & Dependencies Directory](#1-master-libraries--dependencies-directory)
2. [Layer 1: Persistence & Local Database (`com.example.data.local`)](#2-layer-1-persistence--local-database)
3. [Layer 2: Domain & Repository Layer (`com.example.data.repo`)](#3-layer-2-domain--repository-layer)
4. [Layer 3: AI & Natural Language Processing (`com.example.ai` & `com.example.data.remote`)](#4-layer-3-ai--natural-language-processing)
5. [Layer 4: Android System Services & Reminders (`com.example.reminder` & `com.example.util`)](#5-layer-4-android-system-services--reminders)
6. [Layer 5: Presentation & UI Layer (`com.example.ui`)](#6-layer-5-presentation--ui-layer)

---

## 1. Master Libraries & Dependencies Directory

| Library / Coordinate | Category | Why It Was Used in This Project |
| :--- | :--- | :--- |
| **`androidx.room:room-runtime`**<br>`androidx.room:room-ktx`<br>`androidx.room:room-compiler (KSP)` | **Local Database (SQLite)** | Provides an abstraction over SQLite with compile-time SQL verification, preventing runtime database crashes. It returns reactive Kotlin `Flow<List<T>>` streams for automatic UI updates and runs asynchronously via Kotlin Coroutines. |
| **`androidx.compose.ui`**<br>`androidx.compose.material3`<br>`androidx.compose.foundation` | **UI Toolkit** | Modern declarative UI framework. Replaces 1,000+ lines of XML layouts and `RecyclerView.Adapter` classes. Automatically recomposes UI when state changes (`UI = f(State)`). |
| **`androidx.lifecycle:lifecycle-viewmodel-compose`**<br>`androidx.lifecycle:lifecycle-runtime-compose` | **Architecture Components** | Manages ViewModel lifecycles across screen rotations and configuration changes. Provides `collectAsStateWithLifecycle()` to automatically pause database flow observation when the app enters the background, conserving battery. |
| **`androidx.navigation:navigation-compose`** | **In-App Navigation** | Type-safe single-activity navigation managing the bottom navigation bar and backstack transitions between `Today`, `Tasks`, `Notes`, and `Chat`. |
| **`com.squareup.okhttp3:okhttp`** | **Networking** | High-performance, production-grade HTTP client with connection pooling, custom timeouts (30s connect, 60s read), and response interception for communicating with the Google Gemini 2.5 Flash REST API. |
| **`org.jetbrains.kotlinx:kotlinx-serialization-json`** | **Serialization** | Official Kotlin-first JSON parser. Used to parse Gemini's structured JSON output into strongly typed data models (`create_task`, `modify_task`, `daily_plan`) without the reflection overhead of Gson. |
| **`org.jetbrains.kotlinx:kotlinx-coroutines-android`** | **Concurrency** | Enables lightweight asynchronous background threads (`Dispatchers.IO`) for database queries and network calls, preventing the Android Main/UI thread from freezing (avoiding ANR - Application Not Responding dialogs). |
| **`androidx.core:core-ktx`** | **Android Extensions** | Provides idiomatic Kotlin extensions for standard Android SDK APIs (e.g., NotificationManager, AlarmManager, Context). |

---

## 2. Layer 1: Persistence & Local Database
*Package: `com.example.data.local`*

### 1. `AppDatabase.getInstance(context: Context): AppDatabase`
- **Class**: `AppDatabase.kt`
- **Signature**: `fun getInstance(context: Context): AppDatabase`
- **Parameters**: `context: Context` (Application context)
- **Returns**: `AppDatabase` (Thread-safe singleton database instance)
- **What It Does**: Implements the classic Double-Checked Locking Singleton pattern with `@Volatile`. Ensures only one SQLite open connection exists across the entire app lifecycle, eliminating database lock contention.
- **Example Call**:
  ```kotlin
  val database = AppDatabase.getInstance(context.applicationContext)
  val taskDao = database.taskDao()
  val userDao = database.userDao()
  ```

---

### 2. `UserDao.getUserByEmail(email: String): User?`
- **Class**: `UserDao.kt`
- **Signature**: `@Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1") suspend fun getUserByEmail(email: String): User?`
- **Parameters**: `email: String`
- **Returns**: `User?` (Matched User entity or null if not registered)
- **What It Does**: Looks up a registered user by email case-insensitively during login.
- **Example Call**:
  ```kotlin
  val user = userDao.getUserByEmail("himanshu@example.com")
  ```

---

### 3. `UserDao.insertUser(user: User): Long`
- **Class**: `UserDao.kt`
- **Signature**: `@Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertUser(user: User): Long`
- **Parameters**: `user: User` (Entity containing `fullName`, `email`, `passwordHash`, `salt`)
- **Returns**: `Long` (Generated user ID)
- **What It Does**: Stores a newly registered user account into SQLite. Aborts if email already exists due to unique constraint.
- **Example Call**:
  ```kotlin
  val newUserId = userDao.insertUser(user)
  ```

---

### 4. `TaskDao.getAllTasks(): Flow<List<Task>>`
- **Class**: `TaskDao.kt`
- **Signature**: `@Query("SELECT * FROM tasks ORDER BY dueAt ASC") fun getAllTasks(): Flow<List<Task>>`
- **Parameters**: None
- **Returns**: `Flow<List<Task>>` (Cold reactive stream of all saved tasks ordered chronologically)
- **What It Does**: Queries the SQLite `tasks` table. Because it returns a Kotlin `Flow`, Room automatically re-executes the query and emits a new list whenever any task is inserted, updated, or deleted.
- **Example Call**:
  ```kotlin
  taskDao.getAllTasks().collect { tasks ->
      println("Current task count: ${tasks.size}")
  }
  ```

---

### 3. `TaskDao.insertTask(task: Task): Long`
- **Class**: `TaskDao.kt`
- **Signature**: `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTask(task: Task): Long`
- **Parameters**: `task: Task` (The entity to persist)
- **Returns**: `Long` (The autoincrement primary key ID generated by SQLite)
- **What It Does**: Suspends execution on a background thread, executes an SQL `INSERT` statement, and returns the newly generated row ID.
- **Example Call**:
  ```kotlin
  val newTaskId = taskDao.insertTask(Task(title = "Submit Report", dueAt = 1759750000000L))
  ```

---

### 4. `TaskDao.updateTask(task: Task)`
- **Class**: `TaskDao.kt`
- **Signature**: `@Update suspend fun updateTask(task: Task)`
- **Parameters**: `task: Task` (Task entity with updated properties matching an existing ID)
- **Returns**: `Unit`
- **What It Does**: Updates an existing row in SQLite based on matching `id`. Used when checking off a task or changing its due date.
- **Example Call**:
  ```kotlin
  taskDao.updateTask(task.copy(status = "done"))
  ```

---

### 5. `TaskDao.deleteTask(task: Task)`
- **Class**: `TaskDao.kt`
- **Signature**: `@Delete suspend fun deleteTask(task: Task)`
- **Parameters**: `task: Task` (Task entity to be permanently deleted)
- **Returns**: `Unit`
- **What It Does**: Removes the task row from SQLite by its primary key `id`.
- **Example Call**:
  ```kotlin
  taskDao.deleteTask(taskToDelete)
  ```

---

### 6. `NoteDao.searchNotes(query: String): Flow<List<Note>>`
- **Class**: `NoteDao.kt`
- **Signature**: `@Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY updatedAt DESC") fun searchNotes(query: String): Flow<List<Note>>`
- **Parameters**: `query: String` (Text string entered in the search bar)
- **Returns**: `Flow<List<Note>>` (Filtered list of notes)
- **What It Does**: Performs full-text matching against note titles and bodies.
- **Example Call**:
  ```kotlin
  noteDao.searchNotes("meeting").collect { matchingNotes -> ... }
  ```

---

### 7. `DailyPlanDao.getPlanByDate(date: String): Flow<DailyPlanEntity?>`
- **Class**: `DailyPlanDao.kt`
- **Signature**: `@Query("SELECT * FROM daily_plans WHERE date = :date") fun getPlanByDate(date: String): Flow<DailyPlanEntity?>`
- **Parameters**: `date: String` (ISO date string, e.g. `"2026-10-06"`)
- **Returns**: `Flow<DailyPlanEntity?>` (Stored daily time-block schedule or null if not yet planned)
- **What It Does**: Retrieves the cached AI day schedule for the specified calendar day.
- **Example Call**:
  ```kotlin
  dailyPlanDao.getPlanByDate("2026-10-06").collect { planEntity -> ... }
  ```

---

## 3. Layer 2: Domain & Repository Layer
*Package: `com.example.data.repo`*

### 1. `AuthRepository.register(fullName: String, email: String, password: String): Result<UserSession>`
- **Class**: `AuthRepository.kt`
- **Signature**: `suspend fun register(fullName: String, email: String, password: String): Result<UserSession>`
- **Parameters**: `fullName: String`, `email: String`, `password: String`
- **Returns**: `Result<UserSession>` (Successful session or error with reason)
- **What It Does**: Validates email format and password length (>= 6 chars), generates a 16-byte cryptographic salt via `PasswordHasher.generateSalt()`, computes SHA-256 hash, inserts into SQLite `users` table, and persists the active session in `SharedPreferences`.
- **Example Call**:
  ```kotlin
  val result = authRepository.register("Himanshu", "himanshu@example.com", "mypass123")
  ```

---

### 2. `AuthRepository.login(email: String, password: String): Result<UserSession>`
- **Class**: `AuthRepository.kt`
- **Signature**: `suspend fun login(email: String, password: String): Result<UserSession>`
- **Parameters**: `email: String`, `password: String`
- **Returns**: `Result<UserSession>`
- **What It Does**: Retrieves the user by email, computes candidate hash using stored salt, and verifies match via `PasswordHasher.verifyPassword` (constant-time `MessageDigest.isEqual`). If valid, saves session to `SharedPreferences` and emits to `sessionState`.
- **Example Call**:
  ```kotlin
  val result = authRepository.login("himanshu@example.com", "mypass123")
  ```

---

### 3. `AuthRepository.continueAsGuest(): UserSession`
- **Class**: `AuthRepository.kt`
- **Signature**: `fun continueAsGuest(): UserSession`
- **Parameters**: None
- **Returns**: `UserSession` (Guest session with `isGuest = true`)
- **What It Does**: Creates and saves a guest session flag, allowing immediate app evaluation without account registration.
- **Example Call**:
  ```kotlin
  val guestSession = authRepository.continueAsGuest()
  ```

---

### 4. `AuthRepository.logout()`
- **Class**: `AuthRepository.kt`
- **Signature**: `fun logout()`
- **Parameters**: None
- **Returns**: `Unit`
- **What It Does**: Clears session flags from `SharedPreferences` and resets `sessionState.value = null`, returning the user to the `AuthScreen`.
- **Example Call**:
  ```kotlin
  authRepository.logout()
  ```

---

### 5. `TaskRepository.insertTask(task: Task): Long`
- **Class**: `TaskRepository.kt`
- **Signature**: `suspend fun insertTask(task: Task): Long`
- **Parameters**: `task: Task`
- **Returns**: `Long` (Newly inserted task ID)
- **What It Does**:
  1. Writes the task to SQLite via `taskDao.insertTask(task)`.
  2. If `dueAt != null`, immediately invokes `reminderScheduler.scheduleReminder(savedTask)`.
- **Example Call**:
  ```kotlin
  val id = taskRepository.insertTask(task)
  ```

---

### 2. `TaskRepository.toggleTask(task: Task)`
- **Class**: `TaskRepository.kt`
- **Signature**: `suspend fun toggleTask(task: Task)`
- **Parameters**: `task: Task`
- **Returns**: `Unit`
- **What It Does**: Toggles task status between `"pending"` and `"done"`. If marked `"done"`, it cancels the scheduled hardware alarm (`reminderScheduler.cancelReminder(task.id)`). If unchecked back to `"pending"` with a future due date, it reschedules the alarm.
- **Example Call**:
  ```kotlin
  taskRepository.toggleTask(currentTask)
  ```

---

### 3. `TaskRepository.deleteTask(task: Task)`
- **Class**: `TaskRepository.kt`
- **Signature**: `suspend fun deleteTask(task: Task)`
- **Parameters**: `task: Task`
- **Returns**: `Unit`
- **What It Does**: Cancels any scheduled alarm in `AlarmManager` and removes the row from SQLite. Also preserves the task in an in-memory undo buffer for Snackbar undo actions.
- **Example Call**:
  ```kotlin
  taskRepository.deleteTask(task)
  ```

---

### 4. `ChatRepository.sendMessage(userMessage: String): ChatResponseResult`
- **Class**: `ChatRepository.kt`
- **Signature**: `suspend fun sendMessage(userMessage: String): ChatResponseResult`
- **Parameters**: `userMessage: String` (The raw text spoken or typed by the user)
- **Returns**: `ChatResponseResult` (Result object containing AI text response, created task, or proposed modification)
- **What It Does**:
  1. Fetches current context: active user timezone, localized timestamp, existing tasks, and notes.
  2. Formulates prompt using `PromptBuilder`.
  3. Dispatches HTTP POST to Gemini 2.5 Flash via `GeminiLlmService`.
  4. If network succeeds: parses structured JSON (`create_task`, `modify_task`, etc.) and performs database write.
  5. If network fails (`IOException`): routes to `LocalIntentParser` as a zero-network fallback!
- **Example Call**:
  ```kotlin
  val result = chatRepository.sendMessage("Remind me to call John tomorrow at 4 PM")
  ```

---

### 5. `DailyPlanRepository.generatePlanForToday(tasks: List<Task>, zoneId: ZoneId): DailyPlan`
- **Class**: `DailyPlanRepository.kt`
- **Signature**: `suspend fun generatePlanForToday(tasks: List<Task>, zoneId: ZoneId): DailyPlan`
- **Parameters**: `tasks: List<Task>`, `zoneId: ZoneId`
- **Returns**: `DailyPlan` (Ordered list of time blocks with 10-minute rest buffers)
- **What It Does**: Sends pending tasks for the next 24 hours to Gemini, receives optimized schedule blocks within working hours (12:00 to 22:00), and saves to `daily_plans` table.
- **Example Call**:
  ```kotlin
  val plan = dailyPlanRepository.generatePlanForToday(pendingTasks, userZoneId)
  ```

---

## 4. Layer 3: AI & Natural Language Processing
*Packages: `com.example.ai` & `com.example.data.remote`*

### 1. `GeminiLlmService.generateContent(prompt: String): String`
- **Class**: `GeminiLlmService.kt`
- **Signature**: `suspend fun generateContent(prompt: String): String`
- **Parameters**: `prompt: String` (Complete prompt string including system instructions)
- **Returns**: `String` (Raw JSON response payload from Gemini 2.5 Flash)
- **What It Does**: Uses OkHttp to make a POST request to `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`. Configures `"responseMimeType": "application/json"` to enforce structured JSON output.
- **Example Call**:
  ```kotlin
  val jsonResponse = geminiLlmService.generateContent(engineeredPrompt)
  ```

---

### 2. `PromptBuilder.buildSystemInstruction(zoneId: ZoneId): String`
- **Class**: `PromptBuilder.kt`
- **Signature**: `fun buildSystemInstruction(zoneId: ZoneId): String`
- **Parameters**: `zoneId: ZoneId` (User's active timezone, e.g. `Asia/Kolkata`)
- **Returns**: `String` (Formatted system instruction string)
- **What It Does**: Injects the active timezone, localized ISO-8601 timestamp, and explicit date definitions (`"Today is Tuesday, Oct 6. Tomorrow is Wednesday, Oct 7."`), anchoring all relative time interpretations to the user's local clock.
- **Example Call**:
  ```kotlin
  val systemPrompt = PromptBuilder.buildSystemInstruction(ZoneId.of("Asia/Kolkata"))
  ```

---

### 3. `LocalIntentParser.parse(text: String, userZoneId: ZoneId): ParsedTaskIntent?`
- **Class**: `LocalIntentParser.kt`
- **Signature**: `fun parse(text: String, userZoneId: ZoneId): ParsedTaskIntent?`
- **Parameters**: `text: String`, `userZoneId: ZoneId`
- **Returns**: `ParsedTaskIntent?` (Parsed title, due epoch ms, and remind offset, or null if unmatchable)
- **What It Does**: Runs on-device regular expression patterns to match phrases like *"at 4 PM"*, *"tomorrow at 10 AM"*, *"in 30 mins"*. Operates with **zero network connectivity**.
- **Example Call**:
  ```kotlin
  val intent = LocalIntentParser.parse("Remind me to buy groceries tomorrow at 5 PM", userZoneId)
  ```

---

## 5. Layer 4: Android System Services & Reminders
*Packages: `com.example.reminder` & `com.example.util`*

### 1. `ReminderScheduler.scheduleReminder(task: Task)`
- **Class**: `ReminderScheduler.kt`
- **Signature**: `fun scheduleReminder(task: Task)`
- **Parameters**: `task: Task` (Task containing valid `dueAt` and `remindBeforeMin`)
- **Returns**: `Unit`
- **What It Does**:
  1. Computes trigger epoch timestamp: `task.dueAt - (task.remindBeforeMin * 60 * 1000L)`.
  2. Creates explicit `PendingIntent` targeting `ReminderReceiver`.
  3. Registers alarm with `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)`.
  4. Bypasses Android Doze mode to ensure on-time delivery.
- **Example Call**:
  ```kotlin
  reminderScheduler.scheduleReminder(task)
  ```

---

### 2. `ReminderScheduler.cancelReminder(taskId: Long)`
- **Class**: `ReminderScheduler.kt`
- **Signature**: `fun cancelReminder(taskId: Long)`
- **Parameters**: `taskId: Long`
- **Returns**: `Unit`
- **What It Does**: Cancels the registered `PendingIntent` from `AlarmManager`, stopping any future notification from firing.
- **Example Call**:
  ```kotlin
  reminderScheduler.cancelReminder(task.id)
  ```

---

### 3. `ReminderReceiver.onReceive(context: Context, intent: Intent)`
- **Class**: `ReminderReceiver.kt`
- **Signature**: `override fun onReceive(context: Context, intent: Intent)`
- **Parameters**: `context: Context`, `intent: Intent`
- **Returns**: `Unit`
- **What It Does**: BroadcastReceiver called by Android OS when an alarm triggers. Extracts task ID, title, and deadline flags from intent extras, then calls `NotificationHelper.showTaskReminder(...)`.
- **Example Call**:
  *Triggered automatically by Android OS AlarmManager.*

---

### 4. `NotificationHelper.showTaskReminder(context: Context, taskId: Long, title: String, isDeadline: Boolean)`
- **Class**: `NotificationHelper.kt`
- **Signature**: `fun showTaskReminder(context: Context, taskId: Long, title: String, isDeadline: Boolean)`
- **Parameters**: `context: Context`, `taskId: Long`, `title: String`, `isDeadline: Boolean`
- **Returns**: `Unit`
- **What It Does**: Creates a high-importance `NotificationCompat.Builder` with vibration pattern `[0, 250, 250, 250]`, default alarm audio, content intent, and a direct "Mark Done" quick action button.
- **Example Call**:
  ```kotlin
  NotificationHelper.showTaskReminder(context, 42L, "Submit Assignment", isDeadline = true)
  ```

---

### 5. `TimezoneManager.setTimezone(zoneId: ZoneId)`
- **Class**: `TimezoneManager.kt`
- **Signature**: `fun setTimezone(zoneId: ZoneId)`
- **Parameters**: `zoneId: ZoneId` (Target zone, e.g. `ZoneId.of("Asia/Kolkata")`)
- **Returns**: `Unit`
- **What It Does**: Saves the selected timezone string into Android `SharedPreferences` and emits the updated `ZoneId` through `_userZoneIdFlow`. This immediately triggers reactive date recomputation in all ViewModels without restarting the app.
- **Example Call**:
  ```kotlin
  TimezoneManager.setTimezone(ZoneId.of("Asia/Kolkata"))
  ```

---

## 6. Layer 5: Presentation & UI Layer
*Package: `com.example.ui`*

### 1. `TasksViewModel.setTab(tab: TaskTab)`
- **Class**: `TasksViewModel.kt`
- **Signature**: `fun setTab(tab: TaskTab)`
- **Parameters**: `tab: TaskTab` (`TODAY`, `UPCOMING`, `DONE`)
- **Returns**: `Unit`
- **What It Does**: Updates internal `_selectedTab` StateFlow, filtering tasks reactively in the UI.
- **Example Call**:
  ```kotlin
  viewModel.setTab(TaskTab.UPCOMING)
  ```

---

### 2. `ChatViewModel.sendMessage(text: String)`
- **Class**: `ChatViewModel.kt`
- **Signature**: `fun sendMessage(text: String)`
- **Parameters**: `text: String` (User input message)
- **Returns**: `Unit`
- **What It Does**: Appends user message to UI state, sets `isThinking = true`, and launches a coroutine calling `chatRepository.sendMessage(text)`.
- **Example Call**:
  ```kotlin
  chatViewModel.sendMessage("Move my assignment to 5 PM")
  ```

---

### 3. `ChatViewModel.confirmPendingModification(mod: TaskModification)`
- **Class**: `ChatViewModel.kt`
- **Signature**: `fun confirmPendingModification(mod: TaskModification)`
- **Parameters**: `mod: TaskModification` (Proposed modification object)
- **Returns**: `Unit`
- **What It Does**: Executes the user's confirmation by updating the task row in Room and rescheduling the hardware alarm in `AlarmManager`.
- **Example Call**:
  ```kotlin
  viewModel.confirmPendingModification(modification)
  ```

---

### 4. `VoiceInputHandler.startListening()` & `stopListening()`
- **Class**: `VoiceInputHandler.kt`
- **Signature**: `fun startListening()`, `fun stopListening()`
- **Parameters**: None
- **Returns**: `Unit`
- **What It Does**: Initializes Android `SpeechRecognizer`, attaches `RecognitionListener`, requests microphone audio stream, and passes recognized speech text directly to the text field.
- **Example Call**:
  ```kotlin
  voiceInputState.startListening()
  ```

---

### 5. `DateTimeUtils.getGroupHeader(epochMs: Long, zoneId: ZoneId): String`
- **Class**: `DateTimeUtils.kt`
- **Signature**: `fun getGroupHeader(epochMs: Long, zoneId: ZoneId): String`
- **Parameters**: `epochMs: Long` (UTC epoch timestamp), `zoneId: ZoneId` (User timezone)
- **Returns**: `String` (Unambiguous formatted header, e.g. `"Today (Mon, Oct 5)"`, `"Tomorrow (Tue, Oct 6)"`)
- **What It Does**: Compares the target date with `LocalDate.now(zoneId)` and returns unambiguous relative-plus-calendar labels.
- **Example Call**:
  ```kotlin
  val header = DateTimeUtils.getGroupHeader(task.dueAt, userZoneId)
  ```

---

### 6. `DateTimeUtils.formatDueTime(epochMs: Long, zoneId: ZoneId): String`
- **Class**: `DateTimeUtils.kt`
- **Signature**: `fun formatDueTime(epochMs: Long, zoneId: ZoneId): String`
- **Parameters**: `epochMs: Long`, `zoneId: ZoneId`
- **Returns**: `String` (Formatted localized time string, e.g. `"1:30 PM"`)
- **What It Does**: Converts UTC epoch milliseconds into a localized 12-hour AM/PM string in the user's active timezone.
- **Example Call**:
  ```kotlin
  val timeText = DateTimeUtils.formatDueTime(task.dueAt, userZoneId)
  ```

---

*This document serves as your complete code, function, and library manual for your presentation and viva examination!*
