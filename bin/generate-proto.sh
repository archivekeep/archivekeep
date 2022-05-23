#!/usr/bin/env bash

cd "$(dirname "$(realpath "$0")")" || exit;
cd ..

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && cd ../ && pwd )"

SRC_DIR="${SCRIPT_DIR}"
DST_DIR="${SRC_DIR}/internal/grpc/protobuf"

mkdir -p "${DST_DIR}"

protoc \
  --proto_path="${SRC_DIR}" \
  --go_opt=paths=source_relative \
  --go_out="${DST_DIR}" \
  --go-grpc_opt=paths=source_relative \
  --go-grpc_out="${DST_DIR}" \
  "${SRC_DIR}/api.proto"
