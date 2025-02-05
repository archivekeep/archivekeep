#!/usr/bin/env bash

set -e

SCRIPT_DIRECTORY="$(dirname "$0")"
PROJECT_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"

cd "${PROJECT_DIRECTORY}"

rm -rf docs/content/reference/cli
mkdir -p docs/content/reference/cli

./gradlew asciidoctor

echo -e '---\ntitle: CLI man\nweight: 40\nbookCollapseSection: true\n---' >> docs/content/reference/cli/_index.md

cp ./cli/build/generated-picocli-docs/* docs/content/reference/cli

sed -i '1s;^;:relfileprefix: ../\n:relfilesuffix: /\n;' docs/content/reference/cli/archivekeep.adoc
