# Take Me — Native Android Build Spec & Agent Rules

This document is the single source of truth for rebuilding **Take Me** (a medication
adherence tracker) as a native Android app. It serves two purposes:
1. **Build spec** — what the app is, its data model, screens, and rules.
2. **Agent rules** — how the AI coding agent (Claude Code) must work when implementing.

The AI agent should read this document before implementing any feature and keep every
change consistent with it.

---

## PART 1 — AGENT WORKING RULES (read every time)

1. **One feature per task.** Never bundle multiple features or unrelated changes into a
   single implementation. Bundling is the top cause of regressions.
2. **State what you are NOT touching.** For every change, explicitly confirm which files,
   logic, and data structures you are leaving untouched.
3. **Summarize every file changed** after a task, so it can be reviewed.
4. **Plan before big changes.** For anything touching the data model, storage layer, or
   core logic (streaks, adherence), describe the plan first and wait for approval before
   writing code.
5. **Make minimal, scoped edits.** Do not refactor, rename, or "improve" code outside the
   requested change without flagging it first.
6. **Ask before assuming** on anything that affects data or core flows.
7. **No secrets in the repo.** This is a public repository. Never commit API keys,
   credentials, or tokens. (None are needed yet — the app is local-only for now.)
8. **Keep the build green.** After a change, the project must compile and run. If a change
   can't compile cleanly, stop and explain rather than leaving it broken.
9. **Beginner-friendly output.** The project owner is newer to Kotlin. Prefer giving
   complete, full files to paste in over "add this snippet at line N" edits. Explain what
   each new file/piece does in plain terms.

---

## PART 2 — TECH STACK & ARCHITECTURE

- **Package name:** `com.twonorth.takeme` (use this exact application ID from the first
  file onward — do not change it later).
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Min SDK:** target modern Android (agent to propose a sensible minSdk/targetSdk;
  test devices are Pixel 8 Pro and Pixel 9 Pro on current Android).
- **Architecture:** simple, modern, and beginner-readable. Recommended:
    - MVVM with `ViewModel` + Compose state
    - A single-activity app with Compose navigation
    - **Local-first storage using Room** (SQLite) — this is the on-device database.
- **No login and no cloud for now.** All data is stored on-device only. Auth and cloud
  sync (Supabase) are explicitly OUT OF SCOPE until the app's usability is proven. Design
  the storage layer so a cloud sync layer *could* be added later, but do not build it now.
- **Notifications:** native Android notifications (`NotificationManager` +
  `AlarmManager`/`WorkManager` for scheduling). This is a core feature and a key reason
  for building native. It is built as its own dedicated phase (see roadmap), not bolted on.

---

## PART 3 — WHAT THE APP IS

**Purpose:** Take Me helps people track whether they've taken their medication. It uses
the warmth and motivation of a habit tracker (streaks, gentle milestones) on top of
accurate medication logging. Its edge is being **calm, clean, and ad-free** — the opposite
of dense clinical pill apps.

**Target user (launch):** younger adults managing 1–3 medications who like habit-tracker
style apps. Calm and minimal beats clinical and dense.

**Design identity (carry over from the existing app):**
- Calm, single-column, uncluttered layouts. Mobile-first (it's a phone app).
- Warm amber accent as the primary colour (approx `#E0A458`-family amber — agent to set a
  Material 3 colour scheme built around a warm amber primary; keep it calm, not loud).
- Supports light and dark.
- Finger-sized touch targets; the "mark as taken" action is the primary, most prominent
  interaction on each medication.

---

## PART 4 — DATA MODEL

Three core entities (Room tables). Names are internal; user-facing labels say
"Medication," never "Habit."

### Medication
- `id` (primary key)
- `name` (String)
- `dosage` (String, optional, default empty)
- `notes` (String, optional, default empty)
- `color` (stored colour value/label for the medication's accent dot)
- `frequency` (see Frequency below)
- `reminderTime` (optional time, for later notification use)
- `timesPerDay` (Int, default 1 — reserved for future multi-dose; single dose for now)
- `position` (Int, for ordering in the list)
- `createdAt` (timestamp)

### DoseLog (a "taken" record)
- `id` (primary key)
- `medicationId` (foreign key → Medication)
- `date` (the day the dose was taken, one "taken" record per medication per day for now)
- `createdAt` (timestamp)

### SkipRecord (a "skipped with reason" record) — SEPARATE from DoseLog
- `id` (primary key)
- `medicationId` (foreign key → Medication)
- `date` (the day skipped)
- `reason` (stored value: `forgot` | `side_effects` | `ran_out` | `felt_better` | `other`)
- `note` (String, optional, only used when reason = `other`)
- `createdAt` (timestamp)

**Critical rule:** A SkipRecord is NOT a taken dose. It must live in its own table and must
NEVER be read by any streak or adherence calculation. Deleting a Medication must also delete
its DoseLogs and SkipRecords (cascade).

### Frequency
Three shapes:
- **Daily** — scheduled every day.
- **Specific days** — scheduled only on chosen weekdays.
- **Per week** — a target of X times per week.

---

## PART 5 — CORE LOGIC RULES

These rules are non-negotiable and must be preserved exactly:

1. **"Taken" is the only thing that counts toward adherence and streaks.** Only DoseLog
   records feed streaks, adherence, and the "taken today" state.
2. **Skips never affect streaks or adherence.** SkipRecords are for the user's own
   reference (and future reports) only. A skip must not raise or lower adherence, and must
   not break or extend a streak. It is neutral to all calculations.
3. **Mutual exclusivity per day:** on a given day for a given medication, taking clears any
   skip for that day, and skipping clears any taken record for that day. A dose is never
   both taken and skipped on the same day.
4. **Streak** = consecutive scheduled days completed (respecting the frequency — a day the
   medication isn't scheduled should not break the streak). Keep the streak warm and
   motivational in the UI.
5. **Adherence rate** = taken doses ÷ scheduled doses over a window. Skips are excluded
   entirely (a skipped day counts the same as an untaken scheduled day — it does not get
   credit).

---

## PART 6 — SCREENS

1. **Today** (home) — greeting + date; a single-column list of medications scheduled today,
   each showing name, colour dot, taken/not-taken state, a prominent **Mark as taken**
   action, and a small secondary **Skip** action. A progress indicator (e.g. "1 of 3
   taken") that only appears when there is something scheduled. An empty state that invites
   adding the first medication. A clear, persistent way to **add a medication**.
2. **Insights** — per-medication: current streak, longest streak (labelled "Best"),
   adherence rate, and a simple history view (e.g. a calendar/heatmap of the last ~30 days).
   Do not show stats to a brand-new user with no data.
3. **Settings** — theme, and a **Clear all data** action (wipes medications, dose logs, and
   skip records on-device). Later: export.
4. **Add / Edit medication** — a sheet/screen with: Medication name (with a searchable
   suggestion list of common generic medication names — names only, no dosages/advice, and
   the user can always type any name freely), dosage, notes, frequency picker, colour
   picker. A reminder field is reserved for the notifications phase.

### Onboarding (first launch)
- Fast and commitment-style: ask "Which medication are you starting to track?" → name it →
  offer to log the first dose immediately ("I've taken it just now" / "I'll log it later").
- After the first dose is logged, offer an optional "Add details?" step that opens the Edit
  screen for dosage/frequency/colour (skippable).
- No feature-tour / tutorial screens. No statistics before there is data.
- A flag stored on-device marks onboarding complete so it doesn't repeat. "Clear all data"
  resets this flag so onboarding shows again.

---

## PART 7 — TERMINOLOGY & COPY RULES

Two tones, applied by context:
- **Clinical/precise** for: medication name, dosage, schedule, "Mark as taken," adherence
  rate/history, and anything that will appear in a future report. Skip reason labels stored
  neutrally (`forgot`, `side_effects`, `ran_out`, `felt_better`, `other`).
- **Warm/motivational** for: streaks, milestone messages, onboarding, empty states, and the
  "What happened?" skip question (gentle framing, even though the stored reason is neutral).
- Rule of thumb: if it could appear on a doctor-facing report or be read clinically, keep it
  precise. If only the user sees it in the moment, warmth is fine. Tune overall toward warmth
  for the launch audience.
- UI copy: plain, active voice, sentence case. A button says what it does ("Add medication").
  Empty states invite action; errors say what happened and how to fix it, without apologising.

---

## PART 8 — CONSTRAINTS (permanent)

- **No drug interaction checking** — deliberately excluded (legal/liability). Do not add it.
- **No ads.**
- **Not medical advice** — any guidance-style text carries a brief disclaimer.
- **No login / no cloud yet** — on-device only until usability is proven.
- **Privacy:** medication data is sensitive health data. Store only what's needed; keep it
  on-device; never log it anywhere external.

---

## PART 9 — BUILD ROADMAP (incremental order)

Build and test each on a real Pixel before moving to the next. Each step should leave a
running app.

0. **Pipeline proof** — a bare app that builds and runs on the Pixel, showing a simple
   hardcoded list of medication names. No storage, no logic. Confirms the whole toolchain
   (VS Code + Claude Code + Android Studio + device) works end to end.
1. **Core loop** — Room database; add a medication; see it in the Today list; mark it taken;
   state persists across app restarts. Local only.
2. **Today view polish** — progress indicator, empty state, delete a medication, colour dots.
3. **Streaks & Insights** — streak, best streak, adherence rate, 30-day history view.
4. **Onboarding** — the fast commitment-style first-launch flow + "add details" step.
5. **Skip with reason** — Skip action + reason picker + separate SkipRecord storage, with
   mutual exclusivity, kept entirely out of streak/adherence.
6. **Medication name suggestions** — the searchable common-name list on the name field.
7. **Native notifications** — schedule and deliver medication reminders (the core native
   feature). Owner wants to be hands-on for this phase and its testing.
8. **Settings & Clear all data** — theme + full on-device wipe (also resets onboarding).
9. **Later (out of scope for now):** Supabase cloud sync + login, multi-dose per day, refill
   reminders, PDF/report export, caregiver read-only view.

---

## PART 10 — HOW WE WORK (the loop)

1. Planner (Opus 4.8, in chat) writes a precise task prompt against this spec.
2. Claude Code (in VS Code) implements it in Kotlin/Compose.
3. Owner pushes to the public GitHub repo and tests on the Pixel via Android Studio.
4. Owner reports results / pushes code; planner reviews the real code from GitHub and writes
   the next task.
5. Repeat, one feature at a time, keeping the build green.

---

## PART 11 — TECHNICAL GUARDRAILS (mistake-prevention for the agent)

These rules exist to prevent the most common ways an AI agent breaks a native Android
build. Follow them strictly.

### Versions & dependencies (most important)
- **Do NOT change the Gradle, Kotlin, AGP (Android Gradle Plugin), or compileSdk versions**
  generated by the Android Studio new-project wizard, unless the owner explicitly asks.
  Bumping these "to be helpful" is the top cause of sudden build failures. If you believe a
  version change is needed, STOP and explain why first.
- **Use the Gradle version catalog** (`gradle/libs.versions.toml`) for all dependencies and
  versions. Do not hardcode dependency versions inline in `build.gradle.kts`.
- **Use the Compose Bill of Materials (BOM)** so all Compose libraries share one consistent,
  compatible version set. Do not pin individual Compose library versions against the BOM.
- Prefer **stable, documented APIs**. Avoid experimental/alpha APIs and APIs requiring
  opt-in annotations unless there is no stable alternative — and if you must use one, flag
  it and explain why.
- Keep the dependency list minimal. Do not add a library when the standard toolkit
  (Compose, Room, coroutines, ViewModel, WorkManager) already covers the need.

### Project structure (keep consistent across all sessions)
Application ID / base package: `com.twonorth.takeme`. Organise code as:
- `com.twonorth.takeme` — `MainActivity`, app entry, navigation host.
- `.data` — Room entities, DAOs, the database class, and the repository.
- `.data.local` — anything storage-specific (e.g. the onboarding-complete flag via
  DataStore/SharedPreferences).
- `.ui` — Compose screens, grouped per screen (e.g. `.ui.today`, `.ui.insights`,
  `.ui.settings`, `.ui.medication` for add/edit, `.ui.onboarding`).
- `.ui.components` — shared Compose components (medication row, progress indicator, etc.).
- `.ui.theme` — colour scheme, typography, shapes.
- `.viewmodel` — ViewModels (or co-locate with each `.ui.<screen>` package — pick one
  approach and keep it consistent).
  Do not scatter files outside this structure or invent a new layout mid-project.

### Room (on-device database) specifics
- **Use destructive migrations during development.** The app has no real users and no data
  worth preserving yet, so `fallbackToDestructiveMigration()` is acceptable and preferred
  while building — it avoids migration bugs. (We will switch to proper migrations before a
  real launch; note that transition for later.)
- **Date/time storage — one consistent convention across ALL tables:**
    - Day-level dates (a dose's `date`, a skip's `date`): store as `String` in ISO
      `yyyy-MM-dd` format.
    - Timestamps (`createdAt`): store as `Long` epoch milliseconds.
    - Never mix formats between tables. Use the same helper for date formatting everywhere.
- All database access must be **off the main thread** (Room + Kotlin coroutines; DAO
  functions `suspend` or returning `Flow`). Never do DB reads/writes on the UI thread.
- Cascade deletes: deleting a Medication must delete its DoseLogs and SkipRecords (use Room
  foreign keys with `onDelete = CASCADE`).

### State & lifecycle
- Use `ViewModel` + Compose state so UI state survives configuration changes (screen
  rotation) and process death. Collect flows with lifecycle awareness.
- Do not hold database or context references in ways that leak across lifecycle.

### Strings & copy
- Put all user-facing text in `res/values/strings.xml`, not hardcoded in Compose. This keeps
  the clinical-vs-warm terminology rules and British English in one reviewable place.
- Follow Part 7 terminology rules for every string.

### Notifications (for the notifications phase — note now, build later)
- Android 13+ (API 33+) requires the runtime `POST_NOTIFICATIONS` permission — request it
  properly, don't assume it's granted.
- Scheduled reminders need `AlarmManager` (exact alarms may need special permission on newer
  Android) or `WorkManager` — propose the approach and confirm before building this phase.

### Self-verification & handing errors back
- After each task: (1) confirm the project compiles, (2) list files changed, (3) give a
  short **manual test script** — concrete "do this, expect that" steps the owner can run on
  the Pixel to confirm it works.
- When the owner reports a build or runtime error: ask for the **full error text** and the
  relevant file before proposing a fix. Diagnose from the real error — do not guess from a
  paraphrase.