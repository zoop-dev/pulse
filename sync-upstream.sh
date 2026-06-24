#!/usr/bin/env bash
# check codeberg for new upstream gadgetbridge commits, and optionally replay pulse on top.
# usage: ./sync-upstream.sh           just report what's new (safe, default)
#        ./sync-upstream.sh --rebase  actually rebase pulse onto upstream/master
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

if [ "$1" != "--rebase" ]; then
    echo
    echo "(report only. run with --rebase to replay pulse onto upstream.)"
    exit 0
fi

echo
echo "rebasing pulse onto upstream/master… (on conflict: fix files, git add, git rebase --continue;"
echo " or bail with git rebase --abort)"
git rebase upstream/master
echo "rebased. build + deploy when happy:  ./deploy.sh \"sync upstream\""
