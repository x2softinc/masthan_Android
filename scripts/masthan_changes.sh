#!/usr/bin/env bash
# Lists what changed in the iOS repo (x2softinc/Masthan) since the commit recorded in SYNC.md,
# so the changes can be ported to this Android app.
#
# This script only READS from the Masthan repo (shallow fetch into a temp dir); it never
# pushes to it.
#
# Usage: scripts/masthan_changes.sh [path-to-existing-Masthan-clone]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LAST="$(grep -E '^Last synced Masthan commit:' "$ROOT/SYNC.md" | sed -E 's/.*`([0-9a-f]{7,40})`.*/\1/')"
[ -n "$LAST" ] || { echo "Could not read the last synced commit from SYNC.md" >&2; exit 1; }

if [ $# -ge 1 ]; then
  CLONE="$1"
else
  CLONE="$(mktemp -d)"
  trap 'rm -rf "$CLONE"' EXIT
  git clone --quiet --filter=blob:none --no-checkout https://github.com/x2softinc/Masthan "$CLONE"
fi

git -C "$CLONE" fetch --quiet origin
HEAD_SHA="$(git -C "$CLONE" rev-parse origin/HEAD 2>/dev/null || git -C "$CLONE" rev-parse HEAD)"

echo "Last synced : $LAST"
echo "Masthan HEAD: $HEAD_SHA"
if [ "$(git -C "$CLONE" rev-parse "$LAST")" = "$HEAD_SHA" ]; then
  echo "Already in sync."
  exit 0
fi

echo
echo "== Commits =="
git -C "$CLONE" log --oneline "$LAST..$HEAD_SHA"
echo
echo "== Changed files (Swift sources, assets, config) =="
git -C "$CLONE" diff --stat "$LAST" "$HEAD_SHA" -- '*.swift' '*.mp3' '*.png' '*.xcassets' 'Masthan/Info.plist'
echo
echo "Port the changes using the file map in SYNC.md, then update the commit hash there."
