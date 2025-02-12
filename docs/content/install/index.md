---
title: 'Install'
weight: 15
---

## From package repositories

{{< hint info >}}
**Sorry!** This isn't yet packaged in any distribution.
{{< /hint >}}


## Building from sources

### Building Flatpak application

The application can be built by Gradle using `app-desktop:createDistributable` and then installed as an user application using Flatpak.

Install pre-requisites on Fedora (Toolbox):

```shell
# add adoptium repository
sudo dnf install adoptium-temurin-java-repository
sudo dnf config-manager --enable adoptium-temurin-java-repository

# install Temurin JDK
sudo dnf install binutils temurin-17-jdk
```

Setup flatpak:

```shell
flatpak remote-add --if-not-exists --user flathub https://dl.flathub.org/repo/flathub.flatpakrepo
```

```shell
./gradlew app-desktop:createReleaseDistributable

flatpak-builder \
  --force-clean \
  --user \
  --install-deps-from=flathub \
  --state-dir ~/opt/flatpak/flatpak-builder-state \
  --install ~/opt/flatpak/builddir/org.archivekeep.ArchiveKeep \
  distribution/flatpak/org.archivekeep.ArchiveKeep.yaml
```

### Building & installing DEB package

In Ubuntu (tested using Fedora Toolbox):

```shell
sudo apt update
sudo apt install openjdk-17-jdk fakeroot build-essential

# build non-optimized or release build
./gradlew --console=plain --no-daemon clean
./gradlew --console=plain --no-daemon app-desktop:packageDeb
./gradlew --console=plain --no-daemon app-desktop:packageReleaseDeb

# workaround for toolbox
sudo mkdir /usr/share/desktop-directories/

# install non-optimized or release build
sudo apt install ./app-desktop/build/compose/binaries/main/deb/archivekeep-desktop_*.deb 
sudo apt install ./app-desktop/build/compose/binaries/main-release/deb/archivekeep-desktop_*.deb
```

### Building & installing RPM package

In Fedora (tested using Fedora Toolbox):

```shell
sudo dnf install rpm-build

# build non-optimized or release build
./gradlew --console=plain --no-daemon clean
./gradlew --console=plain --no-daemon app-desktop:packageRpm
./gradlew --console=plain --no-daemon app-desktop:packageReleaseRpm

# install non-optimized or release build
sudo dnf install app-desktop/build/compose/binaries/main/rpm/archivekeep-desktop-*.rpm
sudo dnf install app-desktop/build/compose/binaries/main-release/rpm/archivekeep-desktop-*.rpm
```
