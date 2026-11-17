# Prepare Dev for Next Release

After a release is published, bump `gradle.properties` to the next development version.

## Steps

1. In `gradle.properties`, change `version` to the next patch with `-SNAPSHOT` suffix.
   Example: `0.8.4` → `0.8.5-SNAPSHOT`

2. **Do not change `versionCode`** — keep it unchanged. It will be incremented during the actual release.

## Example

After releasing 0.8.4 (versionCode 84), prepare dev for 0.8.5:

```diff
- version = 0.8.4
+ version = 0.8.5-SNAPSHOT
  versionCode = 84
```

The `versionCode` stays at 84 — it will be incremented again when 0.8.5 is released.

3. Prepare a git commit message for the user and output it to chat. Use the format:

   > Prepare for the next version after X.Y.Z

   Where X.Y.Z is the latest released version (e.g., "Prepare for the next version after 0.8.4").
