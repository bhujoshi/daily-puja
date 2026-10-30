#!/usr/bin/env bash
set -euo pipefail
cd /Users/bhuwanjoshi/work/worship/android
exec /opt/homebrew/opt/ruby/bin/bundle exec fastlane android "$@"
