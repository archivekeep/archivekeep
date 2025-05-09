#!/usr/bin/env bash

set -e

SCRIPT_DIRECTORY="$(dirname "$0")"
PROJECT_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"

cd "${PROJECT_DIRECTORY}"

rm -rf docs/content/reference/cli
mkdir -p docs/content/reference/cli

rm -rf docs/static/generated_screenshots
mkdir -p docs/static/generated_screenshots

./gradlew asciidoctor
./gradlew app-ui:desktopTest # TODO: should run only screenshots rendering

echo -e '---\ntitle: CLI man\nweight: 40\nbookCollapseSection: true\n---' >> docs/content/reference/cli/_index.md

cp ./modules/cli/build/generated-picocli-docs/* docs/content/reference/cli

cp -R ./modules/app-ui/build/generated-ui-screenshots/* docs/static/generated_screenshots/

sed -i '1s;^;:relfileprefix: ../\n:relfilesuffix: /\n;' docs/content/reference/cli/archivekeep.adoc
