#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

repo_root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
manifest="$repo_root/app/src/main/assets/body/model-manifest.json"
assets_root="$repo_root/app/src/main/assets"

if [[ ! -f "$manifest" ]] || ! jq empty "$manifest" >/dev/null 2>&1; then
    jq -n '{valid:false, errors:["manifest file is missing or invalid"], modelCount:0, totalBytes:0}'
    exit 1
fi

tmp_report="$(mktemp)"
trap 'rm -f "$tmp_report"' EXIT
failures=0
total_bytes=0
model_count="$(jq '.entries | length' "$manifest")"
male_seen=0
female_seen=0

is_blocked_metadata() {
    local normalized
    normalized="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    [[ -z "$normalized" || "$normalized" == "missing" || "$normalized" == "blocked" || "$normalized" == "not supplied" || "$normalized" == "not confirmed" ]]
}

while IFS=$'\t' read -r gender path expected_bytes expected_sha license_status source_url source_commit license attribution apk_authorization; do
    if [[ "$gender" == "MALE" ]]; then
        [[ "$male_seen" -eq 1 ]] && { echo "duplicate gender: $gender" >>"$tmp_report"; failures=$((failures + 1)); }
        male_seen=1
    elif [[ "$gender" == "FEMALE" ]]; then
        [[ "$female_seen" -eq 1 ]] && { echo "duplicate gender: $gender" >>"$tmp_report"; failures=$((failures + 1)); }
        female_seen=1
    fi
    if [[ "$gender" != "MALE" && "$gender" != "FEMALE" ]]; then
        echo "invalid gender: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if [[ -z "$path" || "$path" == /* || "$path" == *\\* || "$path" =~ ^[A-Za-z][A-Za-z0-9+.-]*: || "$path" == ../* || "$path" == */../* || "$path" == */.. ]]; then
        echo "non-relative android_asset path: $path" >>"$tmp_report"; failures=$((failures + 1)); continue
    fi
    if [[ "$license_status" != "confirmed" ]]; then
        echo "licenseStatus is not confirmed: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if is_blocked_metadata "$source_url"; then
        echo "source URL or repository is missing or blocked: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if is_blocked_metadata "$source_commit"; then
        echo "source commit or version is missing or blocked: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if is_blocked_metadata "$license"; then
        echo "license is missing or blocked: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if is_blocked_metadata "$attribution"; then
        echo "attribution is missing or blocked: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if [[ "$apk_authorization" != "confirmed" ]]; then
        echo "APK redistribution authorization is not confirmed: $gender" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if [[ ! "$expected_bytes" =~ ^[0-9]+$ ]]; then
        if [[ "$expected_bytes" =~ ^- ]]; then
            echo "negative byte count: $path" >>"$tmp_report"
        else
            echo "invalid byte count: $path" >>"$tmp_report"
        fi
        failures=$((failures + 1))
    fi
    if [[ ! "$expected_sha" =~ ^[0-9A-Fa-f]{64}$ ]]; then
        echo "invalid SHA-256: $path" >>"$tmp_report"; failures=$((failures + 1))
    fi
    normalized_expected_sha="$(printf '%s' "$expected_sha" | tr '[:upper:]' '[:lower:]')"
    file="$assets_root/$path"
    if [[ ! -f "$file" ]]; then
        echo "asset does not exist: $path" >>"$tmp_report"; failures=$((failures + 1)); continue
    fi
    actual_bytes="$(wc -c <"$file" | tr -d ' ')"
    actual_sha="$(shasum -a 256 "$file" | awk '{print $1}')"
    total_bytes=$((total_bytes + actual_bytes))
    if [[ "$expected_bytes" =~ ^[0-9]+$ && "$expected_bytes" != "$actual_bytes" ]]; then
        echo "byte count mismatch: $path" >>"$tmp_report"; failures=$((failures + 1))
    fi
    if [[ "$normalized_expected_sha" != "$actual_sha" ]]; then
        echo "SHA-256 mismatch: $path" >>"$tmp_report"; failures=$((failures + 1))
    fi
done < <(jq -r '.entries[] | [.gender, .assetPath, (.bytes|tostring), .sha256, .licenseStatus, .sourceUrlOrRepository, .sourceCommitOrVersion, .license, .attribution, .apkRedistributionAuthorization] | @tsv' "$manifest")

if [[ "$male_seen" -eq 0 ]]; then echo "missing gender: MALE" >>"$tmp_report"; failures=$((failures + 1)); fi
if [[ "$female_seen" -eq 0 ]]; then echo "missing gender: FEMALE" >>"$tmp_report"; failures=$((failures + 1)); fi

if [[ "$failures" -eq 0 ]]; then
    jq -n --argjson count "$model_count" --argjson bytes "$total_bytes" \
        '{valid:true, errors:[], modelCount:$count, totalBytes:$bytes}'
else
    jq -n --argjson count "$model_count" --argjson bytes "$total_bytes" \
        --rawfile error_text "$tmp_report" \
        '{valid:false, errors:($error_text | split("\n") | map(select(length > 0))), modelCount:$count, totalBytes:$bytes}'
fi
exit "$failures"
