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
FINGERPRINT=80A165093CC44A46AD283FD696356F6032C581EABEC43EB6EBE4811796ED6F9F

[ -f "$HOME/keys/pulse-signing.env" ] && . "$HOME/keys/pulse-signing.env"

for j in "$JAVA_HOME" "$HOME/dev/android-toolchain/jdk21" "$HOME/dev/android-toolchain/jdk17" \
         "$HOME/.sdkman/candidates/java/current"; do
    [ -x "$j/bin/javac" ] && { export JAVA_HOME="$j"; break; }
done
for a in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "$HOME/dev/android-toolchain/sdk" "$HOME/Android/Sdk"; do
    [ -d "$a/build-tools" ] && { export ANDROID_HOME="$a"; break; }
done
export PATH="$JAVA_HOME/bin:$HOME/.local/bin:$PATH"

fail() { echo "publish-fdroid: $1" >&2; exit 1; }
[ -d "$ANDROID_HOME/build-tools" ] || fail "no Android SDK build-tools (set ANDROID_HOME). run this on the machine with the toolchain + signing keys."
[ -n "$PULSE_KEYSTORE" ] && [ -f "$PULSE_KEYSTORE" ] || fail "app signing key not found (\$HOME/keys/pulse-signing.env). run this on the machine that holds the release key."
command -v fdroid >/dev/null || fail "fdroidserver not installed (pip install fdroidserver)."

echo "building release (r8)…"
# skip vital lint — slow and not a useful gate for our own repo
./gradlew :app:assembleMainlineRelease --no-daemon -x lintVitalMainlineRelease

VCODE=$(grep -oE 'versionCode +[0-9]+' app/build.gradle | grep -oE '[0-9]+' | head -1)
VNAME=$(grep -oE 'versionName +"[^"]+"' app/build.gradle | grep -oE '"[^"]+"' | tr -d '"' | head -1)-pulse
UNSIGNED=$(ls -t app/build/outputs/apk/mainline/release/*.apk | head -1)
mkdir -p fdroid/repo/icons

# keep the "suggested version" metadata in lockstep with the actual build - a stale
# CurrentVersionCode here makes fdroidserver point clients at an older APK (it clamps
# suggestedVersionCode down to the highest one that actually exists in the repo).
sed -i \
    -e "s/^CurrentVersion:.*/CurrentVersion: ${VNAME}/" \
    -e "s/^CurrentVersionCode:.*/CurrentVersionCode: ${VCODE}/" \
    fdroid/metadata/cc.zachy.pulse.yml

echo "signing…"
APKSIGNER=$(ls "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
[ -x "$APKSIGNER" ] || fail "apksigner not found under $ANDROID_HOME/build-tools"
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
import sys, json, urllib.parse
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

# "Add to Obtainium" deep link for our main app - overrideSource "FDroidRepo" and
# additionalSettings.appIdOrName are what Obtainium actually reads on import (verified
# via a real export from the app, not the wiki docs, which don't document this field).
app_id = "cc.zachy.pulse"
app_name = pkgs.get(app_id, {}).get("metadata", {}).get("name", {}).get("en-US", "Pulse")
obtainium_cfg = {
    "id": app_id,
    "url": f"https://fdroid.zachy.cc/repo?appId={app_id}",
    "author": "zoop",
    "name": app_name,
    "overrideSource": "FDroidRepo",
    "additionalSettings": json.dumps({"appIdOrName": app_id}),
}
obtainium_link = urllib.parse.quote(json.dumps(obtainium_cfg), safe="")

html = open("fdroid/landing.html").read()
html = (html.replace("__FP__", fp)
            .replace("__APPCOUNT__", str(len(pkgs)))
            .replace("__APPS__", ", ".join(sorted(apps)))
            .replace("__OBTAINIUM__", obtainium_link))
open(f"{pub}/index.html", "w").write(html)
PY

echo "deploying to cloudflare pages…"
npx wrangler pages deploy "$PUB" --project-name pulse-fdroid --commit-dirty=true
rm -rf "$PUB"
echo "done. repo: https://fdroid.zachy.cc/repo"
