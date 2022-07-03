#!/usr/bin/env bash

cd "$(dirname "$(realpath "$0")")" || exit;
cd ..

CompileDaemon \
  -build 'go build -tags embed_assets ./cmd/archivekeep-server' \
  -pattern  "(.+\\.go|.+\\.c|.+\\.sql|.+\\.js)$" \
  -command './archivekeep-server run --grpc-listen-on-all-interfaces --http-listen-on-all-interfaces' \
  -graceful-kill
