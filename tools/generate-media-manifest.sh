#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

repo_root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
catalog_dir="$repo_root/app/src/main/assets/catalog"
source_json="$catalog_dir/exercises.json"
images_dir="$catalog_dir/images"
videos_dir="$catalog_dir/videos"
manifest_path="$catalog_dir/media-manifest.json"
report_path="$catalog_dir/media-check-report.json"
source_commit="7455efae41b330c265e7cd4b78dfa848e7ce5ebd"
apk_path="${2:-}"

temp_entries="$(mktemp)"
trap 'rm -f "$temp_entries"' EXIT

missing_images=0
missing_gifs=0
while IFS=$'\t' read -r id image gif_path; do
    image_file="$catalog_dir/$image"
    gif_file="$catalog_dir/$gif_path"
    if [[ ! -f "$image_file" ]]; then
        missing_images=$((missing_images + 1))
        continue
    fi
    if [[ ! -f "$gif_file" ]]; then
        missing_gifs=$((missing_gifs + 1))
        continue
    fi
    image_bytes="$(wc -c < "$image_file" | tr -d ' ')"
    gif_bytes="$(wc -c < "$gif_file" | tr -d ' ')"
    image_sha="$(shasum -a 256 "$image_file" | awk '{print $1}')"
    gif_sha="$(shasum -a 256 "$gif_file" | awk '{print $1}')"
    jq -n \
        --arg id "$id" \
        --arg thumbnailPath "catalog/$image" \
        --arg gifPath "catalog/$gif_path" \
        --arg thumbnailSha256 "$image_sha" \
        --arg gifSha256 "$gif_sha" \
        --argjson thumbnailBytes "$image_bytes" \
        --argjson gifBytes "$gif_bytes" \
        '{id: $id, thumbnailPath: $thumbnailPath, gifPath: $gifPath, thumbnailBytes: $thumbnailBytes, gifBytes: $gifBytes, thumbnailSha256: $thumbnailSha256, gifSha256: $gifSha256}' >> "$temp_entries"
done < <(jq -r '.[] | [.id, .image, .gif_url] | @tsv' "$source_json")

jq -s '{entries: .}' "$temp_entries" > "$manifest_path"

image_count="$(find "$images_dir" -type f -name '*.jpg' | wc -l | tr -d ' ')"
gif_count="$(find "$videos_dir" -type f -name '*.gif' | wc -l | tr -d ' ')"
image_bytes="$(find "$images_dir" -type f -name '*.jpg' -exec wc -c {} + | awk 'END {print $1 + 0}')"
gif_bytes="$(find "$videos_dir" -type f -name '*.gif' -exec wc -c {} + | awk 'END {print $1 + 0}')"
source_count="$(jq 'length' "$source_json")"
if [[ -n "$apk_path" && -f "$apk_path" ]]; then
    apk_size="$(wc -c < "$apk_path" | tr -d ' ')"
    apk_status="measured-debug-apk"
else
    apk_size="null"
    apk_status="not-measured"
fi

jq -n \
    --arg sourceCommit "$source_commit" \
    --arg generatedAt "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" \
    --argjson sourceCount "$source_count" \
    --argjson imageCount "$image_count" \
    --argjson gifCount "$gif_count" \
    --argjson imageBytes "$image_bytes" \
    --argjson gifBytes "$gif_bytes" \
    --argjson missingImages "$missing_images" \
    --argjson missingGifs "$missing_gifs" \
    --argjson apkSize "$apk_size" \
    --arg apkStatus "$apk_status" \
    '{sourceCommit: $sourceCommit, generatedAt: $generatedAt,
      sourceExerciseCount: $sourceCount,
      media: {imageCount: $imageCount, gifCount: $gifCount, imageBytes: $imageBytes, gifBytes: $gifBytes},
      mapping: {missingImages: $missingImages, missingGifs: $missingGifs,
        status: (if ($missingImages == 0 and $missingGifs == 0 and $imageCount == $sourceCount and $gifCount == $sourceCount) then "executed-no-missing-files" else "blocked-missing-files" end)},
      apk: {sizeBytes: $apkSize, status: $apkStatus},
      runtime: {thumbnailLoadMs: null, gifLoadMs: null, memoryBytes: null, status: "not-measured"},
      authorization: {localCopy: "recorded-from-upstream-notice", apkRedistribution: "unconfirmed"}}' > "$report_path"

echo "Generated $manifest_path"
echo "Generated $report_path"
