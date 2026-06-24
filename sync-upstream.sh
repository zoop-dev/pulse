#!/usr/bin/env bash
# pull in new upstream gadgetbridge commits from codeberg and replay pulse on top.
# usage: ./sync-upstream.sh          report + rebase
#        ./sync-upstream.sh --check  just report what's new, don't touch anything
set -e
cd "$(dirname "$0")"

git fetch upstream master
BASE=$(git merge-base HEAD upstream/master)
N=$(git rev-list --count "$BASE"..upstream/master)

if [ "$N" -eq 0 ]; then
    echo "already up to date with upstream."
    exit 0
fi

echo "== $N new upstream commits =="
git log --oneline "$BASE"..upstream/master
echo
echo "== of those, ones touching garmin / files we changed =="
git log --oneline "$BASE"..upstream/master -- \
    'app/src/main/java/nodomain/freeyourgadget/gadgetbridge/devices/garmin/**' \
    'app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/**' \
    $(git diff --name-only "$BASE"..HEAD | tr '\n' ' ') || true

[ "$1" = "--check" ] && exit 0

echo
echo "rebasing pulse onto upstream/master… (resolve conflicts, then: git rebase --continue)"
git rebase upstream/master
echo "rebased. now build + deploy:  ./deploy.sh \"sync upstream\""
