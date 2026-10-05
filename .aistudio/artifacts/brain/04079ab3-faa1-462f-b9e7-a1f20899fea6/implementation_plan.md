# Implementation Plan: Comprehensive Project Viva & Architecture Guide

Generate a complete, rigorous, and presentation-ready documentation guide (`PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md`) to help you prepare your presentation slides (PPT) and ace tomorrow's viva examination.

## Proposed Documentation Structure

### 1. Project Overview & Pitch
- **Project Title & Identity**: AI Personal Coordinator & Smart Task Manager.
- **Problem Statement**: Traditional to-do apps require manual data entry, complex multi-tap date pickers, and lack smart context-aware scheduling.
- **Solution**: A privacy-conscious, offline-first personal assistant combining Google Gemini AI with local SQLite persistence and exact Android system alarm notifications.

### 2. End-to-End System Architecture
- **Architecture Pattern**: MVVM (Model-View-ViewModel) + Clean Architecture + Repository Pattern.
- **Data Flow Diagram**:
  `Compose UI` ⇆ `ViewModel (StateFlow)` ⇆ `Repository Layer` ⇆ `Room Database (SQLite)` & `System AlarmManager` & `Gemini LLM (REST API)`.
- **Key Design Principles**: Single Source of Truth (SSOT), Reactive Streams (`Flow`/`StateFlow`), Unidirectional Data Flow (UDF), Offline-First.

### 3. Component-by-Component Technical Deep Dive & File Directory
Detailed file-by-file breakdown explaining *What it does*, *How it works under the hood*, and *Exact file location*:
- **UI & Presentation (`com.example.ui`)**:
  - `MainActivity.kt` & `AppNavigation.kt`: Jetpack Compose navigation host, bottom nav bar, theme setup.
  - `today/TodayScreen.kt` & `TodayViewModel.kt`: Real-time greeting, daily statistics, next-up task, time-blocked daily plan.
  - `tasks/TasksScreen.kt` & `TasksViewModel.kt`: Reactive task grouping, date headers with explicit calendar dates, filter tabs (Today, Upcoming, Done), swipe/FAB actions.
  - `notes/NotesScreen.kt` & `NotesViewModel.kt`: Offline notes creation, search, card view.
  - `chat/ChatScreen.kt` & `ChatViewModel.kt`: Real-time chat stream, task modification cards, interactive confirmation dialogs.
  - `voice/VoiceInputHandler.kt`: Android `SpeechRecognizer` integration for voice commands.
  - `components/TimezoneSelectionDialog.kt` & `util/TimezoneManager.kt`: Real-world timezone auto-detection and persistence.
  - `util/DateTimeUtils.kt`: Date/time conversion between UTC epoch milliseconds and localized date strings.
- **Data & Persistence Layer (`com.example.data.local`)**:
  - `AppDatabase.kt`: Room database initialization, migration, SQLite open helper.
  - `Task.kt`, `TaskDao.kt`: Entity schema, reactive SQL queries using Kotlin Coroutines Flow.
  - `Note.kt`, `NoteDao.kt`: Offline note schema and search queries.
  - `ChatMessage.kt`, `ChatDao.kt`: Conversation persistence and message history.
  - `DailyPlanEntity.kt`, `DailyPlanDao.kt`: Stored time blocks for day planning.
- **Domain & Repository Layer (`com.example.data.repo`)**:
  - `TaskRepository.kt`: Bridge between database, AlarmManager, and UI. Includes undo stack.
  - `NoteRepository.kt`: Note lifecycle management.
  - `ChatRepository.kt`: Prepares AI prompt with database context, executes Gemini API calls, validates JSON schema, applies task modifications atomically.
  - `DailyPlanRepository.kt`: Feeds pending tasks to the LLM to generate daily time blocks.
- **AI & Natural Language Processing (`com.example.ai` & `com.example.data.remote`)**:
  - `GeminiLlmService.kt`: OkHttp + Kotlinx Serialization integration with Google Gemini 2.5 Flash using JSON-only schema mode.
  - `PromptBuilder.kt`: Context-aware prompt engine injecting current date/time, active timezone, upcoming tasks, and notes into system instructions.
  - `LocalIntentParser.kt`: Fallback rule-based NLP engine ensuring the app can parse reminders even with 0% internet connectivity.
- **Hardware & Android System Services (`com.example.reminder`)**:
  - `ReminderScheduler.kt`: Android `AlarmManager.setExactAndAllowWhileIdle` for guaranteed battery-efficient wake-ups.
  - `ReminderReceiver.kt`: `BroadcastReceiver` receiving the exact alarm trigger.
  - `NotificationHelper.kt`: High-importance `NotificationChannel` with vibration and sound.

### 4. PPT Slide-by-Slide Blueprint (10 Slides)
Ready-to-copy slide outlines with Slide Title, Bullet Points, and Speaker Notes:
- Slide 1: Title & Team Members
- Slide 2: Problem Statement & Motivation
- Slide 3: Proposed Solution & Core Features
- Slide 4: System Architecture & Tech Stack
- Slide 5: Jetpack Compose UI & State Management
- Slide 6: Offline-First Room SQLite Database
- Slide 7: Gemini AI Integration & Prompt Engineering
- Slide 8: Android Background Alarms & Notification Architecture
- Slide 9: Demo Screenshots & Walkthrough
- Slide 10: Conclusion & Future Scope

### 5. Top 20 Examiner Viva Questions & Model Answers
Comprehensive answers to likely questions asked by university/technical examiners:
1. *Why did you use Jetpack Compose instead of XML layouts?*
2. *Why use Room Database instead of direct SQLiteOpenHelper or SharedPreferences?*
3. *How does the app handle offline mode when Gemini API is unreachable?*
4. *Why AlarmManager instead of WorkManager for reminders?*
5. *How do you prevent SQL injection and data inconsistency in Room?*
6. *How do Kotlin Coroutines and StateFlow work in this app?*
7. *What is Unidirectional Data Flow (UDF)?*
8. *How do you enforce structured JSON output from Gemini?*
9. *How does the app handle timezone discrepancies between device and user?*
10. *How does the app schedule exact alarms in Android 12+ (SCHEDULE_EXACT_ALARM)?*
11. *What is the role of BroadcastReceiver in reminders?*
12. *Explain the MVVM architecture and separation of concerns.*
13. *What is LazyColumn and how is it optimized compared to RecyclerView?*
14. *How does speech-to-text work in the app?*
15. *How is user privacy protected regarding notes and tasks?*
16. *What are Room TypeConverters and why are they needed?*
17. *How does `setExactAndAllowWhileIdle()` handle Android Doze mode?*
18. *Why is `collectAsStateWithLifecycle()` preferred over `collectAsState()`?*
19. *What is the purpose of `remember` and `rememberSaveable` in Compose?*
20. *If your app scaled to 1,000,000 tasks, how would you optimize database queries?*

---

## Verification Plan
- Create `PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md` in the project root.
- Ensure all file paths, class names, methods, and architecture explanations precisely match the actual codebase.
- Verify the build using `compile_applet` to confirm the project remains completely healthy.
