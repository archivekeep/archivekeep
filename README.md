# archivekeep


## Building native image (on NixOS)

Unfortunately, Gradle GraalVM plugin doesn't work well with non-FHS NixOS.

**Warning:** there's a bug in GraalVM ([#8792](https://github.com/oracle/graal/issues/8792)), that makes native image not work well with non-ASCII filenames and paths.

```shell
./gradlew clean nativeCompileClasspathJar

native-image \
  --libc=musl \
  --static \
  --no-fallback \
  -cp ./build/libs/nativecompile-classpath-1.0-SNAPSHOT.jar \
  -o archivekeep \
  org.archivekeep.cli.MainKt
```
