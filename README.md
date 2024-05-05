# archivekeep


## Building native image (on NixOS)

Unfortunately, Gradle GraalVM plugin doesn't work well with non-FHS NixOS.

**Warning:** there's a bug in GraalVM ([#8792](https://github.com/oracle/graal/issues/8792)), that makes native image not work well with non-ASCII filenames and paths.

Build native-image binary:

```shell
./gradlew --console=plain --no-daemon clean cli:nativeCompile
```

Run native-image binary:

```shell
time ./cli/build/native/nativeCompile/archivekeep --help
```
