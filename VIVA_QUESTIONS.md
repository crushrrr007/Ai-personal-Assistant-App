# Technical Viva Examination Guide: Personal AI Assistant & Scheduler

This guide covers 5 tricky architectural and edge-case questions designed for technical viva examinations, project defenses, and engineering code reviews.

---

### Question 1: Timezone Integrity & Epoch Storage
**Q:** *Why are all task and reminder timestamps stored as UTC epoch milliseconds (`Long`) in SQLite/Room rather than formatted ISO-8601 strings or device-local timestamps? How does the application handle edge cases like Daylight Saving Time (DST) or a user traveling across time zones?*

**Concise Answer:**
- **Comparison & Ordering:** Epoch milliseconds provide numeric, non-ambiguous, monotonic values that can be indexed and queried directly in SQLite using standard mathematical operators (`due_at > :now`), avoiding expensive string parsing inside SQL queries.
- **Time Zone Independence:** A UTC epoch millisecond timestamp represents an absolute instant in the physical universe. If stored as local time (e.g., `2026-10-05 18:00`), crossing a timezone or changing device clocks causes the trigger time to warp.
- **DST & Presentation:** The timestamp is converted to human-readable strings using `java.time.ZonedDateTime` with `ZoneId.systemDefault()` strictly at presentation time in the UI layer. When DST shifts clocks by ±1 hour, the absolute alarm trigger point remains preserved without corrupting database records.

---

### Question 2: Exact Alarm Scheduling vs. WorkManager
**Q:** *Why did you choose `AlarmManager.setExactAndAllowWhileIdle()` instead of `WorkManager` for task deadlines and reminders? What are the battery implications under Android Doze mode, and how do you handle phone reboots?*

**Concise Answer:**
- **Deterministic Timing:** `WorkManager` is explicitly designed for deferrable background tasks and batches jobs to preserve battery. It cannot guarantee exact-minute firing (Google imposes minimum latency and Doze batching). In contrast, submission deadlines require deterministic, exact execution down to the second.
- **Doze Mode Exemption:** `setExactAndAllowWhileIdle()` guarantees that the CPU will wake up and deliver the `PendingIntent` even if the device is in deep Doze mode (`RTC_WAKEUP`).
- **Reboot Recovery:** Because the operating system clears all scheduled alarms from memory when the phone powers off, we implemented a dedicated `BootReceiver` registered to `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED`. Upon boot, it queries Room for all future pending tasks and reschedules their exact alarms.

---

### Question 3: LLM Resilience, Rate-Limits (HTTP 429) & Offline Fallbacks
**Q:** *What happens when the Gemini API encounters rate limits (HTTP 429 / Quota Exceeded), network degradation, or invalid JSON syntax from the model? How does the app prevent user disruption?*

**Concise Answer:**
- **Tiered Multi-Model Failover:** The `GeminiLlmService` maintains an automated fallback chain (`gemini-3.8-flash` → `gemini-3.5-flash` → `gemini-3.1-flash-lite-preview` → `gemini-flash-latest`). If a 429 or 503 is returned, it executes exponential backoff and transparently tries the next candidate model.
- **Defensive Parser & 1-Turn Repair:** If the LLM generates markdown code fences (````json ... ````), regex sanitizers strip them. If syntax is broken, it performs an automatic 1-turn retry enforcing schema constraints.
- **Deterministic Heuristic Fallback (`LocalRuleBasedParser`):** If network connectivity fails or all online model quotas are exhausted, natural language commands like *"Remind me to submit the assignment at 6 PM tomorrow"* are immediately resolved on-device via regex and date matchers. The user is never blocked from creating critical reminders.

---

### Question 4: Jetpack Compose State Management & Unidirectional Data Flow (UDF)
**Q:** *How does your UI architecture prevent redundant recompositions, race conditions, and memory leaks when observing continuous Room database streams?*

**Concise Answer:**
- **Cold Flows to Hot State:** Room emits cold `Flow<List<T>>` streams on database writes. In the `ViewModel`, these flows are converted into hot `StateFlow` instances using `.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ...)`:
  - The `5000ms` timeout prevents rapid cancellation/restart during transient configuration changes (such as rotating the phone).
- **Immutable UI States:** Screens collect state via `collectAsState()` or `collectAsStateWithLifecycle()`. UI components only emit user intent events (`onToggleDone`, `onSendMessage`) upward, following clean Unidirectional Data Flow (UDF).
- **Keyed Lazy Lists:** All `LazyColumn` items specify unique `key = { it.id }`, allowing Compose to intelligently reorder and animate items rather than re-rendering the entire list when an individual item's state changes.

---

### Question 5: Concurrency, Idempotency & Database Integrity
**Q:** *If a user taps 'Mark Done' on a push notification while simultaneously interacting with the task in the app, how does the system avoid race conditions, deadlocks, or duplicate reminder fires?*

**Concise Answer:**
- **Asynchronous Broadcast Receiver:** The `ReminderReceiver` calls `goAsync()` to obtain a `PendingResult`, delegating database queries to `Dispatchers.IO` inside a supervised coroutine scope before invoking `pendingResult.finish()`.
- **Atomic Single-Source-of-Truth:** All mutations route through Room's thread-safe SQLite transactions. Marking a task done executes an atomic SQL update: `UPDATE tasks SET status = 'done' WHERE id = :taskId`.
- **Cancellation Idempotency:** The `ReminderScheduler.cancelReminder(taskId)` method cancels the notification ID and removes both the primary alarm and the 15-minute follow-up nudge using unique `FLAG_NO_CREATE` / `FLAG_UPDATE_CURRENT` pending intent identifiers, preventing phantom notifications.
