#!/usr/bin/env bash

CompileDaemon \
  -build 'go build ./cmd/archivekeep-server' \
  -pattern  "(.+\\.go|.+\\.c|.+\\.sql)$" \
  -command './archivekeep-server run' \
  -graceful-kill
