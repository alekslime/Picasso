#!/usr/bin/env bash
# Builds the base and pro editions and copies them to build/apks/.
# Usage: ./scripts/build-apks.sh [Debug|Release]   (default: Debug)
set -euo pipefail
cd "$(dirname "$0")/.."

variant="${1:-Debug}"
lower="$(echo "$variant" | tr '[:upper:]' '[:lower:]')"
version="$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')"

./gradlew "assembleBase${variant}" "assemblePro${variant}"

out="build/apks"
mkdir -p "$out"
for edition in base pro; do
  src="$(find "app/build/outputs/apk/${edition}/${lower}" -maxdepth 1 -name '*.apk' | head -n1)"
  if [ -z "$src" ]; then echo "No ${edition} ${lower} APK found" >&2; exit 1; fi
  dest="${out}/Picasso-${edition}-${lower}-${version}.apk"
  cp "$src" "$dest"
  echo "-> ${dest}"
done
[ "$lower" = "release" ] && echo "Note: release APKs are unsigned unless PICASSO_KEYSTORE_* env vars are set."
