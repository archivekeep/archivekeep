# archivekeep


## Building native image (on NixOS)

Unfortunately, Gradle GraalVM plugin doesn't work well with non-FHS NixOS.

```shell
./gradlew clean nativeCompileClasspathJar

native-image \
  --no-server \
  --static \
  --no-fallback \
  -cp ./build/libs/nativecompile-classpath-1.0-SNAPSHOT.jar \
  -o archivekeep \
  org.archivekeep.cli.MainKt
```
