# RFP draft — submit at gitlab.com/fdroid/rfp/-/issues/new

Two ways to go (an MR is faster if you're comfortable with it):
- **RFP issue** (title below) — ask the F-Droid team to package it.
- **Merge request** to gitlab.com/fdroid/fdroiddata adding `metadata/cc.zachy.pulse.yml`
  (use fdroid/OFFICIAL-fdroiddata.yml from this repo). An MR usually moves faster.

---

**Title:** Pulse — Garmin watch companion (Gadgetbridge fork)

**Body:**

- **Name:** Pulse
- **Package ID:** cc.zachy.pulse
- **Source:** https://github.com/zoop-dev/pulse
- **License:** AGPL-3.0-or-later
- **Categories:** Sports & Health, Connectivity

**What it is**

Pulse is a Garmin-only fork of Gadgetbridge with a redesigned UI. It pairs with a
Garmin watch over Bluetooth and keeps everything on the phone — no account, no
vendor app, no cloud (apart from optional weather).

**Why a separate app rather than just Gadgetbridge**

It's a focused reskin: Garmin only, a redesigned Today/Sleep/Fitness/Health layout
(steps ring carousel, sleep score + trend, a daily intensity-minutes view Garmin
itself only shows weekly, shareable cards), and a single-file `.pulse` backup format.
It targets people who only have a Garmin and want a simpler, opinionated UI. It's
not meant to replace Gadgetbridge upstream, which stays the multi-device project.

**Building**

- Standard Gradle, flavor `mainline`, `subdir: app`.
- No proprietary dependencies; fonts are OFL (Plus Jakarta Sans, Space Grotesk,
  Unbounded). Released builds use R8.
- Tagged releases (e.g. `0.96.0`); recipe drafted at `fdroid/OFFICIAL-fdroiddata.yml`.
- Fastlane listing/metadata lives in `fastlane/metadata/android/en-US/`.

Happy to make any changes needed to fit packaging requirements.
