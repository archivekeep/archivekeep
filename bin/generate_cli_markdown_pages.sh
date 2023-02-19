#!/usr/bin/env bash

SCRIPT_DIRECTORY="$(dirname "$0")"
PROJECT_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"

cd "${PROJECT_DIRECTORY}"

rm -rf docs/content/reference/cli
mkdir -p docs/content/reference/cli

go run ./main.go gen-markdown docs/content/reference/cli

echo -e '---\ntitle: CLI man\nweight: 40\nbookCollapseSection: true\n---' >> docs/content/reference/cli/_index.md
