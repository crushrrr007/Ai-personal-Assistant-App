# 🎓 Comprehensive Project Viva & System Architecture Guide
**Project Title**: AI Personal Coordinator & Smart Task Manager (Android Native)  
**Author / Developer**: Himanshu Meena  
**Core Technologies**: Kotlin, Jetpack Compose, Material 3, Android Jetpack Room (SQLite), Coroutines & Flow, Google Gemini 2.5 Flash API, Android AlarmManager & NotificationChannel  

---

## 📌 Table of Contents
1. [Executive Summary & Problem Statement](#1-executive-summary--problem-statement)
2. [High-Level System Architecture & Data Flow](#2-high-level-system-architecture--data-flow)
3. [Component-by-Component Deep Dive & Code Directory](#3-component-by-component-deep-dive--code-directory)
   - [Presentation / UI Layer](#a-presentation--ui-layer)
   - [Domain & Repository Layer](#b-domain--repository-layer)
   - [Local Persistence Layer (Room Database)](#c-local-persistence-layer-room-database)
   - [AI & Natural Language Processing Layer](#d-ai--natural-language-processing-layer)
   - [Android System Services & Hardware Reminders](#e-android-system-services--hardware-reminders)
4. [Slide-by-Slide PPT Blueprint (10 Presentation Slides)](#4-slide-by-slide-ppt-blueprint)
5. [Top 20 Viva Questions & Model Answers](#5-top-20-viva-questions--model-answers)

---

## 1. Executive Summary & Problem Statement

### The Problem
Traditional mobile to-do and calendar applications suffer from significant friction:
1. **Manual Overload**: Users must manually type titles, open calendar pickers, scroll through minute/hour dials, and configure alarms one-by-one.
2. **Context Blindness**: Standard tools cannot interpret contextual, conversational statements like *"Reschedule my MAD review to tomorrow 1:30 PM and remind me 30 mins before"*.
3. **Privacy Concerns & Cloud Lock-in**: Most modern AI assistants stream personal tasks, notes, and habits to foreign cloud databases, preventing offline access and raising privacy concerns.
4. **Timezone & Ambiguity Errors**: Cloud-based tools often miscalculate "today" vs "tomorrow" due to server vs. client clock differences.

### The Solution
The **AI Personal Coordinator** is an **offline-first, privacy-respecting Android application** that merges conversational AI with native device infrastructure:
- **Zero-Friction Scheduling**: Speak or type naturally; Google Gemini 2.5 Flash parses intents, entities, and relative dates into structured JSON.
- **100% Local SQLite Persistence**: All tasks, notes, and chat history reside locally in an encrypted Room database on the device.
- **Hardware-Level Exact Alarms**: Integrates with Android's `AlarmManager` and `BroadcastReceiver` so alarms fire with sound and vibration even in Android Doze mode or when the app is terminated.
- **Dual Engine (AI + Local Fallback)**: If the user is offline, an embedded rule-based regex intent parser processes scheduling commands seamlessly.
- **Timezone Awareness**: Full timezone auto-detection and reactive UI grouping with explicit date headers (e.g. `Tomorrow (Tue, Oct 6)`).

---

## 2. High-Level System Architecture & Data Flow

The project strictly follows **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and the **Repository Pattern** with **Unidirectional Data Flow (UDF)**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (UI)                         │
│   Jetpack Compose | Material 3 | LazyColumn | StateFlow Collection     │
│   [TodayScreen]     [TasksScreen]     [NotesScreen]     [ChatScreen]   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Events (Clicks, Inputs, Voice)
                                    ▼ State (StateFlow / UDF)
┌────────────────────────────────────────────────────────────────────────┐
│                          VIEWMODEL LAYER                               │
│     [TodayViewModel]  [TasksViewModel]  [NotesViewModel]  [ChatViewModel] │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Coroutine Scopes (viewModelScope)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         REPOSITORY LAYER                               │
│  [TaskRepository]   [NoteRepository]   [DailyPlanRepository]  [ChatRepository]
└──────────┬────────────────────────┬──────────────────────┬─────────────┘
           │                        │                      │
           ▼                        ▼                      ▼
┌─────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  LOCAL PERSISTENCE  │  │   ANDROID SYSTEM     │  │   REMOTE / AI LAYER  │
│   Room Database     │  │     SERVICES         │  │   Google Gemini API  │
│   (SQLite Engine)   │  │   AlarmManager       │  │   (JSON Schema Mode) │
│ - Tasks Table       │  │   BroadcastReceiver  │  │          +           │
│ - Notes Table       │  │   NotificationHelper │  │  Local Regex Fallback│
│ - Chat Table        │  │   SpeechRecognizer   │  │                      │
│ - Daily Plans Table │  │                      │  │                      │
└─────────────────────┘  └──────────────────────┘  └──────────────────────┘
```

### Unidirectional Data Flow (UDF) Pipeline
1. **User Action**: The user clicks a button or inputs speech in a Composable.
2. **ViewModel Event**: The Composable delegates the action to a method in `ViewModel`.
3. **Repository Execution**: The `ViewModel` launches a coroutine calling the appropriate `Repository`.
4. **Single Source of Truth**: The Repository updates the `Room Database` (or triggers `AlarmManager`).
5. **Reactive Emission**: Room emits a new snapshot via Kotlin `Flow`.
6. **State Emission**: The ViewModel transforms the Flow into `StateFlow` using `.stateIn(...)`.
7. **Recomposition**: The Compose UI observes `uiState` with `collectAsStateWithLifecycle()` and automatically recomposes only the updated widgets.

---

## 3. Component-by-Component Deep Dive & Code Directory

### A. Presentation / UI Layer
*Directory: `/app/src/main/java/com/example/ui/`*

#### 1. `MainActivity.kt`
- **Location**: `/app/src/main/java/com/example/MainActivity.kt`
- **Role**: The single Activity container for the entire application.
- **How It Works**:
  - Calls `enableEdgeToEdge()` for immersive Material 3 window insets.
  - Instantiates ViewModels using custom `ViewModelProvider.Factory` connected to `AssistantApplication` repositories.
  - Hosts `AppNavigation(navController, ...)`.

#### 2. `navigation/AppNavigation.kt`
- **Location**: `/app/src/main/java/com/example/ui/navigation/AppNavigation.kt`
- **Role**: Type-safe screen navigation and bottom navigation bar.
- **How It Works**:
  - Uses `NavHost` and `rememberNavController()`.
  - Defines 4 primary destinations: `Today`, `Tasks`, `Notes`, `Chat`.
  - Configures animated transitions, icon indicators, and badge counts.

#### 3. `today/TodayScreen.kt` & `TodayViewModel.kt`
- **Location**: `/app/src/main/java/com/example/ui/today/`
- **Role**: The daily dashboard and command center.
- **How It Works**:
  - Displays dynamic greeting (Good morning/afternoon/evening) based on the user's active timezone.
  - Shows real-time statistics cards: Tasks Done, Left, and Deadlines.
  - Displays the "Next Up" task (earliest deadline or pending task).
  - Offers "Plan My Day": calls Gemini to create an hourly time-blocked schedule with 10-minute rest intervals.
  - Houses the Quick Task Entry Bar at the bottom for instant task creation.

#### 4. `tasks/TasksScreen.kt` & `TasksViewModel.kt`
- **Location**: `/app/src/main/java/com/example/ui/tasks/`
- **Role**: Full task lifecycle management with filtering and grouping.
- **How It Works**:
  - Combines `allTasks` flow with filter tabs (`Today`, `Upcoming`, `Done`) and `TimezoneManager.userZoneIdFlow`.
  - Groups tasks reactively using `DateTimeUtils.getGroupHeader(epochMs, zoneId)`.
  - Headers display unambiguous, explicit calendar dates (e.g. `Today (Mon, Oct 5)`, `Tomorrow (Tue, Oct 6)`).
  - Features an interactive **Timezone Chip** in the header displaying current timezone (e.g. `IST (GMT+5:30)`).
  - Supports task completion toggles, deletion with snackbar Undo, and full task creation/editing dialogs.

#### 5. `notes/NotesScreen.kt` & `NotesViewModel.kt`
- **Location**: `/app/src/main/java/com/example/ui/notes/`
- **Role**: Offline notebook and knowledge base.
- **How It Works**:
  - Full CRUD operations (Create, Read, Update, Delete) for personal notes.
  - Real-time search query filtering over note titles and body content.
  - Notes saved here are injected into the Gemini AI prompt so the assistant can answer questions about your notes!

#### 6. `chat/ChatScreen.kt` & `ChatViewModel.kt`
- **Location**: `/app/src/main/java/com/example/ui/chat/`
- **Role**: Natural language chat interface and proactive coordinator.
- **How It Works**:
  - Streams conversational bubbles between User and AI.
  - Renders interactive **Task Modification Confirmation Cards**: when the user asks to reschedule or delete a task, the assistant proposes the change visually with `Confirm` and `Decline` buttons before executing.
  - Integrated with `VoiceInputHandler` for microphone speech-to-text.

#### 7. `components/TimezoneSelectionDialog.kt` & `util/TimezoneManager.kt`
- **Location**: `/app/src/main/java/com/example/ui/components/` & `/app/src/main/java/com/example/util/`
- **Role**: Multi-timezone support and auto-detection.
- **How It Works**:
  - `TimezoneManager` holds `_userZoneIdFlow` and `_isAutoDetectFlow`, backed by `SharedPreferences`.
  - `TimezoneSelectionDialog` provides a searchable dialog with popular international zones (IST, US PT, ET, GMT, etc.) and auto-detect toggle.
  - Any change to the timezone reactively recomputes dates across all screens without restarting the app.

#### 8. `util/DateTimeUtils.kt`
- **Location**: `/app/src/main/java/com/example/ui/util/DateTimeUtils.kt`
- **Role**: Timezone-aware date and time formatting.
- **How It Works**:
  - Converts database UTC epoch milliseconds to localized date/time strings.
  - Formats group headers with explicit labels: `Today (Mon, Oct 5)`, `Tomorrow (Tue, Oct 6)`.

---

### B. Domain & Repository Layer
*Directory: `/app/src/main/java/com/example/data/repo/`*

#### 1. `TaskRepository.kt`
- **Location**: `/app/src/main/java/com/example/data/repo/TaskRepository.kt`
- **Role**: Mediates between UI, database, and system alarms.
- **How It Works**:
  - Exposes `allTasks: Flow<List<Task>>`.
  - When `insertTask(task)` is called: saves to `TaskDao`, and if `dueAt != null`, immediately invokes `reminderScheduler.scheduleReminder(savedTask)`.
  - When a task is marked `DONE` or deleted: automatically cancels the scheduled alarm via `reminderScheduler.cancelReminder(task.id)`.

#### 2. `NoteRepository.kt`
- **Location**: `/app/src/main/java/com/example/data/repo/NoteRepository.kt`
- **Role**: Manages note persistence and search queries.

#### 3. `ChatRepository.kt`
- **Location**: `/app/src/main/java/com/example/data/repo/ChatRepository.kt`
- **Role**: The brain connecting AI with device databases.
- **How It Works**:
  1. Gathers current device state: upcoming tasks from `TaskRepository`, notes from `NoteRepository`, and recent chat messages.
  2. Constructs prompt via `PromptBuilder` passing the user's active timezone and exact local date/time.
  3. Sends request to `LlmService.generateResponse()`.
  4. Parses the JSON output:
     - If `intent == "create_task"`: Inserts task into database and schedules exact alarm.
     - If `intent == "modify_task"`: Emits proposed task modification for user confirmation.
     - If `intent == "create_note"`: Inserts note into database.
     - If `intent == "answer_notes"`: Answers user questions based on offline notes.
  5. If the network call fails or throws an exception, seamlessly executes `LocalIntentParser` as a zero-network fallback!

#### 4. `DailyPlanRepository.kt`
- **Location**: `/app/src/main/java/com/example/data/repo/DailyPlanRepository.kt`
- **Role**: Generates and persists time-blocked schedules.
- **How It Works**:
  - Prompts Gemini to organize pending tasks into realistic blocks between working hours (e.g. 12:00 to 22:00).
  - Enforces 10-minute rest buffers between blocks.
  - Stores accepted plans in the `daily_plans` SQLite table.

---

### C. Local Persistence Layer (Room Database)
*Directory: `/app/src/main/java/com/example/data/local/`*

#### 1. `AppDatabase.kt`
- **Location**: `/app/src/main/java/com/example/data/local/AppDatabase.kt`
- **Role**: Room Database holder (`assistant_database.db`).
- **Features**:
  - Thread-safe Singleton pattern using `@Volatile` and `synchronized(this)`.
  - Declares 4 entities: `Task`, `Note`, `ChatMessage`, `DailyPlanEntity`.

#### 2. Database Entities & DAOs
- **`Task.kt` / `TaskDao.kt`**:
  - Schema: `id` (Auto-increment PK), `title`, `dueAt` (Long UTC ms), `isDeadline` (Boolean), `remindBeforeMin` (Int), `estimatedMin` (Int), `status` (pending/done), `source` (ai/user).
  - DAO provides reactive `@Query("SELECT * FROM tasks ORDER BY dueAt ASC") fun getAllTasks(): Flow<List<Task>>`.
- **`Note.kt` / `NoteDao.kt`**:
  - Schema: `id`, `title`, `body`, `tags`, `createdAt`, `updatedAt`.
  - Full search with SQL `LIKE %:query%`.
- **`ChatMessage.kt` / `ChatDao.kt`**:
  - Schema: `id`, `role` (user/assistant), `text`, `intent`, `proposedTaskModJson`, `timestamp`.
- **`DailyPlanEntity.kt` / `DailyPlanDao.kt`**:
  - Schema: `date` (PK string like "2026-10-06"), `planJson`, `createdAt`.

---

### D. AI & Natural Language Processing Layer
*Directory: `/app/src/main/java/com/example/ai/` & `/app/src/main/java/com/example/data/remote/`*

#### 1. `GeminiLlmService.kt`
- **Location**: `/app/src/main/java/com/example/data/remote/GeminiLlmService.kt`
- **Role**: Communicates with Google Gemini 2.5 Flash via REST API.
- **How It Works**:
  - Uses `OkHttp` with timeouts (30s connect, 60s read).
  - Endpoint: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`.
  - Configures `responseMimeType = "application/json"` to guarantee the model outputs raw JSON matching our schema without markdown backticks.

#### 2. `PromptBuilder.kt`
- **Location**: `/app/src/main/java/com/example/ai/PromptBuilder.kt`
- **Role**: Context engineering and few-shot prompt formulation.
- **How It Works**:
  - Injects active timezone and ISO-8601 local timestamp:
    `CURRENT LOCAL DATE & TIME: Tuesday, October 6, 2026 1:30 PM (Asia/Kolkata)`
  - Explicitly states: `"Today is Tuesday, Oct 6. Tomorrow is Wednesday, Oct 7."`
  - Injects currently scheduled tasks so the AI can resolve references (e.g. *"Push the MAD review back 2 hours"*).
  - Injects saved offline notes so the AI can answer personal queries.

#### 3. `LocalIntentParser.kt`
- **Location**: `/app/src/main/java/com/example/ai/LocalIntentParser.kt`
- **Role**: 100% offline, zero-network fallback NLP engine.
- **How It Works**:
  - Employs compiled regular expressions to match time keywords ("at 4 PM", "tomorrow at 10:30 AM", "in 20 minutes").
  - Ensures the user can still create reminders when airplane mode is on or in remote areas.

---

### E. Android System Services & Hardware Reminders
*Directory: `/app/src/main/java/com/example/reminder/`*

#### 1. `ReminderScheduler.kt`
- **Location**: `/app/src/main/java/com/example/reminder/ReminderScheduler.kt`
- **Role**: Interacts with Android OS `AlarmManager`.
- **How It Works**:
  - Computes trigger time: `task.dueAt - (task.remindBeforeMin * 60 * 1000L)`.
  - Creates an explicit `PendingIntent` targeting `ReminderReceiver`.
  - Uses `alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)`:
    - **RTC_WAKEUP**: Wakes up the device CPU if the screen is off.
    - **setExactAndAllowWhileIdle**: Bypasses Android Doze mode power restrictions to ensure the alarm fires at the exact minute.

#### 2. `ReminderReceiver.kt`
- **Location**: `/app/src/main/java/com/example/reminder/ReminderReceiver.kt`
- **Role**: Background `BroadcastReceiver`.
- **How It Works**:
  - Receives the system broadcast when the alarm fires.
  - Extracts task title, ID, and deadline flags from Intent extras.
  - Delegates to `NotificationHelper.showTaskReminder(...)`.

#### 3. `NotificationHelper.kt`
- **Location**: `/app/src/main/java/com/example/reminder/NotificationHelper.kt`
- **Role**: Creates and displays Android notifications.
- **How It Works**:
  - Creates high-importance notification channels (`IMPORTANCE_HIGH`) with vibration patterns and default alarm audio.
  - Adds quick actions: "Mark Complete" directly from the notification shade.

---

## 4. Slide-by-Slide PPT Blueprint

You can directly copy the text below into PowerPoint or Google Slides:

### 📽️ Slide 1: Title Slide
- **Title**: AI Personal Coordinator & Smart Task Manager
- **Subtitle**: An Offline-First, Privacy-Centric Android Assistant Powered by Jetpack Compose & Gemini AI
- **Presented by**: Himanshu Meena
- **Key Badges**: Android Native (Kotlin) • Jetpack Compose • Room Database • Gemini 2.5 Flash

### 📽️ Slide 2: Problem Statement & Motivation
- **Manual Overhead**: Setting reminders in standard apps takes 6–8 taps across separate date/time pickers.
- **Lack of Natural Understanding**: Inability to interpret relative phrases ("this Tuesday at 4", "remind me 30 mins before").
- **Privacy Intrusion**: Many AI apps upload personal life data to third-party cloud servers.
- **Timezone Inconsistencies**: Server-side scheduling creates date offsets when server and client timezones differ.

### 📽️ Slide 3: Proposed Solution
- **Conversational Intelligence**: Talk or type naturally; AI extracts titles, due dates, reminder offsets, and priorities.
- **100% Offline-First Architecture**: Core tasks, notes, and schedules persist on-device in SQLite via Room.
- **Exact System Alarms**: Uses Android `AlarmManager` (`RTC_WAKEUP`) to guarantee timely alerts even in deep Doze mode.
- **Dual Engine Reliability**: Automatic fallback to local regex parser when offline.

### 📽️ Slide 4: System Architecture
- **Architecture Pattern**: MVVM + Clean Architecture + Repository Pattern.
- **Unidirectional Data Flow**: User Event ➔ ViewModel ➔ Repository ➔ Room Database ➔ UI Recomposition.
- **Single Source of Truth**: UI never holds raw data; it observes reactive `StateFlow` streams.

### 📽️ Slide 5: Presentation Layer (Jetpack Compose & Material 3)
- **100% Declarative UI**: No legacy XML layouts; written entirely in modern Jetpack Compose.
- **Smart Screens**:
  - *Today*: Dynamic time-of-day greeting, statistical cards, Next-Up deadline, and AI Day Planner.
  - *Tasks*: Filter tabs (Today/Upcoming/Done), explicit date headers (`Tomorrow (Tue, Oct 6)`).
  - *Chat*: Conversational stream with interactive Task Modification Confirmation cards.
  - *Notes*: Offline notebook indexed into AI context.

### 📽️ Slide 6: Data Persistence & Local Storage
- **Jetpack Room (SQLite)**:
  - 4 Normalized Tables: `tasks`, `notes`, `chat_messages`, `daily_plans`.
  - Reactive queries returning Kotlin `Flow<List<T>>`.
- **SharedPreferences**: Stores active timezone preference with auto-detection.
- **Privacy Guarantee**: Zero personal data stored in external clouds.

### 📽️ Slide 7: AI Integration & Prompt Engineering
- **Google Gemini 2.5 Flash**: Lightning-fast, cost-effective inference.
- **JSON Schema Mode**: Forces the LLM to output strict JSON conforming to our schema.
- **Context Injection**: Dynamically provides the LLM with:
  - Exact localized date, day of week, and timezone offset.
  - Existing tasks (for rescheduling and deletion).
  - Saved notes (for personal QA).

### 📽️ Slide 8: Android Hardware & System Reminders
- **AlarmManager Integration**: `setExactAndAllowWhileIdle()` wakes device CPU from sleep.
- **BroadcastReceiver**: Handles alarm intent in background.
- **Heads-Up Notifications**: High-importance channel with custom vibration and "Mark Done" quick actions.
- **Speech-to-Text**: Native `SpeechRecognizer` for hands-free voice input.

### 📽️ Slide 9: Key Technical Highlights & Challenges Solved
- **Timezone Synchronization**: Solved cloud emulator vs. user local time discrepancies by injecting active client timezone into the AI prompt and UI grouping.
- **Safe Modifications**: Proposed task edits require visual user confirmation before database modification.
- **Resilience**: Zero crashes on network disconnects due to dual-layer local fallback.

### 📽️ Slide 10: Conclusion & Future Scope
- **Conclusion**: A production-grade, highly responsive Android assistant that respects user privacy and eliminates scheduling friction.
- **Future Enhancements**:
  - On-device local SLM (e.g. Gemini Nano) for 100% offline conversational reasoning.
  - Wear OS companion app for smartwatch reminder management.
  - Google Calendar two-way synchronization.

---

## 5. Top 20 Viva Questions & Model Answers

### Q1: Why did you use Jetpack Compose instead of traditional XML layouts?
> **Answer**: Jetpack Compose is Android's modern declarative UI toolkit. It drastically reduces boilerplate code, eliminates view-binding synchronization bugs, and inherently enforces Unidirectional Data Flow (UDF). In XML, UI state is mutable and distributed across views (`findViewById`); in Compose, UI is a direct mathematical function of state (`UI = f(State)`). When state changes, Compose intelligently recomposes only the affected composables, leading to higher performance and cleaner architecture.

### Q2: Explain the MVVM architecture implemented in your app.
> **Answer**: We followed Model-View-ViewModel:
> - **View (Jetpack Compose)**: Renders UI and captures user interactions. It observes state and emits events. It contains zero business logic.
> - **ViewModel (`TasksViewModel`, `ChatViewModel`, etc.)**: Survived across configuration changes (e.g., screen rotation). Holds UI state in `StateFlow` and executes business logic via `viewModelScope`.
> - **Model (`TaskRepository`, `Room Database`)**: Handles data persistence, network calls, and caching. The Repository acts as the Single Source of Truth.

### Q3: Why did you choose Room Database over direct SQLiteOpenHelper or SharedPreferences?
> **Answer**: While `SharedPreferences` is only suitable for simple key-value pairs (like timezone settings), `Room` is an abstraction layer over SQLite that provides:
> 1. Compile-time verification of SQL queries (catching SQL syntax errors during build time rather than runtime crashes).
> 2. Direct integration with Kotlin Coroutines and `Flow` for reactive, observable database streams.
> 3. Automatic object mapping (POJO to database rows) without error-prone manual `Cursor` indexing.

### Q4: How does your app ensure reminders fire on time when the device is in Doze Mode?
> **Answer**: Android 6.0 introduced Doze mode to save battery by deferring background jobs and alarms. Standard alarms and `WorkManager` jobs are delayed until maintenance windows. To overcome this for critical reminders, our `ReminderScheduler` uses `alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)`. The `RTC_WAKEUP` flag wakes the device's CPU, and `setExactAndAllowWhileIdle` instructs the OS that this alarm is exempt from Doze mode delays.

### Q5: Why did you use `AlarmManager` instead of `WorkManager` for reminders?
> **Answer**: `WorkManager` is designed for deferrable, opportunistic background work (like syncing data or uploading logs) and does not guarantee exact-to-the-minute execution. `AlarmManager` is designed for time-critical, user-facing events (like alarms and calendar reminders) where an exact trigger time is mandatory.

### Q6: How does the app handle offline scenarios when there is no internet connection?
> **Answer**: The app is architected with an **offline-first** philosophy:
> 1. All tasks, notes, and plans are stored locally in Room SQLite, so creating, checking off, and editing tasks requires zero network.
> 2. For natural language scheduling, `ChatRepository` wraps the Gemini API call in a `try-catch` block. If network failure occurs, it automatically routes the user prompt to `LocalIntentParser`, an on-device regex-based intent engine that extracts dates and creates tasks locally.

### Q7: How do you enforce structured JSON output from Google Gemini 2.5 Flash?
> **Answer**: In `GeminiLlmService.kt`, we pass `"responseMimeType": "application/json"` in the generation configuration. Additionally, in `PromptBuilder.kt`, we provide a strict JSON schema with few-shot format definitions. This guarantees that the model returns pure JSON without markdown backticks (````json ... ````) or conversational filler text, allowing reliable parsing into Kotlin data classes.

### Q8: What was the timezone discrepancy issue and how did you resolve it?
> **Answer**: When running on cloud containers or remote emulators, the system clock defaults to UTC or Pacific Time (UTC-7). If the user is in India (IST / UTC+5:30), the server clock might be Sunday night while the user's real calendar day is already Monday. 
> We resolved this by:
> 1. Creating `TimezoneManager` with user-controlled and auto-detect timezone settings.
> 2. Injecting the user's exact active timezone and ISO-8601 local timestamp into Gemini's system instructions, anchoring all relative dates ("tomorrow", "this Tuesday") to the user's timezone.
> 3. Formatting task headers with explicit dates: `Today (Mon, Oct 5)` and `Tomorrow (Tue, Oct 6)`.

### Q9: What is the difference between `StateFlow` and `SharedFlow`? Where did you use each?
> **Answer**:
> - **`StateFlow`**: A state-holder observable flow that always holds a current value (replay cache = 1) and conflates consecutive identical values. We used it for UI states like `uiState: StateFlow<TasksUiState>`.
> - **`SharedFlow`**: A hot flow that emits one-time transient events without holding a persistent state. We used it for one-shot UI events like `TaskUiEvent.ShowSnackbar` to trigger undo toasts without repeating upon screen rotation.

### Q10: Why did you use `collectAsStateWithLifecycle()` instead of `collectAsState()`?
> **Answer**: Standard `collectAsState()` keeps collecting flow emissions as long as the composable is in the composition, even if the app is in the background or the screen is off, consuming battery and CPU cycles. `collectAsStateWithLifecycle()` is lifecycle-aware (from `androidx.lifecycle.compose`); it automatically pauses flow collection when the app enters `STOPPED` state and resumes when `STARTED`, conserving device resources.

### Q11: How do you prevent race conditions or database locks in Room?
> **Answer**: Room handles multi-threading via Kotlin Coroutines. DAO operations use `suspend` functions executing on `Dispatchers.IO`. Room manages its own SQLite transaction locks and connection pooling, ensuring that concurrent reads and writes do not corrupt the database.

### Q12: How does the conversational Task Modification Confirmation work?
> **Answer**: When the user requests an edit (e.g. *"Move my MAD review to 4 PM"*), rather than silently altering data, the AI outputs a `proposed_task_mod` object containing `task_id`, `action: "update"`, and `new_due_at`. `ChatScreen` renders this as an interactive Material 3 Card with "Confirm" and "Decline" buttons. Only when the user taps "Confirm" does the app update Room and reschedule the alarm.

### Q13: What permissions did you declare in `AndroidManifest.xml` and why?
> **Answer**:
> - `android.permission.INTERNET`: To communicate with the Gemini API.
> - `android.permission.POST_NOTIFICATIONS`: Required on Android 13+ (API 33) to display reminder heads-up notifications.
> - `android.permission.SCHEDULE_EXACT_ALARM`: Required on Android 12+ (API 31) to set exact alarms in `AlarmManager`.
> - `android.permission.RECEIVE_BOOT_COMPLETED`: To reschedule active alarms if the device reboots.
> - `android.permission.RECORD_AUDIO`: For speech-to-text voice assistant inputs.
> - `android.permission.VIBRATE`: For tactile alarm notifications.

### Q14: What is the role of `BroadcastReceiver` in your application?
> **Answer**: `ReminderReceiver` extends `BroadcastReceiver`. It serves as the entry point when `AlarmManager` fires an alarm. It receives the intent even if the application is killed or in the background, extracts the task details from intent extras, and displays the notification via `NotificationManager`.

### Q15: How does the Speech-to-Text feature work in the app?
> **Answer**: We implemented `VoiceInputHandler.kt` utilizing Android's native `SpeechRecognizer` API (`android.speech.SpeechRecognizer`). When the microphone button is pressed, it begins listening, converts audio stream into text via `RecognitionListener`, and populates the text field in `ChatScreen` or `TodayScreen`.

### Q16: How do you handle configuration changes (like screen rotation) without losing state?
> **Answer**: In Jetpack Compose + MVVM:
> 1. ViewModels survive Activity recreation because their lifecycle is bound to the `ViewModelStoreOwner`.
> 2. Transient UI state inside composables (like input text) is preserved using `rememberSaveable { mutableStateOf("") }`, which saves state into the Android `Bundle`.

### Q17: What are Room Type Converters and did you need any?
> **Answer**: SQLite only natively stores basic data types (NULL, INTEGER, REAL, TEXT, BLOB). When storing complex objects (like Lists, Dates, or Custom JSON objects), Room requires `@TypeConverter` methods to serialize the object into a supported primitive (e.g. converting `List<String>` to a comma-separated String or JSON String) and back.

### Q18: What is Unidirectional Data Flow (UDF) and what are its advantages?
> **Answer**: In UDF, state flows *down* from ViewModels to UI components, and events flow *up* from UI components to ViewModels. Advantages:
> - **Testability**: You can unit test ViewModels independently of Android UI frameworks.
> - **Debugging**: State is predictable; there is a single place where state is modified.
> - **Decoupling**: UI components are stateless and reusable.

### Q19: If this app scaled to 500,000 tasks, how would you maintain performance?
> **Answer**:
> 1. **Database Indexing**: Add `@Index(value = ["dueAt", "status"])` to the `Task` entity to speed up date-range queries from $O(N)$ full table scans to $O(\log N)$ binary tree searches.
> 2. **Paging 3 Library**: Replace `Flow<List<Task>>` with `PagingSource<Int, Task>` to load tasks in memory chunks of 30 items as the user scrolls.
> 3. **Archiving Completed Tasks**: Move completed tasks older than 30 days to an archive table.

### Q20: What makes this project unique compared to existing todo apps on the Play Store?
> **Answer**: Most todo apps are either passive data entry lists (Todoist, Google Tasks) or cloud-heavy chatbots (ChatGPT) with no native device integration. Our app bridges both worlds: it provides the conversational intelligence of an LLM with the deep hardware integration, privacy, exact alarms, and instant speed of a 100% native offline SQLite Android app.

---

## 💡 Quick Tips for Tomorrow's Viva
1. **Live Demo Sequence**:
   - Open **Today Screen**: Point out the dynamic greeting, timezone chip, and "Next Up" task.
   - Open **Chat**: Send a voice or text command: *"Schedule MAD Project Presentation for tomorrow at 2:30 PM with a 15 min reminder"*.
   - Show how the AI returns structured confirmation, creates the task in **Tasks Screen** with header `Tomorrow (Tue, Oct 6)`, and registers the exact alarm in Android OS.
   - Switch timezone in the top bar to show instant reactive UI updates.
2. **Key Buzzwords to Emphasize**:
   - *Declarative Jetpack Compose*, *Offline-First Architecture*, *Exact RTC_WAKEUP Alarms*, *Doze Mode Resistance*, *Structured JSON LLM Prompting*, *Unidirectional Data Flow (UDF)*.

*Good luck with your viva and presentation! You have a robust, production-grade project.*
