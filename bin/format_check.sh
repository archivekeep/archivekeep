#!/usr/bin/env bash

set -e

goimports -local github.com/archivekeep/archivekeep -e -d . 2>&1 | tee /dev/stderr | (! read)
