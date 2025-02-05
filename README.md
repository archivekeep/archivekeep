Archivekeep
===========

_Keep your files archived on multiple places, in a reliable and simple way._

The primary requirements and priorities of ArchiveKeep are to make process of **reliable archivation** to be simple and easy:

1. **safe replication** to other storages:
    - prevent propagation of redactions and deletion of files
    - prevent propagation of corruption and destructive changes of original information, 
2. **offline storages** - external HDDs - have first class support,
3. **plain files** for direct access, maximum compatibility and convenience,
4. **optional server** with asynchronous online synchronisation.

## Under development

The ArchiveKeep is under active development, although it already serves author's needs. Focus is to not introduce breaking changes, especially for formats of data at rest. However, that is not guaranteed.

However, as data files are being kept as plain files, if there would be a breaking change to support data structures, it would only render indexes and other technical data incompatible across versions, and would require reinitialisation and reindexing, and it should not cause data loss to plain data files.

The state of GUI is somewhere between PoC and MVP, and the code whole after being rewritten in Kotlin is in early development phase, and there could be bugs in edge-cases, and there definitely are missing features, and unsolved UX/UI ugliness (and correct GUI-friendly error handling) and other technical debts.

## About

Main features and functionality provided:

- **synchronization** to other places,
- **preview of differences** before synchronization,
- **integrity check** of individual data copies to detect tamper or corruption.

Threat model, in which you should still be able to retain your data:

- **lost access** to a device or online account,
- **corrupted drives** containing backups,
- **tampered or accidentally deleted data** - damage is not propagated and can be detected,
- **software ceases to exist** or becomes unavailable - archived copies are all regular files accompanied by hashes for integrity checks.

Assuming you don't lose every access to each copy of your data.

### Current and planned features

Supported storages:

- [X] plain simple files on HDD, SSD, USB drives or other filesystem media.
- [ ] self-hosting server - currently, it is implemented in previous version written in Go, not yet rewritten to Kotlin, but it's compatible with Kotlin client,
- [ ] Amazon S3 and object storage services providing S3 API.

Encryption:

- [X] no encryption,
  - note: filesystem or disk encryption transparent to applications can be used (i.e. [LUKS]), 
- [ ] per file E2E encryption (for storing files in cloud storage).

## Inspiration and similar software

This tool is inspired mainly by [Perkeep] and standard way of delivery of software archives and images via FTP and HTTP repositories having `.md5` or `.sha1` files containing checksum.

Notable software in similar software with less or more different goals:

- [Perkeep] - is a set of open source formats, protocols, and software for modeling, storing, searching, sharing and synchronizing data in the post-PC era. Data may be files or objects, tweets or 5TB videos, and you can access it via a phone, browser or FUSE filesystem. 
  - difference is in data storage architecture, where Perkeep has custom blob format for files, but ArchiveKeep keeps files as plain files:
    - direct access to files without any software or server running,
    - no lock-in to specific software to access files,
    - i.e. just pass external HDD to other people, or plug it into a smart TV,... 
- [Syncthing] - continuous file synchronization program. It synchronizes files between two or more computers in real time, safely protected from prying eyes.
  - difference is in focus, Syncthing focuses on seamless and continuous file synchronisation, where ArchiveKeeps focuses on archivation and preservation of original data untouched and uncorrupted,
- [borgbackup], [restic] and other backup software - designed to get back in time, which either includes corruption that was already present at the point when backup was done, or requires to preserve the full history and the figure out which backup contains uncorrupted data.

## Installation

Install from packages:

- not packaged for any distribution, yet.

Build sources with Gradle and install as Flatpak application:

- see [docs - install](docs/content/install/index.md) for instructions.

## Usage

### GUI

The ArchiveKeep should appear in system application menu after installation.

Alternatively, it's possible to launch Flatpak application manually from terminal:

```shell
flatpak run org.archivekeep.ArchiveKeep
```

### CLI

See [docs/features.md](docs/content/about/features/index.md) for overview. To be documented later.

## Warranty

Although author desires to make system 100% bulletproof to prevent loss of own data. This is open-source software accessible for free, and comes with no warranty.

## License

The whole project is licensed under AGPLv3 - GNU Affero General Public License, version 3. See [LICENSE](LICENSE) for details.

[Perkeep]: https://perkeep.org/
[Syncthing]: https://syncthing.net/
[BorgBackup]: https://www.borgbackup.org/
[restic]: https://restic.net/
[LUKS]: https://en.wikipedia.org/wiki/Linux_Unified_Key_Setup
