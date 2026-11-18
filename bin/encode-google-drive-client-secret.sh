#!/usr/bin/env bash
set -euo pipefail

KEY="8a5a6db6f987e7130912fecfaca02c2e3b4a4b0b620376200ccd2bffa7c7748f"

if ! command -v openssl >/dev/null || ! command -v xxd >/dev/null; then
  echo "This script requires openssl and xxd." >&2
  exit 1
fi

read -r -s -p "New Google Drive client secret: " client_secret
printf '\n' >&2
if [[ -z "$client_secret" ]]; then
  echo "The client secret cannot be empty." >&2
  exit 1
fi

iv="$(openssl rand -hex 16)"
{
  printf '%s' "$iv" | xxd -r -p
  printf '%s' "$client_secret" | openssl enc -aes-256-cbc -K "$KEY" -iv "$iv" -nosalt
} | base64 -w0
printf '\n'
unset client_secret