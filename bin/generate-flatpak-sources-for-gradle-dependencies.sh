#!/usr/bin/env bash

./gradlew --no-daemon --console=plain --info flatpakGradleGenerator

cp ./build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-root.json
cp ./modules/app-core/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-core.json
cp ./modules/app-desktop/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-desktop.json
cp ./modules/cli/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-cli.json
cp ./modules/files/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-files.json
