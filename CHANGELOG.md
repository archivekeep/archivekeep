# Changelog

## [Unreleased]

### Added

- DEB and RPM packaging support
- Maven Central publishing support

### Changed

- migrate to Gradle version catalog
- upgrade to Kotlin 2.0.20 from 1.9.23
- renamed package common to files

### Fixed

- release build processed by ProGuard - Gradle task proguardReleaseJars / createReleaseDistributable / runReleaseDistributable

### Removed

- GraalVM build & setup

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
