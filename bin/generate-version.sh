#!/usr/bin/env bash

cd "$(dirname "$(realpath "$0")")" || exit;
cd ..

TAG_COMMIT="$(git rev-list --abbrev-commit --tags --max-count=1)"
TAG="$(git describe --abbrev=0 --tags ${TAG_COMMIT} 2>/dev/null || true)"
COMMIT="$(git rev-parse --short HEAD)"
COMMIT_DATE="$(git log -1 --format=%cd --date=format:"%Y%m%d")"
CHANGES="$( git status --porcelain)"

cat << EOF > internal/buildinfo/archivekeep-version.json
{
  "Tag": "${TAG}",
  "TagCommit": "${TAG_COMMIT}",
  "Commit": "${COMMIT}",
  "CommitDate": "${COMMIT_DATE}",
  "Changes": "$(echo "${CHANGES}" | sed -z "s/\n/\\\n/g")"
}
EOF
