#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

repo_root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
catalog_dir="$repo_root/app/src/main/assets/catalog"
source_json="$catalog_dir/exercises.json"
manifest_json="$catalog_dir/media-manifest.json"

expected_count="$(jq 'length' "$source_json")"
actual_count="$(jq '.entries | length' "$manifest_json")"
if [[ "$expected_count" != "$actual_count" ]]; then
    echo "media manifest count mismatch: source=$expected_count manifest=$actual_count" >&2
    exit 1
fi

failures=0
while IFS=$'\t' read -r id thumbnail_path gif_path thumbnail_bytes gif_bytes thumbnail_sha gif_sha; do
    thumbnail_file="$repo_root/app/src/main/assets/$thumbnail_path"
    gif_file="$repo_root/app/src/main/assets/$gif_path"
    if [[ ! -f "$thumbnail_file" ]]; then
        echo "missing thumbnail: $id $thumbnail_path" >&2
        failures=$((failures + 1))
        continue
    fi
    if [[ ! -f "$gif_file" ]]; then
        echo "missing gif: $id $gif_path" >&2
        failures=$((failures + 1))
        continue
    fi
    actual_thumbnail_bytes="$(wc -c < "$thumbnail_file" | tr -d ' ')"
    actual_gif_bytes="$(wc -c < "$gif_file" | tr -d ' ')"
    actual_thumbnail_sha="$(shasum -a 256 "$thumbnail_file" | awk '{print $1}')"
    actual_gif_sha="$(shasum -a 256 "$gif_file" | awk '{print $1}')"
    if [[ "$thumbnail_bytes" != "$actual_thumbnail_bytes" || "$thumbnail_sha" != "$actual_thumbnail_sha" ]]; then
        echo "thumbnail checksum mismatch: $id" >&2
        failures=$((failures + 1))
    fi
    if [[ "$gif_bytes" != "$actual_gif_bytes" || "$gif_sha" != "$actual_gif_sha" ]]; then
        echo "gif checksum mismatch: $id" >&2
        failures=$((failures + 1))
    fi
    if [[ "$thumbnail_path" == http* || "$gif_path" == http* ]]; then
        echo "remote media path: $id" >&2
        failures=$((failures + 1))
    fi
done < <(jq -r '.entries[] | [.id, .thumbnailPath, .gifPath, (.thumbnailBytes|tostring), (.gifBytes|tostring), .thumbnailSha256, .gifSha256] | @tsv' "$manifest_json")

if [[ "$failures" -ne 0 ]]; then
    echo "media validation failed: $failures issue(s)" >&2
    exit 1
fi

echo "media validation passed: $actual_count action mappings, local files and SHA-256 values verified"
