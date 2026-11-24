#!/usr/bin/env bash
# Fully automatic README screenshots: launches the app and taps through it.
# Usage: ./scripts/screenshots.sh [--serial <id>] [--install]
exec python3 "$(dirname "$0")/screenshots.py" "$@"
