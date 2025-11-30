#!/usr/bin/env bash

mkdir -p ~/opt/flatpak/repo/org.archivekeep

flatpak-builder \
  --force-clean \
  --user \
  --install-deps-from=flathub \
  --state-dir ~/opt/flatpak/flatpak-builder-state \
  --repo ~/opt/flatpak/repo/org.archivekeep \
  --install ~/opt/flatpak/builddir/org.archivekeep.ArchiveKeep \
  --disable-rofiles-fuse \
  distribution/flatpak/org.archivekeep.ArchiveKeep.yaml
