#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

repo_root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
exec python3 "$repo_root/tools/inspect-body-models.py" "$repo_root"
