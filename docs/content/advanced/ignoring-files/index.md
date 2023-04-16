---
title: 'Ignoring files'
---

Ignore functionality prevents files and directories from being indexed and added to an archive.

Ignoring of files and directories is achieved by specifying patterns in `.archivekeepignore` file in the root of archive repository.
The syntax of patterns is specified in [path.Match](https://pkg.go.dev/path#Match) function from Go standard library,
as this function is used for pattern matching of files names and directory names relative from archive root directory.

Lines starting with `#` are considered to be comments, and are ignored - not considered as patterns.

Example `.archivekeepignore` file contents:

```gitignore
# ignore all XMP files as they are part of exported JPEGs from darktable
*.xmp
*.XMP

# ignore darktable export directory containing draft/non-final attempts
darktable_exported
```
