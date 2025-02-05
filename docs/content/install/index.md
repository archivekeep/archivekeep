---
title: 'Install'
weight: 15
---

## From packages

{{< hint info >}}
**Sorry!** This isn't yet packaged in any distribution.
{{< /hint >}}


## From sources - as a Flatpak application

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
./gradlew app-desktop:createDistributable

flatpak-builder \
  --force-clean \
  --user \
  --install-deps-from=flathub \
  --state-dir ~/opt/flatpak/flatpak-builder-state \
  --install ~/opt/flatpak/builddir/org.archivekeep.ArchiveKeep \
  distribution/flatpak/org.archivekeep.ArchiveKeep.yaml
```
