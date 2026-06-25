#!/usr/bin/env bash
# build a signed R8 release apk, drop it in the self-hosted f-droid repo, rebuild the
# index, and push it to cloudflare pages (fdroid.zachy.cc).
#
# one-time setup before this works:
#   - app signing key:   ~/keys/pulse-signing.env (PULSE_KEYSTORE / *_PASS / *_ALIAS)
#   - repo signing key:  cd fdroid && fdroid init   (creates keystore.p12, kept out of git)
#   - cloudflare:        a Pages project "pulse-fdroid" mapped to fdroid.zachy.cc
set -e
cd "$(dirname "$0")"

# repo signing-key SHA-256 fingerprint, for the one-tap "Add to F-Droid" link
FINGERPRINT=64E3BF277CC48A270B543B5D8B4087CB3CBA19D350947464776079A52E56C4A3

[ -f "$HOME/keys/pulse-signing.env" ] && . "$HOME/keys/pulse-signing.env"
export JAVA_HOME="${JAVA_HOME:-$HOME/dev/android-toolchain/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/dev/android-toolchain/sdk}"
export PATH="$JAVA_HOME/bin:$HOME/.local/bin:$PATH"

echo "building release (r8)…"
# skip vital lint — slow and not a useful gate for our own repo
./gradlew :app:assembleMainlineRelease --no-daemon -x lintVitalMainlineRelease

VCODE=$(grep -oE 'versionCode +[0-9]+' app/build.gradle | grep -oE '[0-9]+' | head -1)
UNSIGNED=$(ls -t app/build/outputs/apk/mainline/release/*.apk | head -1)
mkdir -p fdroid/repo/icons

echo "signing…"
APKSIGNER=$(ls "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)
"$APKSIGNER" sign --ks "$PULSE_KEYSTORE" --ks-pass env:PULSE_KS_PASS \
    --ks-key-alias "$PULSE_KEY_ALIAS" --key-pass env:PULSE_KEY_PASS \
    --out "fdroid/repo/cc.zachy.pulse_${VCODE}.apk" "$UNSIGNED"

# icon source for both the repo (fdroid/icon.png) and the app listing (fastlane images/)
cp fdroid/pulse-icon.png fdroid/icon.png
cp fdroid/pulse-icon.png fdroid/metadata/cc.zachy.pulse/en-US/images/icon.png

echo "rebuilding index…"
( cd fdroid && fdroid update -c )

echo "staging (repo under /repo + landing page)…"
PUB=$(mktemp -d)
mkdir -p "$PUB/repo"
cp -r fdroid/repo/. "$PUB/repo/"
python3 - "$FINGERPRINT" "$PUB" <<'PY'
import sys, json
fp, pub = sys.argv[1], sys.argv[2]
try:
    import qrcode
    qrcode.make(f"fdroidrepos://fdroid.zachy.cc/repo?fingerprint={fp}").save(f"{pub}/qr.png")
except Exception:
    pass
d = json.load(open("fdroid/repo/index-v2.json"))
pkgs = d["packages"]
apps = []
for p in pkgs.values():
    name = p["metadata"]["name"]["en-US"]
    ver = max(p["versions"].values(), key=lambda v: v["manifest"]["versionCode"])["manifest"]["versionName"]
    apps.append(f"{name} {ver}")
html = open("fdroid/landing.html").read()
html = (html.replace("__FP__", fp)
            .replace("__APPCOUNT__", str(len(pkgs)))
            .replace("__APPS__", ", ".join(sorted(apps))))
open(f"{pub}/index.html", "w").write(html)
PY

echo "deploying to cloudflare pages…"
npx wrangler pages deploy "$PUB" --project-name pulse-fdroid --commit-dirty=true
rm -rf "$PUB"
echo "done. repo: https://fdroid.zachy.cc/repo"
