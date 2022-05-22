Features
========

This document provides high level overview of ArchiveKeep parts:

- CLI,
- encrypted archive,
- hosting server with WebUI.

## Command-line interface

The CLI supports following commands for local work:

- `init` a new archive for working plain archive and encrypted archive,
- `add` files to plain working archive,
    - check for missing files when adding a new one to detect moves,
- `rm` files from plain working archive,
    - should be able to remove existing files from index, and also files that were deleted but are still present in the index
- `verify` to verify integrity of archive contents.

Commands to sync contents and work with other archives are following:

- `compare` with other archive,
- `push` files to other archive,
- `pull` files from other archive.

### Sync operations

The sync operations for `push` to other archive, and `pull` from other archive, should support following operation modes:

- `--new` - copies new files, is default mode,
- `--resolve-moves` - moves existing files,
- `--enable-duplication-increase` - increases duplication of files,
- `--enable-duplication-reduction` - reductions duplication of files,
- ... and `--deletions` to be thought about.

Sync operations support working with following other archives:

- direct access to archives available on local filesystem,
- remote access via gRPC or HTTP(s).

### Security

The CLI implementation adheres to following security-related principles:

- all privacy and security enforcing secrets are stored in a wallet,
- network communication is encrypted using TLS (optional opt-out).

The implementation contains following builtin functionality:

- basic wallet based on JSON Web Encryption (JWE) and stored within archive contents.

## Encrypted Archive

The encrypted archive is a fallback approach to store data in a protected way.

It is intended to be used, when filesystem encryption is unavailable, or filesystem is shared with untrusted applications:

- phones with unavailable encryption of SD card to secure data at rest (phone theft),
- installed applications have unrestricted access to SD card contents.

Encrypted files have the following contents:

- signed public metadata - storing checksums and public-safe information,
- encrypted private metadata:
  - asymmetrical encryption to potentially support multiple consumers capable of decrypting contents for sharing purposes,
  - stores symmetric encryption key for contents and other sensitive data.
- symmetrically encrypted content with random-access support.

 ## Hosting server

The hosting server should provide the following functionality:

- ... to be added ...

### Web UI

The web UI should provide the following functionality:

- ... to be added ...

### Security

The server and network communication security is following:

- ... to be added ...

Thoughts:

- one-time burn-on-read tokens to establish new session (phone, local archive,...) shared via QR code or token string.
