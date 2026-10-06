#!/usr/bin/env bash
set -euo pipefail

repo_root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
production_dir="$repo_root/app/src/main/java"
build_file="$repo_root/app/build.gradle.kts"
media_manifest="$repo_root/app/src/main/assets/catalog/media-manifest.json"
body_manifest="$repo_root/app/src/main/assets/body/model-manifest.json"

if rg -n --glob '*.{kt,java}' -i 'https?://|cdn|retrofit|okhttp|java\.net|URL\(|download' "$production_dir"; then
    echo "offline contract failed: production code contains a remote loading/network marker" >&2
    exit 1
fi

if rg -n -i 'coil-network|retrofit|okhttp' "$build_file"; then
    echo "offline contract failed: app build declares a network media dependency" >&2
    exit 1
fi

if ! rg -n 'file:///android_asset/' "$production_dir" >/dev/null; then
    echo "offline contract failed: no local Android asset media access found" >&2
    exit 1
fi

check_local_paths() {
    local manifest="$1"
    local paths
    if [[ ! -f "$manifest" ]] || ! jq empty "$manifest" >/dev/null 2>&1; then
        echo "offline contract failed: manifest is missing or invalid: $manifest" >&2
        return 1
    fi
    paths="$(jq -r "$2" "$manifest")"
    while IFS= read -r path; do
        if [[ -z "$path" || "$path" == /* || "$path" == *\\* || "$path" =~ ^[A-Za-z][A-Za-z0-9+.-]*: || "$path" == ../* || "$path" == */../* || "$path" == */.. ]]; then
            echo "offline contract failed: non-local asset path: $path" >&2
            return 1
        fi
    done <<<"$paths"
}

check_local_paths "$media_manifest" '.entries[] | .thumbnailPath, .gifPath'
check_local_paths "$body_manifest" '.entries[] | .assetPath'

echo "offline contract passed: production media access is local-only"
