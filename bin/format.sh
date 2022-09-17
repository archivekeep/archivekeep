#!/usr/bin/env bash

set -e

goimports -local github.com/archivekeep/archivekeep -w .
