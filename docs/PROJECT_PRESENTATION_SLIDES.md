---
marp: true
theme: gaia
_class: lead
paginate: true
backgroundColor: #0f172a
color: #f8fafc
header: 'AI Personal Coordinator & Smart Task Manager'
footer: 'Academic Project Viva Defense | Himanshu Meena'
style: |
  section {
    font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    padding: 40px;
  }
  h1 {
    color: #38bdf8;
  }
  h2 {
    color: #818cf8;
    margin-bottom: 20px;
  }
  h3 {
    color: #a5b4fc;
  }
  strong {
    color: #38bdf8;
  }
  code {
    background-color: #1e293b;
    color: #f1f5f9;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 0.85em;
  }
  pre {
    background-color: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 12px;
  }
  ul {
    font-size: 0.9em;
    line-height: 1.5;
  }
  footer {
    color: #64748b;
    font-size: 0.55em;
  }
  header {
    color: #64748b;
    font-size: 0.55em;
  }
---

# 🤖 AI Personal Coordinator & Smart Task Manager
### Native Android Application with On-Device Persistence & Multimodal Intent Parsing

**Presenter / Developer**: Himanshu Meena  
**Domain**: Mobile Computing, Artificial Intelligence & Distributed Systems  
**Core Stack**: Kotlin | Jetpack Compose | AndroidX Room | Google Gemini 2.5 Flash | AlarmManager  

---

<!-- Speaker Notes:
Good morning/afternoon, respected examiners and faculty members. Today I am presenting my project: the AI Personal Coordinator & Smart Task Manager. 
This is a modern Android native application designed to bridge conversational AI with deterministic, hardware-backed mobile OS services. 
Unlike typical chatbot apps that just produce text, this system translates natural language into structured, executable entities: exact system alarms, relational SQLite records, and time-blocked daily schedules.
-->

## 📌 Slide 2: Problem Statement & Motivation

### The Productivity Bottleneck in Modern Task Management
* **High Manual Entry Friction**: Users must tap through date pickers, time dropdowns, reminder intervals, and category selectors just to record a simple reminder.
* **Isolated "Chatbot" Silos**: Modern LLM chat apps offer intelligence but cannot schedule actual exact device alarms or store relational data offline.
* **Rigid Day Planning**: Static todo lists fail to calculate realistic transition buffers, cognitive energy blocks, and conflict management throughout a workday.
* **Network Vulnerability**: Cloud-only productivity solutions completely freeze and become unusable when the device loses cellular or Wi-Fi connectivity.

**Key Objective**: Develop an intelligent, privacy-first mobile coordinator that parses unstructured user voice/text commands into real Android OS actions with 100% offline fallback capabilities.

---

<!-- Speaker Notes:
Examiners often ask: "Why did you build another todo or reminder app?" 
Our research showed that users abandon reminder apps primarily due to input friction—it takes 6 to 8 taps just to set a reminder with a specific date and time.
Furthermore, generic LLMs like ChatGPT or Claude run in an isolated sandbox. If you ask them "remind me to call John tomorrow at 5 PM", they just reply with text; they cannot schedule an actual Android hardware alarm. 
Our project solves both problems: you talk naturally, and the app directly schedules exact hardware alarms in the Android OS while storing data safely on-device.
-->

## 💡 Slide 3: Proposed Solution & Core Innovation

### Bridging Conversational Intelligence with Deterministic Mobile Execution

* **Zero-Friction NLP Parsing**: Speak or type naturally: *"Remind me to submit assignment tomorrow at 6 PM and review biology notes at 8 PM"*.
* **Batch Multi-Action Execution**: Automatically splits compound user queries into multiple discrete entities (reminders, notes, and task schedules) in a single interaction.
* **Autonomous Day Schedule Engine**: Computes time-blocked daily agendas with intelligent 10-minute transition buffers and urgency scoring.
* **True Hardware Alarm Integration**: Bypasses unreliable background workers in favor of Android `AlarmManager` with `RTC_WAKEUP` exact precision.
* **Privacy & Local Ownership**: Powered by a local SQLite Room Database with encrypted user authentication; user data never lives on third-party tracking servers.

---

<!-- Speaker Notes:
Here are the core innovations of this project:
First, multi-action intent resolution: a single sentence can create multiple reminders and notes simultaneously.
Second, an autonomous Day Planning engine that blocks time on your schedule and inserts realistic 10-minute buffers between activities so you don't burn out.
Third, hardware-level exact alarms that ring even when the phone is locked in deep sleep Doze Mode.
And fourth, zero cloud lock-in: all your tasks, notes, and credentials stay securely stored in local SQLite Room tables.
-->

## 🏗️ Slide 4: High-Level System Architecture

### Clean Architecture & Unidirectional Data Flow (UDF)

```
┌─────────────────────────────────────────────────────────────┐
│               PRESENTATION LAYER (Jetpack Compose)          │
│   TodayScreen  │  ChatScreen  │  TasksScreen  │  NotesScreen│
└──────────────────────────────▲──────────────────────────────┘
                               │ UI State (StateFlow)
                               ▼ User Events / Intents
┌─────────────────────────────────────────────────────────────┐
│                 VIEWMODEL LAYER (AndroidX Lifecycle)        │
│   TodayViewModel │ ChatViewModel │ TaskViewModel │ NotesVM  │
└──────────────────────────────▲──────────────────────────────┘
                               │ Coroutines / Kotlin Flow
                               ▼ Repository Pattern
┌─────────────────────────────────────────────────────────────┐
│                   DATA & INTEGRATION LAYER                  │
│  ┌─────────────────────────┐     ┌───────────────────────┐  │
│  │   Room SQLite Database  │     │   AlarmManager Engine │  │
│  │   (Offline First Cache) │     │   (RTC_WAKEUP Exact)  │  │
│  └─────────────────────────┘     └───────────────────────┘  │
│  ┌─────────────────────────┐     ┌───────────────────────┐  │
│  │   Gemini 2.5 Flash API  │     │   Local Regex Parser  │  │
│  │   (Structured JSON NLP) │     │   (100% Offline Mode) │  │
│  └─────────────────────────┘     └───────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

<!-- Speaker Notes:
This diagram illustrates our end-to-end architectural design. 
We strictly follow Google's recommended Modern Android Architecture:
At the top is the Presentation Layer written 100% declaratively in Jetpack Compose and Material 3.
In the middle is the ViewModel Layer, which holds state using Kotlin StateFlow and survives configuration changes like screen rotations.
At the bottom is the Data Layer with the Repository pattern. Notice the dual execution engine: when online, requests go to Gemini 2.5 Flash with strict JSON schema enforcement; when offline, our local regex parser takes over seamlessly.
-->

## 🛠️ Slide 5: Technology Stack & Engineering Rationale

| Layer / Component | Technology Selected | Technical Rationale |
| :--- | :--- | :--- |
| **Language** | **Kotlin 2.2** | First-class coroutines, null-safety, official Google standard for Android. |
| **UI Framework** | **Jetpack Compose (M3)** | Declarative, reactive UI; eliminates legacy XML boilerplate and View binding. |
| **Local Persistence** | **AndroidX Room (SQLite)** | Compile-time SQL validation, reactive Flow queries, seamless migrations. |
| **AI Intelligence** | **Google Gemini 2.5 Flash** | Sub-second latency, superior few-shot instruction following, strict JSON mode. |
| **Networking** | **OkHttp 4.10 + Coroutines** | Lightweight HTTP client with custom timeout interceptors and secure headers. |
| **Task Scheduling** | **Android AlarmManager** | Exact hardware-level alarm triggers; guaranteed execution during Doze mode. |
| **Security** | **SHA-256 + Salt Hashing** | One-way cryptographic password hashing for local user accounts. |

---

<!-- Speaker Notes:
If examiners ask: "Why did you pick Room over SQLiteOpenHelper or SharedPreferences?"
Answer: Room provides compile-time verification of SQL queries—if you make a typo in a column name, the project fails to compile, eliminating runtime crashes. Furthermore, Room returns reactive Kotlin Flows, so whenever a task is added or completed, the UI automatically updates without manual refreshes.
If asked: "Why Gemini 2.5 Flash over Gemini Pro or OpenAI?"
Answer: Flash offers sub-second inference latency, which is essential for conversational mobile responsiveness, while providing native JSON schema adherence.
-->

## 🧠 Slide 6: AI Intent Parsing & Structured Execution

### From Unstructured Natural Language to Deterministic Actions

```
[ User Input ]  "Remind me to submit report at 4 PM and buy groceries"
       │
       ▼
[ PromptBuilder ]
  - Injects current ISO timestamp & device timezone
  - Few-shot system prompt requiring strict JSON schema
       │
       ▼
[ Gemini 2.5 Flash REST API ] (or Local Regex Parser if offline)
       │
       ▼
[ Structured JSON Response ]
  {
    "action": "CREATE_TASK_BATCH",
    "tasks": [
      { "title": "Submit report", "time": "16:00", "urgency": "HIGH" },
      { "title": "Buy groceries", "time": null, "urgency": "MEDIUM" }
    ],
    "assistantMessage": "Scheduled your report reminder and added groceries to your list."
  }
       │
       ▼
[ IntentDispatcher ] ➔ Room DB Insert ➔ Exact AlarmManager Schedule
```

---

<!-- Speaker Notes:
This slide highlights the NLP pipeline. 
One major technical challenge with mobile LLM integrations is timezone mismatch. If the server runs in UTC and the user is in IST (UTC+5:30), "tomorrow at 9 AM" could be parsed incorrectly.
We solved this in PromptBuilder.kt: before each API call, the app queries the Android device's ZoneId, calculates the exact local reference date and time, and anchors the prompt with this contextual data.
The model returns pure JSON, which Moshi deserializes directly into Kotlin data classes, triggering task insertions and alarm scheduling.
-->

## 💾 Slide 7: Local Data Persistence (Room Database)

### Relational Entity-Relationship Design (5 Core Tables)

* **`tasks` Table**:
  * `id (PK)`, `title`, `scheduledDate`, `scheduledTime`, `urgency`, `isCompleted`, `reminderTriggered`
  * Index on `(scheduledDate, isCompleted)` for fast agenda queries.
* **`notes` Table**:
  * `id (PK)`, `title`, `content`, `category`, `createdAt`, `colorTag`
* **`daily_plans` Table**:
  * `id (PK)`, `date`, `totalFocusMinutes`, `tasksScheduleJson`, `generatedAt`
* **`chat_messages` Table**:
  * `id (PK)`, `senderRole` (User / Assistant), `content`, `timestamp`, `actionMetadata`
* **`users` Table**:
  * `id (PK)`, `username`, `passwordHash`, `salt`, `createdAt`

---

<!-- Speaker Notes:
Our local database architecture consists of 5 normalized Room entities.
Notice that each entity has proper primary keys, typed converters for Date and Time instances, and indexed columns for fast queries.
By using Kotlin Flow return types on all DAO methods, our UI reacts instantaneously to changes. For example, when a user completes a task in TodayScreen, TasksScreen and ChatScreen reflect the update instantly without polling.
-->

## ⏰ Slide 8: Background Scheduling & Exact Alarms

### Overcoming Android Battery Optimizations (Doze Mode)

* **Why WorkManager Was Not Enough**:
  * `WorkManager` is designed for deferrable, batched jobs (e.g., sync, backup). It cannot guarantee exact-minute execution due to OS battery batching.
* **The Solution: Exact AlarmManager**:
  * Uses `AlarmManager.setExactAndAllowWhileIdle()` configured with `AlarmManager.RTC_WAKEUP`.
  * Wakes the device CPU out of deep sleep (Doze mode) at the exact scheduled second.
* **BroadcastReceiver & Notification Pipeline**:
  * `ReminderReceiver` catches the alarm intent ➔ extracts task ID and title.
  * Verifies task completion state in Room DB before alerting.
  * Dispatches high-priority heads-up notification with action buttons (*"Mark Complete"*, *"Snooze 10m"*).
  * Plays system alert sound and vibration pattern.

---

<!-- Speaker Notes:
Examiners love asking about background processing in Android!
They will ask: "Why didn't you use WorkManager for reminders?"
Your answer: WorkManager does not guarantee exact-time execution because Google designed it for battery-efficient deferrable tasks. For reminders where a student needs to submit an assignment at exactly 6:00 PM, WorkManager could be delayed by 15 to 45 minutes if the device is in Doze Mode.
Therefore, we implemented AlarmManager with setExactAndAllowWhileIdle() and RTC_WAKEUP, combined with a BroadcastReceiver and NotificationChannel with HIGH importance.
-->

## 🛡️ Slide 9: Offline-First Architecture & Resilience

### Uninterrupted User Experience Without Connectivity

```
                     ┌────────────────────────┐
                     │ User Command Submitted │
                     └───────────┬────────────┘
                                 │
                     ┌───────────▼────────────┐
                     │ Network State Monitor  │
                     └─────┬────────────┬─────┘
           Network Online  │            │  Network Offline
                           ▼            ▼
               ┌───────────────┐    ┌────────────────────┐
               │ Gemini 2.5 LLM│    │ Local Regex Engine │
               │ Cloud Parsing │    │ (Deterministic)    │
               └───────┬───────┘    └─────────┬──────────┘
                       │                      │
                       └──────────┬───────────┘
                                  │ Parsed Intent
                                  ▼
                      ┌───────────────────────┐
                      │  Room SQLite Database │
                      │  & AlarmManager (OS)  │
                      └───────────────────────┘
```

---

<!-- Speaker Notes:
A common pitfall of AI applications is that they become useless paperweights as soon as the user enters an elevator, airplane, or low-connectivity area.
In our application, we built an on-device fallback parser in LocalIntentParser.kt.
When connectivity fails or the API times out, the app automatically falls back to regex and date heuristics.
Commands like "Remind me to call Mom at 5 PM" or "Note down: groceries" are parsed directly on the phone and saved to SQLite without throwing an error to the user.
-->

## 🔐 Slide 10: Security & Authentication Architecture

### Zero-Cloud Privacy & Cryptographic Hashing

* **Local-First Security Posture**:
  * No user passwords or personal tasks are transmitted to external marketing or telemetry servers.
  * API communication with Gemini uses HTTPS TLS 1.3 with secure API key injection via Gradle BuildConfig.
* **Cryptographic Credential Storage**:
  * Passwords are never stored in plaintext.
  * Stored using **SHA-256 with Unique Salt** generated via `SecureRandom`.
  * Prevents Rainbow Table attacks even if the database file is extracted.
* **Guest Exploration Mode**:
  * Allows examiners and first-time users to evaluate all application capabilities without mandatory registration.
  * Isolated session management via `AuthRepository` reactive state.

---

<!-- Speaker Notes:
Security and privacy are vital evaluation criteria.
Instead of relying on insecure plaintext storage, our AuthRepository implements industry-standard SHA-256 cryptographic hashing with a 16-byte random salt per user.
Furthermore, we added a convenient "Continue as Guest" mode so evaluators can test all features immediately without having to create an account first.
-->

## ⚙️ Slide 11: Technical Challenges Overcome

### Key Engineering Hurdles Solved During Development

1. **Android 13+ & 14 Exact Alarm Policies**:
   * Resolved strict runtime permission requirements (`SCHEDULE_EXACT_ALARM` & `POST_NOTIFICATIONS`) with dynamic permission launchers and fallback intent links.
2. **Keyboard IME Double-Padding in Jetpack Compose**:
   * Solved edge-to-edge keyboard layout bug by scoping `Modifier.imePadding()` specifically to the input surface while consuming insets across `NavigationShell`'s `Scaffold`.
3. **Compound Multi-Action Query Resolution**:
   * Engineered few-shot prompt definitions and batch database transaction routines to handle compound requests (*"Set 2 reminders and write a note"*) atomically.
4. **Timezone Desynchronization**:
   * Dynamically injected client `ZoneId.systemDefault()` into LLM context to prevent server-side UTC time distortion.

---

<!-- Speaker Notes:
This slide demonstrates problem-solving depth. Examiners want to see what went wrong and how you fixed it.
For example, in Android 13+, Google introduced strict runtime permission restrictions for notifications and exact alarms. We implemented graceful permission launchers so the app never crashes.
Another challenge was IME keyboard padding: under edge-to-edge mode, applying imePadding at both the screen and scaffold levels caused a double-offset bug where the text field jumped halfway up the screen. We resolved this by scoping insets cleanly to the input bar.
-->

## 🎯 Slide 12: Conclusion & Viva Voce Q&A

### Summary of Achievements
* ✅ **Production-Ready Android Native App**: 100% Kotlin & Jetpack Compose.
* ✅ **Real System Integration**: True exact alarms, high-priority notifications, and reactive Room SQLite persistence.
* ✅ **Intelligent & Resilient**: Google Gemini 2.5 Flash with deterministic on-device regex fallback.
* ✅ **Academic & Software Engineering Best Practices**: Clean Architecture, MVVM, UDF, and zero dead-end affordances.

---

# 🎓 Thank You!
### Open for Questions and Technical Discussion

**Project Repository & Files Ready for Inspection**:
* `PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md` (Comprehensive Guide & Top 20 Q&A)
* `SYSTEM_WORKFLOW_AND_DATA_FLOWS.md` (Mermaid & ASCII Workflow Traces)
* `KOTLIN_TO_JAVA_EXPLANATION_GUIDE.md` (Kotlin vs Java Concepts)
