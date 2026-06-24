#!/usr/bin/env bash
# build a signed R8 release apk, drop it in the self-hosted f-droid repo, rebuild the
# index, and push it to cloudflare pages (fdroid.zachy.cc).
#
# one-time setup before this works:
#   - app signing key:   set PULSE_KEYSTORE / PULSE_KS_PASS / PULSE_KEY_ALIAS / PULSE_KEY_PASS
#   - repo signing key:  cd fdroid && fdroid init   (generates fdroid/keystore.jks, keep it secret)
#   - cloudflare:        a Pages project mapped to fdroid.zachy.cc
set -e
cd "$(dirname "$0")"

echo "building release (r8)…"
JAVA_HOME="${JAVA_HOME:-$HOME/dev/android-toolchain/jdk17}" \
    ./gradlew :app:assembleMainlineRelease --no-daemon -q

UNSIGNED=$(ls -t app/build/outputs/apk/mainline/release/*.apk | head -1)
OUT="fdroid/repo/cc.zachy.pulse_$(grep -m1 versionCode app/build.gradle | grep -oE '[0-9]+').apk"
mkdir -p fdroid/repo

echo "signing $UNSIGNED…"
"$ANDROID_HOME"/build-tools/*/apksigner sign \
    --ks "$PULSE_KEYSTORE" --ks-pass env:PULSE_KS_PASS \
    --ks-key-alias "$PULSE_KEY_ALIAS" --key-pass env:PULSE_KEY_PASS \
    --out "$OUT" "$UNSIGNED"

echo "rebuilding index…"
( cd fdroid && fdroid update -c --pretty )

echo "deploying to cloudflare pages…"
npx wrangler pages deploy fdroid/repo --project-name pulse-fdroid

echo "done. repo: https://fdroid.zachy.cc/repo"
