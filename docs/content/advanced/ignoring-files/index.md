---
title: 'Ignoring files'
---

Ignore functionality prevents files and directories from being indexed and added to an archive.

Ignoring of files and directories is achieved by specifying patterns in `.archivekeepignore` file in the root of archive repository.
The syntax for pattern matching is [glob](https://en.wikipedia.org/wiki/Glob_(programming)),
and matching is done on files names and directory names relative from archive root directory.

Lines starting with `#` are considered to be comments, and are ignored - not considered as patterns.

Example `.archivekeepignore` file contents:

```gitignore
# ignore all XMP files as they are part of exported JPEGs from darktable
*.xmp
*.XMP

# ignore darktable export directory containing draft/non-final attempts
darktable_exported
```
