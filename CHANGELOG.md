# Changelog

## Unreleased

### Changed:

- UI: make style more lighter by removing shadows, and adjust dimensions

## 0.4.0 - 2025-04-29 - Initial Android application, and UI improvements

### Added

- UI: add connection status tag for storages in Storages view
- UI: indicate disconnected status of repositories (reduce alpha, add text)
- UI: responsive app navigation layout supporting mobile screens:
  - top bar & drawer for small screens, 
  - top & bottom bar on narrow but tall screens,
  - rail bar for wide screens with low height,
  - top bar with embedded navigation for big screens.
- UI: navigation bar instead of navigation rail on small width screens (window sizes)
- setup: create Android application gradle module

### Changed

- UI: move repository add actions to dropdown in app bar (or rail bar)
- UI: improve look and feel of storages view
- UI: show only available and connected storages in home view
- UI: split storages based on type (local/external/online) in storages view
- UI: less framed look of the application (make content flow behind window insets)
- UI: remove welcome text from home page once setup for more efficient space use
- UI: more compact dimensions for small window/screen sizes
- UI: make dialogs utilize (more) screen space (dynamically based on items in many select) 
- refactor: extract UI to app-ui module
- build: limit collection of Gradle dependencies for Flatpak to sourceSets needed for its build
- build: merge generated Flatpak sources to one file
- build: split Flatpak sources generation from build in GH Actions

### Fixed

- repository shouldn't be added if init fails
- repository addition not always detects correct filesystem (on new mounts) and mixes up different filesystems

## 0.3.3 - 2025-03-31

### Added

- feature: support index update for non-primary (filesystem) repositories of archives
- UI: improve Upload/Download dialog: add progress summary, and error details in case of failure 
- UI: show execution indicator in dialogs
- UI: show execution error (if failed) in dialogs
- build: GH Actions cancel in-progress build in the same branch

### Change

- UI: unify and improve dialog control buttons for repository data operation dialogs
- metadata: expand and improve AppStream metainfo (by [@bragefuglseth])
- refactor: extract operation execution lifecycle related boilerplate from individual implementations

### Fixed

- UI: crash with large amounts of items (to add/commit/push)
- build: GH pages build cancels runs from different branches 

## [0.3.2] - 2025-03-25

### Added

- UI: add ability to forget repository stored in local storage
- UI: show if storage is local or external in storages view
- UI: show version info in navigation rail
- UI: support copy-paste of error stacktraces
- build: ARM64 Flatpak build support

## Changed

- UI: show details instead of short text for errors in filesystem repository add dialog
- build: Flatpak source lists are generated within CI/CD pipeline

### Removed

- UI: hide Archives and Settings views offering no features yet
- UI: remove icon for not-implemented dropdown for repositories in storages view

### Fixed

- the first user mount, in /run/media/<user>/<media>, after OS start not detected, when directory /run/media/<user> is just created
- storage related functionality not working if /run/media doesn't exist
- XdgFilePickerPortal based directory picker not working in ProGuard optimized build
- progress observation in Upload/Download dialogs sometimes (race-condition) crashes immediately after launch of copy

## [0.3.1] - 2025-03-22 - Flatpak build improvements

### Added

- distribution: [AppStream] MetaInfo

### Changed

- flatpak: add `--share=ipc` (for `--socket=x11`)
- flatpak: cleanup manifest

## [0.3.0] - 2025-03-21 - Improve GUI and add packaging support

This release is focused on:

- improving GUI for existing functionality,
- adding Flatpak packaging support.

### Added

- feature: relocations with duplication decrease support
- UI: open repository folder in system's file manager application
- UI: support relocations in Upload/Download dialogs (including duplication increase and/or decrease)
- UI: support custom selection of operations to execute in Upload/Download dialogs
- UI: Material Design TopAppBar (with client side decorations)
- UI: show preparation progress of index updating operations
- build: DEB and RPM packaging support
- build: Maven Central publishing support
- build: Flatpak offline Gradle build support

### Changed

- UI: add loading indication of secondary repository state
- UI: consolidate and refine dialog styles
- UI: move unlock button to TopAppBar
- UI: move information about function to separate view
- build: migrate to Gradle version catalog
- build: migrate GRPC to lite variant with okhttp
- build: upgrade to Kotlin 2.0.20 from 1.9.23
- docs: reworked home page
- refactor: renamed package common to files

### Fixed

- UI: reopen of Add & Push Dialog when operation is running
- build: release build processed by ProGuard - Gradle task proguardReleaseJars / createReleaseDistributable / runReleaseDistributable

### Removed

- build: GraalVM build & setup

## [0.2.0] - 2025-02-05 - Add Desktop GUI & rewrite to Kotlin

The initial Desktop GUI application was implemented using Jetpack Compose. Currently, the state of it is somewhere in between PoC and MVP.

Author decided to rewrite the project to Kotlin (and Jetpack Compose for GUI) when choosing technology for Desktop GUI (and eventually Android App).

**Important notice:** the license was changed to AGPL v3.

**Important remark:** technical and UI/UX debt was heavily created in this release, because perfection is enemy of done, and the goal was to _finish_ the first iteration of rewrite Kotlin and GUI.

### Decision details

The decision to drastically change technology to Kotlin (combined with Jetpack Compose for GUI) was based on:

- better multi-platform support:
  - supports many platforms including mobile Android, which enables
  - sharing common codebase between desktop application, server, command-line tools and planned Android application
- author's subjective preference of Kotlin language and its expressiveness.

The multi-platform support for Jetpack Compose is provided by [Compose Multiplatform][compose-multiplatform].

### Added 

- Desktop GUI,
- association of individual repositories to form a single logical archive, 
- ... not collected further ...

### Changed

- License to AGPL v3
- ... not documented further ...

### Removed - not ported yet

- server for self-hosting,
- verify sub-command,
- ... not documented further ...

## 0.1.3 and past

Undocumented

[AppStream]: https://www.freedesktop.org/software/appstream/docs/
[compose-multiplatform]: https://www.jetbrains.com/compose-multiplatform/
[@bragefuglseth]: https://github.com/bragefuglseth
