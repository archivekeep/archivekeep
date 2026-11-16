# Preparing a New Release

This document describes the steps to prepare a new release of ArchiveKeep. It's intended for developers and AI agents working on the project.

## Overview

A release requires changes to three files:

1. **`CHANGELOG.md`** — document user-facing changes since the last release
2. **`gradle.properties`** — remove the `-SNAPSHOT` suffix from the version and set `versionCode`
3. **`distribution/resources/org.archivekeep.ArchiveKeep.metainfo.xml`** — add the release entry for Flatpak / Flathub

Proceed sequentially and ask for confirmation after step 2 before updating the Flatpak file.

---

## Step 0: Determine version bump type

Ask whether this release is a **patch** or **minor** version bump:

- **Patch bump** (typical): Increment the patch number, keeping minor unchanged. The version with `-SNAPSHOT` suffix is usually already set in `gradle.properties`.  
  Example: `0.8.3` → `0.8.4`
- **Minor bump**: Increment the minor number and reset patch to 0.  
  Example: `0.8.x` → `0.9.0`

### Version Code Convention

`versionCode` follows the `[minor][patch]` format — a two-digit number where the first digit(s) represent the minor version and the last digit represents the patch version.

| Version | versionCode |
|---------|--------------|
| 0.7.0   | 70           |
| 0.8.0   | 80           |
| 0.8.3   | 83           |
| 0.8.4   | 84           |
| 0.9.0   | 90           |

---

## Step 1: Collect changes since the last release

Identify the last release tag and list all commits since then:

```bash
# Find the last release tag
git tag --list 'v*' --sort=-version:refname | head -5

# List commits since the last release (adjust tag)
git log v0.8.2..HEAD --oneline
```

Classify each commit into changelog categories:

- **Added** — new user-visible features (`feat:`)
- **Changed** — behavior or UI changes (`feat:` that modifies rather than adds, `ui:`, `refactor:` with user-visible effects)
- **Fixed** — bug fixes (`fix:`)
- **Technical** — dependency upgrades, build improvements, and other technical changes that may have indirect user impact (stability, security, performance)

Skip purely internal changes with no user impact at all: test additions, internal refactoring with no behavioral change, etc.

---

## Step 2: Update `CHANGELOG.md`

Add a new section at the top of the file, immediately after `# Changelog`.

### Heading format

Use the version from `gradle.properties` and the current date:

```markdown
## X.Y.Z - YYYY-MM-DD
```

Do not use an "Unreleased" placeholder — write the version heading from the start.

### Subsections

Use `### Added`, `### Changed`, `### Fixed` as needed. Omit subsections that have no entries.

### Entry wording rules

- **Be user-facing**: describe what the user sees or can do, not internal implementation details.
- **Mention UI location when relevant**: if a feature lives in a specific dialog, tab, or view, name it plainly (e.g., "in the index update dialog").
- **Mention user activation when relevant**: if a feature is user-triggered (dialog, button, option), make that clear (e.g., "on demand", "option to ...").
- **Start with a verb**: "add", "replace", "show", "prevent", etc.
- **Explain the benefit**: include why the change matters (e.g., "to let users add new files without waiting for the full scan").
- **Avoid internal jargon**: no class names, method names, or internal terminology.
- **Avoid vague descriptions**: "improve performance" or "fix bug" are too vague — be specific.
- **Avoid repetition**: don't repeat the same word (e.g., "performance tab showing performance").
- **Avoid backticks or code formatting** around feature names.
- Keep each entry to **one sentence**.

### Punctuation

- All list items end with a **comma** except the **last item** in each subsection, which ends with a **period**.
- Verify against the previous release section before submitting.

### Examples

```markdown
## 0.8.3 - 2026-11-16

### Added

- repository verification dialog to check integrity of archived files on demand,
- quick-add option in the index update dialog to let users add new files without waiting for the full scan,
- live performance tab to show where time is spent during repository operations.

### Changed

- operation progress tabs replaced with button-style design for visual consistency across the app,
- "committing" terminology replaced with "indexing" for clarity,
- "adding" terminology replaced with "index updating" for clarity, including renaming "Add new files" to "Update index", "Add and push" to "Update index and push", and "Quick add" to "Quick update".

### Fixed

- reliability and security issues identified by static analysis.
```

### Validation

After drafting, ask: "Would someone who doesn't know the codebase understand what changed, where to find it, and why it matters?"

---

## Step 3: Update `gradle.properties`

Two changes are needed in this step:

Edit the file at the project root:

```
version = X.Y.Z
versionCode = N
```

- **`version`** — remove the `-SNAPSHOT` suffix (e.g., `0.8.4-SNAPSHOT` → `0.8.4`). The version number itself is usually already correct.
- **`versionCode`** — set according to the `[minor][patch]` convention described in Step 0 (e.g., `0.8.4` → `84`, `0.9.0` → `90`)

After the release, bump to the next SNAPSHOT version for development:

```
version = X.Y.(Z+1)-SNAPSHOT
versionCode = N+1
```

---

## Step 4: Update Flatpak metainfo

File: `distribution/resources/org.archivekeep.ArchiveKeep.metainfo.xml`

Add a new `<release>` entry at the top of the `<releases>` section, before the existing releases. The content mirrors the CHANGELOG entry but in HTML format.

### Format

```xml
<release version="X.Y.Z" date="YYYY-MM-DD">
    <description>
        <p>Added:</p>
        <ul>
            <li>feature description,</li>
            <li>another feature.</li>
        </ul>
        <p>Changed:</p>
        <ul>
            <li>change description.</li>
        </ul>
        <p>Fixed:</p>
        <ul>
            <li>fix description.</li>
        </ul>
    </description>
</release>
```

### Notes

- Punctuation follows the same rules as CHANGELOG.md: commas for all but the last item, period for the last.
- Omit `<p>...</p>` blocks for subsections that have no entries.
- The Flatpak manifest (`distribution/flatpak/org.archivekeep.ArchiveKeep.yaml`) requires no changes — it builds from source and picks up the version from `gradle.properties` automatically.

---

## Commands reference

```bash
# List tags
git tag --list 'v*' --sort=-version:refname

# List commits since last tag
git log v0.8.2..HEAD --oneline

# Show diff of a specific commit's strings (for text renamings)
git show <commit> -- modules/app-ui/src/commonMain/resources/values/strings.xml
```
