#!/usr/bin/env bash
#
# compute-model-checksums.sh — Download each model in the catalog, compute its
# SHA256, and print values ready to paste into model_catalog.json.
#
# Why: the bundled model_catalog.json ships with empty "sha256" fields, so the
# app skips download integrity verification. Run this once (on a machine with
# network access and enough disk) to populate the hashes and enforce
# verification for every download.
#
# Usage:
#   bash scripts/compute-model-checksums.sh                 # download + hash all
#   bash scripts/compute-model-checksums.sh path/to/file    # hash a local file
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CATALOG="$PROJECT_ROOT/androidApp/src/main/assets/model_catalog.json"

sha_of() {
    if command -v sha256sum &>/dev/null; then sha256sum "$1" | awk '{print $1}';
    else shasum -a 256 "$1" | awk '{print $1}'; fi
}

if [ "$#" -ge 1 ]; then
    echo "$(sha_of "$1")  $1"
    exit 0
fi

# Parse id + url pairs from the catalog (needs jq).
if ! command -v jq &>/dev/null; then
    echo "ERROR: jq is required to read $CATALOG (or pass a local file path)." >&2
    exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

jq -r '.models[] | "\(.id)\t\(.url)"' "$CATALOG" | while IFS=$'\t' read -r id url; do
    echo "==> $id" >&2
    out="$TMP/$id.gguf"
    curl -fL --retry 3 -o "$out" "$url"
    echo "$id  sha256: $(sha_of "$out")"
done

echo ""
echo "Paste each sha256 into the matching model's \"sha256\" field in:"
echo "  $CATALOG"
