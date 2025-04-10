#!/usr/bin/env bash

OUT="flatpak-sources-merged"

mkdir -p $OUT

process_list () {
  jq --slurp 'include "comm"; [([.[] | sort] | comm[0])[] + {"only_arches": ["x86_64"]}]' flatpak-sources-{x86_64,aarch64}/$1.json > $OUT/$1.x86_64.json
  jq --slurp 'include "comm"; [([.[] | sort] | comm[1])[] + {"only_arches": ["aarch64"]}]' flatpak-sources-{x86_64,aarch64}/$1.json > $OUT/$1.aarch64.json
  jq --slurp 'include "comm"; [.[] | sort] | comm[2]' flatpak-sources-{x86_64,aarch64}/$1.json > $OUT/$1.json
}

# thanks https://stackoverflow.com/a/65417432/409102
# shellcheck disable=SC2016
echo 'def comm: (.[0]-(.[0]-.[1])) as $d | [.[0]-$d,.[1]-$d, $d];' > comm.jq

process_list gradle-sources-app-core
process_list gradle-sources-app-desktop
process_list gradle-sources-app-ui
process_list gradle-sources-cli
process_list gradle-sources-files
process_list gradle-sources-root

process_list gradle-sources
