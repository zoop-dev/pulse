<div align="center">

# Pulse

**A Garmin-only wearable companion with a brand-new look.**

*by [zoop](https://zachy.cc) · `cc.zachy.pulse`*

Neon-blue, near-black, fully on-device. No accounts, no vendor cloud, no telemetry.

</div>

---

## What is Pulse?

Pulse is a focused fork of [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) that pairs with **Garmin** watches and reskins the whole experience around a single idea: your health data should look good and stay on your phone.

Everything Gadgetbridge does to talk to your Garmin over Bluetooth is still here — Pulse just rebuilds the entire surface on top of it: a new home, new tabs, custom typography, light/dark theming, accent colours, and a pile of features that aren't in upstream.

## Highlights

- **Today** — a big animated steps ring with a rounded nub, tinted hero tiles that fill with your progress, and a personalised greeting.
- **Goal celebration & streaks** — confetti and a notification when you hit a goal; tap the flame for a full streak calendar. Optional "any goal counts" streak mode.
- **Health tab** — a customizable grid of metric cards (heart rate, body energy, stress, SpO₂, HRV, respiration…) each with a chunky 7-day mini chart.
- **Sleep tab** — a sleep score, last-night stage hypnogram, nap detection, and a 7-night trend.
- **Fitness** — browse recorded workouts and in-depth, neon-styled charts.
- **Week in Review** — an animated weekly recap with an adaptive challenge and a Sunday-evening summary.
- **Achievements** — unlockable badges you can share as cards.
- **Home-screen widgets** — full + compact steps widgets with a refresh button, plus a Quick Settings tile.
- **Make it yours** — Light / Dark / System, and accent colours (Neon Blue, Violet, Coral, Mint, Pink).
- **Weather** — fetches local weather (Open-Meteo) and pushes it to your watch.

See [`app/src/main/res/xml/changelog_master.xml`](app/src/main/res/xml/changelog_master.xml) for the full, version-by-version history.

## Aesthetic

Near-black UI, neon-blue accents, [Unbounded](https://fonts.google.com/specimen/Unbounded) + [Satoshi](https://www.fontshare.com/fonts/satoshi) type. Built to feel like [zachy.cc](https://zachy.cc).

## Building

Uses the standard Gadgetbridge Gradle setup. Pulse ships as the `mainline` flavour:

```bash
./gradlew :app:assembleMainlineDebug
```

The APK lands in `app/build/outputs/apk/mainline/debug/`.

## Privacy

Like Gadgetbridge, Pulse keeps **everything on your device**. There are no accounts, no analytics, and no data ever leaves your phone except to talk to your watch (and, optionally, to fetch weather).

## Credits & licence

Pulse is built on the excellent work of the [**Gadgetbridge**](https://codeberg.org/Freeyourgadget/Gadgetbridge) project and the entire Garmin device stack it provides. Huge thanks to all of its contributors.

Pulse is licensed under the **GNU Affero General Public License v3.0** (AGPLv3), the same as Gadgetbridge. See [`LICENSE`](LICENSE). As a fork, the source stays open — if you distribute a build, you must make the corresponding source available under the same terms.

> Pulse is an independent, unofficial fork. It is not affiliated with or endorsed by Gadgetbridge, Garmin, or any wearable vendor.
</content>
