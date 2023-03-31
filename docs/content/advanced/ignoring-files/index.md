---
title: 'Ignoring files'
---

Files can be ignored, and therefore prevented from being indexed and added to archive, by specifying patterns in `.archivekeepignore` file in the root of archive repository.

The syntax of patterns is specified in [path.Match](https://pkg.go.dev/path#Match) function from Go standard library,
as this function is used for pattern matching of files names.

Lines starting with `#` are considered to be comments, and are ignored - not considered as patterns.
