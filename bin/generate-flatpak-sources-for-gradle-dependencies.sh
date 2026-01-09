#!/usr/bin/env bash

set -e

./gradlew --no-daemon --console=plain --info flatpakGradleGenerator

cp ./build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-root.json
cp ./modules/app-core/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-core.json
cp ./modules/app-desktop/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-desktop.json
cp ./modules/app-ui/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-app-ui.json
cp ./modules/cli/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-cli.json
cp ./modules/files/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-files.json
cp ./modules/files-driver-grpc/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-files-driver-grpc.json
cp ./modules/files-driver-s3/build/flatpak/dependencies-sources.json ./distribution/flatpak/gradle-sources-files-driver-s3.json

jq --slurp 'reduce .[] as $i ([]; . + ($i - .))' ./distribution/flatpak/gradle-sources-*.json > ./distribution/flatpak/gradle-sources.json
