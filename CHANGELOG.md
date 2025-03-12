# Changelog

## [Unreleased]

### Added

- feature: relocations with duplication decrease support
- UI: open repository folder in system's file manager application
- UI: support relocations in Upload/Download dialogs (including duplication increase and/or decrease)
- UI: support custom selection of operations to execute in Upload/Download dialogs
- UI: Material Design TopAppBar (with client side decorations)
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

[compose-multiplatform]: https://www.jetbrains.com/compose-multiplatform/
