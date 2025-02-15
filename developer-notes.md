
Building and uploading to Maven Central:

```shell
rm -rf build/maven-publish-output build/maven-central-publish-package.zip

./gradlew --no-daemon --console=plain publisAllPublicationsToLocalOutputRepository

(cd build/maven-publish-output/ && zip -r ../maven-central-publish-package.zip .)
```

Updating dependencies for Flatpak packaging:

```shell
./gradlew --no-daemon --console=plain flatpakGradleGenerator

cp ./build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-root.json
cp ./app-core/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-core.json
cp ./app-desktop/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-desktop.json
cp ./cli/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-cli.json
cp ./files/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-files.json
```
