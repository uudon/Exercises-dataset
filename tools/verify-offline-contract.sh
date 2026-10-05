#!/usr/bin/env bash
set -euo pipefail

repo_root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
production_dir="$repo_root/app/src/main/java"
build_file="$repo_root/app/build.gradle.kts"

if rg -n --glob '*.kt' 'https?://|cdn|Retrofit|OkHttp|URL\(' "$production_dir"; then
    echo "offline contract failed: production code contains a remote loading/network marker" >&2
    exit 1
fi

if rg -n 'coil-network|retrofit|okhttp' "$build_file"; then
    echo "offline contract failed: app build declares a network media dependency" >&2
    exit 1
fi

if ! rg -n 'file:///android_asset/' "$production_dir" >/dev/null; then
    echo "offline contract failed: no local Android asset media access found" >&2
    exit 1
fi

echo "offline contract passed: production media access is local-only"
