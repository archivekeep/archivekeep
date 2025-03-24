
Building and uploading to Maven Central:

```shell
rm -rf build/maven-publish-output build/maven-central-publish-package.zip

./gradlew --no-daemon --console=plain publisAllPublicationsToLocalOutputRepository

(cd build/maven-publish-output/ && zip -r ../maven-central-publish-package.zip .)
```
