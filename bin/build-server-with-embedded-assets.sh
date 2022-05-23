#!/usr/bin/env bash

cd "$(dirname "$(realpath "$0")")" || exit;
cd ..

go build -tags embed_assets ./cmd/archivekeep-server
