# ArchiveKeep — Agent Guidelines

This file contains rules and conventions for AI agents working on the ArchiveKeep codebase.

---

## Project Overview

ArchiveKeep is a **Kotlin Multiplatform** application for reliable file archivation with integrity checks. It synchronizes files across multiple storage locations while preventing corruption propagation.

- **Language**: Kotlin
- **Build system**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **UI**: Jetpack Compose Multiplatform (shared UI across desktop and Android)
- **Dependency injection**: Metro (`dev.zacsweers.metro`)
- **Async**: Kotlin Coroutines + Flow
- **Target platforms**: Desktop (JVM), Android
- **Modules**: all code lives under `modules/`

---

## Git Commit Message Rules

- **Audience**: Non-technical users and future developers — the message should be understandable without reading the code.
- **Format**: Use conventional commits (`feat:`, `fix:`, `ui:`, etc.) followed by a single, concise sentence.
- **Focus on the "what" and "why"**: Describe what changed from the user's perspective and the benefit, not the internal implementation details.
- **Structure**: One sentence maximum — name the feature or fix, then explain the user-facing impact.
- **Conventional commit types**:
  - `feat:` — new user-visible feature or functionality that changes how the app works or what it can do
  - `fix:` — corrects incorrect behavior, crashes, data issues, or broken workflows (the app was doing the wrong thing)
  - `ui:` — presentation-layer changes with no new functionality or bug fix: restyling, visual polish, layout adjustments, theme changes, adding/removing visual elements, conditional visibility, UI behavior, or anything that only affects how things look or feel or behave on UI without change to how functionality works
  - `docs:` — documentation changes (README, CHANGELOG, guides, agent instructions)
  - `refactor:` — internal code changes with **no user-visible effect** (do not use if the change looks different to the user)
  - `chore:` — maintenance tasks, dependencies, build config
- **Wording rules**:
  - Use plain language — avoid technical terms like "refactor," "optimize," "implement," or internal class/method names.
  - Describe the actual change, not existing behavior that was already present before it.
  - Prefer direct phrasing: "to show X" or "to allow X" over wordier alternatives like "to let users see X."
  - Include temporal qualifiers when relevant: "live," "instant," "real-time" — they convey important behavioral differences.
  - Name the feature plainly if it has a user-visible name (e.g., "quick-add," "quick scan") — but don't use backticks or code formatting.
  - Explain the benefit, not the mechanism: "without waiting for the full scan" instead of "bypassing the discovery phase."
  - Keep it action-oriented: start with a verb ("add," "allow," "fix," "improve").
  - Cut redundant filler: if the verb already conveys the outcome, drop the rest. For example, "cancelling" implies stopping something in progress, so "allow cancelling verify contents operation" is sufficient — don't add "so users can stop file verification in progress."
- **Avoid**:
  - Internal jargon ("index," "procedure," "supervisor," "discovery phase").
  - Implementation details ("add `startImmediate()` method," "refactor IndexUpdateProcedure").
  - Vague descriptions ("improve performance," "fix bug," "update logic").
  - Repetition: don't repeat the same word (e.g., "performance tab showing performance").
  - Backticks or code formatting around feature names.
  - Multiple sentences or semicolons — keep it to one clear statement.
  - Mislabeling `ui:` changes as `fix:` — hiding empty content, toggling visibility, or adjusting what the user sees is a `ui:` change, not a bug fix. A `fix:` is reserved for incorrect behavior, crashes, or data issues.
- **Template**: `type: [verb] [feature name] to [user benefit]`
  - Optionally append: `without/instead of [pain point]` when it adds clarity.
- **Examples**:
  - ✅ `feat: add live performance tab to show where time is spent during repository operations`
  - ✅ `feat: add quick-add to let users add new files to the index without waiting for the full scan`
  - ✅ `feat: share Google Drive login across repositories to avoid signing in for each one`
  - ✅ `fix: prevent duplicate files from being added when repository path changes`
  - ✅ `ui: replace operation progress tabs with button-style design to improve visual consistency across the app`
  - ✅ `ui: hide performance tab when tracker has no recorded stats`
  - ❌ `refactor: replace operation progress tabs with button-style design` — visible change, not a refactor
  - ❌ `style: restyle tab buttons with filled background` — UI change, not code formatting
  - ❌ `feat: implement startImmediate() in IndexUpdateProcedureSupervisor`
  - ❌ `fix: handle edge case in discovery result processing`
  - ❌ `fix: hide performance tab when tracker has no recorded stats` — hiding empty content is a `ui:` change, not a bug fix
  - ❌ `feat: add performance tab showing performances of tracked internal operations`
  - ❌ `feat: save Google Drive login securely for use across repositories` — secure saving already existed; sharing is the change
- **Validation**: After drafting, ask: "Would someone who doesn't know the codebase understand what changed and why it matters?"
- **One-liner rule**: Commit messages must be a single line — no multi-line commits, no bullet points, no elaboration. If the change is way too complex or big, ask whether multi-line is acceptable.

---

## Development Environment

- **JDK location**: Look for JDKs in `~/.jdks/` — this is where JetBrains IDEs download and install JDKs. On immutable distros like Bluefin, system-level JDK installation is not available, so this directory is the primary source.

---

## Licensing

The project is licensed under **GNU General Public License v3**. All new code must be compatible with GPL-3.0. Avoid dependencies that are not fully FOSS (e.g., ObjectBox was dropped for this reason).

---

## Release preparation

See `dev/prepare-new-release.md` for the step-by-step release workflow (changelog, version bump, Flatpak metainfo).
See `dev/prepare-dev-for-next-release.md` for bumping to the next SNAPSHOT version after a release is published.
