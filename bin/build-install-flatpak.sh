#!/usr/bin/env bash

flatpak-builder \
  --force-clean \
  --user \
  --install-deps-from=flathub \
  --state-dir ~/opt/flatpak/flatpak-builder-state \
  --repo ~/opt/flatpak/repo/org.archivekeep \
  --install ~/opt/flatpak/builddir/org.archivekeep.ArchiveKeep \
  distribution/flatpak/org.archivekeep.ArchiveKeep.yaml
