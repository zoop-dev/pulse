#!/usr/bin/env bash
# pulse deploy: refresh the upstream diff, commit, push to github.
# usage: ./deploy.sh "commit message"   (message defaults to "deploy")
#        SKIP_BUILD=1 ./deploy.sh "..."  to skip the build check
set -e
cd "$(dirname "$0")"

MSG="${1:-deploy}"
OUT=pulse-vs-gadgetbridge.diff
# wherever our fork currently branches off upstream (tracks syncs automatically)
BASE=$(git merge-base HEAD upstream/master 2>/dev/null || echo 79ad5a6)

if [ -z "$SKIP_BUILD" ]; then
    echo "building…"
    JAVA_HOME="${JAVA_HOME:-$HOME/dev/android-toolchain/jdk17}" \
        ./gradlew :app:assembleMainlineDebug --no-daemon -q
fi

# stage everything except the diff (it's regenerated below)
git add -A -- . ":(exclude)$OUT"
committed=0
if ! git diff --cached --quiet; then
    git commit -q -m "$MSG"
    committed=1
fi

# regenerate the full diff vs upstream (excluding the diff file itself)
{
    echo "### pulse vs upstream gadgetbridge (base $BASE)"
    echo "### generated $(date -u +%Y-%m-%dT%H:%MZ)"
    echo
    git diff "$BASE"..HEAD -- . ":(exclude)$OUT"
} > "$OUT"

git add "$OUT"
if [ "$committed" = 1 ]; then
    git commit -q --amend --no-edit          # fold the refreshed diff into this deploy
elif ! git diff --cached --quiet; then
    git commit -q -m "refresh diff"
fi

git push origin main
echo "deployed: $(git rev-parse --short HEAD)"
