#!/usr/bin/env bash

cd "$(dirname "$(realpath "$0")")" || exit;
cd ..

CompileDaemon \
  -build 'go build ./cmd/archivekeep-server' \
  -pattern  "(.+\\.go|.+\\.c|.+\\.sql)$" \
  -command './archivekeep-server run' \
  -graceful-kill
