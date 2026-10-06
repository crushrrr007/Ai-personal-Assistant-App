# 🤖 AI Personal Coordinator & Smart Task Manager

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-M3-4285F4)](https://m3.material.io/)
[![Room Database](https://img.shields.io/badge/AndroidX%20Room-2.7.0%20SQLite-3DDC84?logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Google Gemini](https://img.shields.io/badge/Gemini-2.5%20Flash%20API-4E80EE?logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-3DDC84)](https://developer.android.com)

A native Android personal coordinator that bridges conversational AI with deterministic mobile OS hardware services. It translates natural language text and speech into structured SQLite records, exact device alarms (`AlarmManager`), time-blocked daily schedules with buffer intervals, and quick notes—complete with a 100% offline-resilient regex fallback engine.

---

## 🌟 Key Features

* **Natural Language Intent Parsing**: Speak or type conversational commands (*"Remind me to submit the assignment tomorrow at 6 PM and review biology notes at 8 PM"*).
* **Compound Batch Actions**: Automatically splits compound user queries into multiple discrete entities (reminders, notes, and task schedules) in a single request.
* **Exact Hardware Alarms**: Bypasses unreliable background workers in favor of Android `AlarmManager` with `setExactAndAllowWhileIdle()` and `RTC_WAKEUP` to wake the device even during deep Doze Mode.
* **Autonomous Day Schedule Engine**: Computes realistic, time-blocked daily agendas featuring 10-minute transition buffers and energy management.
* **100% Offline-First Architecture**: When the device is offline or without cellular connectivity, a deterministic on-device regex and date parser takes over without error.
* **Encrypted Local Authentication**: Built-in Room database user authentication using SHA-256 with unique 16-byte random salts. Includes a one-tap **Guest Exploration Mode**.
* **Edge-to-Edge Material 3 UI**: Clean, responsive Jetpack Compose interface with proactive IME keyboard inset handling and smooth bottom navigation management.

---

## 🏗️ System Architecture

The application is engineered strictly following **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and **Unidirectional Data Flow (UDF)**.

```
┌─────────────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER (Jetpack Compose)            │
│   TodayScreen    │    ChatScreen    │   TasksScreen │  Notes    │
└────────────────────────────────▲────────────────────────────────┘
                                 │ UI State (StateFlow)
                                 ▼ User Events
┌─────────────────────────────────────────────────────────────────┐
│                   VIEWMODEL LAYER (AndroidX Lifecycle)          │
│   TodayViewModel │   ChatViewModel  │  TaskViewModel │ AuthVM   │
└────────────────────────────────▲────────────────────────────────┘
                                 │ Coroutines / Flow
                                 ▼ Repository Pattern
┌─────────────────────────────────────────────────────────────────┐
│                      DATA & HARDWARE LAYER                      │
│  ┌───────────────────────────┐    ┌──────────────────────────┐  │
│  │   Room SQLite Database    │    │   AlarmManager Engine    │  │
│  │   (5 Relational Tables)   │    │   (RTC_WAKEUP Exact)     │  │
│  └───────────────────────────┘    └──────────────────────────┘  │
│  ┌───────────────────────────┐    ┌──────────────────────────┐  │
│  │   Gemini 2.5 Flash API    │    │   Local Regex Fallback   │  │
│  │   (Few-Shot JSON Schema)  │    │   (On-Device Offline)    │  │
│  └───────────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📚 Project Documentation & Viva Defense Guides

All detailed technical documentation, presentation slide decks, and academic viva guides are organized inside the [`/docs`](./docs) directory:

| Document | Description | Direct Link |
| :--- | :--- | :--- |
| 📽️ **Presentation Slides** | 12-slide Marp & PowerPoint-compatible defense presentation deck with speaker notes. | [`docs/PROJECT_PRESENTATION_SLIDES.md`](./docs/PROJECT_PRESENTATION_SLIDES.md) |
| 🎓 **Viva & Architecture Guide** | Complete system architecture deep-dive and Top 20 examiner viva Q&A with model answers. | [`docs/PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md`](./docs/PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md) |
| 🔄 **System Workflows & Data Flows** | Detailed Mermaid and ASCII sequence diagrams illustrating end-to-end execution flows. | [`docs/SYSTEM_WORKFLOW_AND_DATA_FLOWS.md`](./docs/SYSTEM_WORKFLOW_AND_DATA_FLOWS.md) |
| ☕ **Kotlin to Java Guide** | Side-by-side Kotlin vs Java translations, architectural comparisons, and speaking scripts. | [`docs/KOTLIN_TO_JAVA_EXPLANATION_GUIDE.md`](./docs/KOTLIN_TO_JAVA_EXPLANATION_GUIDE.md) |
| 📖 **API & Library Reference** | Comprehensive dictionary of functions, libraries, parameters, and signatures. | [`docs/API_FUNCTIONS_AND_LIBRARIES_REFERENCE.md`](./docs/API_FUNCTIONS_AND_LIBRARIES_REFERENCE.md) |
| ❓ **Viva Questions Checklist** | Quick-reference checklist of core viva voce questions and technical talking points. | [`docs/VIVA_QUESTIONS.md`](./docs/VIVA_QUESTIONS.md) |

---

## 📁 Repository Directory Structure

```
.
├── app/                           # Android application module
│   ├── src/main/java/com/example/
│   │   ├── data/                  # Room Entities, DAOs, Repositories, Database
│   │   ├── domain/model/          # Clean Architecture Models (Task, Note, Plan, User)
│   │   ├── service/               # Gemini AI Service, Alarm Scheduler, Notification Helper
│   │   └── ui/                    # Jetpack Compose Screens (Today, Chat, Tasks, Notes, Auth)
│   └── src/main/res/              # Android Resources (Drawables, Values, Colors, Icons)
├── docs/                          # Comprehensive Documentation & Viva Defense Guides
│   ├── PROJECT_PRESENTATION_SLIDES.md
│   ├── PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md
│   ├── SYSTEM_WORKFLOW_AND_DATA_FLOWS.md
│   ├── KOTLIN_TO_JAVA_EXPLANATION_GUIDE.md
│   ├── API_FUNCTIONS_AND_LIBRARIES_REFERENCE.md
│   └── VIVA_QUESTIONS.md
├── gradle/                        # Gradle wrapper & Version Catalog (libs.versions.toml)
├── .env.example                   # Environment variable template for Gemini API key
├── .gitignore                     # Production Android gitignore configuration
├── build.gradle.kts               # Root build configuration
├── gradle.properties              # JVM memory & Gradle settings
├── metadata.json                  # AI Studio platform descriptor
├── README.md                      # Project documentation (this file)
└── settings.gradle.kts            # Project repositories and module settings
```

---

## 🚀 Getting Started & How to Run

### Prerequisites
* **Android Studio**: Android Studio Hedgehog / Iguana / Jellyfish (or newer)
* **JDK**: Java Development Kit 17 or 21
* **Android SDK**: API Level 34+ (targetSdk is 36)

### Installation Steps
1. **Clone or Extract the Repository**:
   ```bash
   git clone <repository-url>
   cd assistant-app
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select **Open**.
   - Browse to the project folder and click **OK**.
   - Allow Gradle to sync dependencies automatically.
3. **Configure Environment Secrets (Optional)**:
   - For Gemini AI features, configure your API key in `.env` (or via the **Secrets panel** in AI Studio):
     ```bash
     cp .env.example .env
     # Add: GEMINI_API_KEY=your_gemini_api_key_here
     ```
   - *Note: If no API key is provided, the app continues to function seamlessly using the on-device regex parsing engine!*
4. **Build and Run**:
   - Select an Android Virtual Device (AVD) or connect a physical Android device with USB debugging enabled.
   - Click the green **Run (Shift+F10)** button.

### Running Unit Tests
Execute the local JVM unit and Robolectric test suite via Gradle:
```bash
gradle :app:testDebugUnitTest
```

---

## 🔒 Security & Privacy

* **Zero Cloud Tracking**: All tasks, daily plans, notes, and user account records reside in the local SQLite database on the device.
* **Cryptographic Passwords**: Passwords are hashed using one-way SHA-256 with a unique 16-byte cryptographically secure salt.
* **Granular Permissions**: Declares only required Android permissions (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`).

---

## 👤 Author & Credits

* **Developer / Presenter**: Himanshu Meena
* **Framework**: Android Native (Kotlin & Jetpack Compose)
* **Platform**: Google AI Studio Build
