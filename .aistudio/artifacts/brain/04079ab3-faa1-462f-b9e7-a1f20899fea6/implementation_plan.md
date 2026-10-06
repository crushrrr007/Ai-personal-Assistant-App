# Repository Cleanup & Export Preparation

Clean up and structure the repository for project export (ZIP export / GitHub repository) by organizing all documentation into a dedicated `/docs` folder, generating a professional academic and open-source `README.md`, optimizing `.gitignore`, and verifying build integrity.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed and govern this cleanup plan:

- **Confirmed Decision 1**: Move all six documentation and viva preparation guides (`PROJECT_PRESENTATION_SLIDES.md`, `PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md`, `SYSTEM_WORKFLOW_AND_DATA_FLOWS.md`, `KOTLIN_TO_JAVA_EXPLANATION_GUIDE.md`, `API_FUNCTIONS_AND_LIBRARIES_REFERENCE.md`, `VIVA_QUESTIONS.md`) into a dedicated `/docs` directory.
- **Confirmed Decision 2**: Create a comprehensive, production-grade `README.md` in the project root with architecture diagrams, setup instructions, feature highlights, and direct index links to all guides.
- **Confirmed Decision 3**: Optimize `.gitignore` for standard Android Studio / GitHub exports, removing unwanted caches and build artifacts.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the workspace into a clean, professional, and well-structured Android repository ready for university submission, external grading, GitHub publishing, and APK generation.
- **Target Audience / Persona**: Evaluators, examiners, developers cloning or importing the project in Android Studio.
- **Key Value**: Delivers an immediately intuitive repository where the root contains standard Android project files and a polished `README.md`, while all presentation slides and defense guides are neatly organized inside `/docs`.

---

## 2. Directory Structure & Organization

### Target Repository Layout

```
.
├── app/                           # Android application module (Compose UI, ViewModels, Room DB, Alarms)
│   ├── src/main/java/com/example/
│   │   ├── data/                  # Room Entities, DAOs, Repositories, Database
│   │   ├── domain/model/          # Clean Architecture Models (Task, Note, Plan, User)
│   │   ├── service/               # Gemini AI Service, Alarm Scheduler, Notification Helper
│   │   └── ui/                    # Jetpack Compose Screens (Today, Chat, Tasks, Notes, Auth)
│   └── src/main/res/              # Android Resources (Drawables, Values, Colors, Icons)
├── docs/                          # Dedicated Documentation & Viva Defense Folder
│   ├── PROJECT_PRESENTATION_SLIDES.md            # 12-slide Marp/PowerPoint defense deck
│   ├── PROJECT_VIVA_AND_ARCHITECTURE_GUIDE.md   # System architecture & top 20 viva Q&A
│   ├── SYSTEM_WORKFLOW_AND_DATA_FLOWS.md         # Mermaid & ASCII data flow traces
│   ├── KOTLIN_TO_JAVA_EXPLANATION_GUIDE.md       # Side-by-side Kotlin vs Java translations
│   ├── API_FUNCTIONS_AND_LIBRARIES_REFERENCE.md  # Architectural functions & libraries dictionary
│   └── VIVA_QUESTIONS.md                         # Quick-reference viva questions checklist
├── gradle/                        # Gradle wrapper & Version Catalog (libs.versions.toml)
├── .env.example                   # Environment variable template for Gemini API key
├── .gitignore                     # Production Android gitignore (caches, IDE files, keystores)
├── build.gradle.kts               # Root build configuration
├── gradle.properties              # JVM memory & Gradle settings
├── metadata.json                  # AI Studio platform descriptor
├── README.md                      # Comprehensive project documentation & getting started guide
└── settings.gradle.kts            # Project repositories and module settings
```

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dedicated `/docs` Directory vs Root Clutter**
  - *Chosen Approach*: Group all documentation in `/docs` and keep only `README.md` in the root.
  - *Why*: Adheres to standard open-source Android conventions. Android Studio and GitHub show a clean root directory without dozens of markdown files competing with build files.
- **Decision 2: Comprehensive `README.md` with Direct Index**
  - *Chosen Approach*: The root `README.md` acts as an executive portal linking to every document in `/docs`, providing quick setup commands (`./gradlew assembleDebug`), architecture highlights, and screenshots/feature summaries.
  - *Why*: Evaluators opening the GitHub repo or ZIP extract can read everything in one place with click-through links to the slide deck and viva guides.

---

## 4. Technical Architecture & Verification

### Step-by-Step Execution Plan
1. **Create `/docs` Folder & Relocate Documents**:
   - Move all 6 markdown guides into `/docs/`.
2. **Author Root `README.md`**:
   - Write a complete, polished `README.md` formatted with clean markdown, tech stack badges, system diagrams, and documentation links.
3. **Enhance `.gitignore`**:
   - Add standard patterns for `.build-outputs/`, OS files (`.DS_Store`, `Thumbs.db`), Android build caches, and sensitive files.
4. **Compile & Unit Test Verification**:
   - Run `compile_applet` and `gradle :app:testDebugUnitTest` to guarantee zero regressions.
