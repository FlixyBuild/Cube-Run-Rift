# Cube Run — Riftbreak

Cube Run is an Android endless runner built on libGDX. Run, jump, roll, collect, and cross a shuffled campaign of nine biomes while keeping the original Cube Run identity and its original-mode option.

## The run

- **You are the cube.** Six original starters remain free. Three new portrait skins repeat each supplied photo across all six faces of the cube, and the winged **Sky Soarer** is both the Air map's automatic visual skin and a selectable Wardrobe skin. Existing cube cosmetics remain available.
- **Jungle Canopy:** follow green-coin trails and finish the optional eight-green-coin harvest for bonus coins and score.
- **Neon City:** dodge traffic cars and bikes beneath neon-lit towers; collect blue coins to secure the city cache.
- **Frost Peaks:** race through falling snow, rolling snowballs, ice spikes and icicles.
- **Railway Yard:** read the signal posts and dodge trains on the rail-lined course.
- **Riftbound Space:** cross onto a translucent road among stars, planets and gravity hazards. Collect coins for the stardust objective.
- **Tropical Beach:** run past sandy shoulders, surf, palms, parasols and cabanas.
- **Moonlit Wilds:** evade animated wolves and tigers under warm roadside lamps.
- **Matrix Circuit:** read left/right route branches through firewall, drone and data-gate hazards.
- **Air:** race above the clouds through eagle dives, hawks, gulls and flock crossings. The Air route has its own sections, cloudbanks and winged cube.
- **Timed Black Hole escape:** reach the rift over 150 course units before the 14.5-second countdown ends. A safe arrival buffer protects the entry and exit; the moving exit gate stays synchronized with the escape distance. The singularity still captures a run that times out.
- **Bonus portals:** destinations unlock with best-score milestones. A shuffled destination bag visits every currently unlocked bonus world before it repeats; the boundary between cycles avoids an immediate repeat when there is more than one choice.
- **Coin families and powers:** red, blue, green, pink and mixed coins have distinct colours and values. Jetpack, skateboard, jump-shoes, phase, time-warp and bubble effects remain integrated with the existing controls and HUD.
- **Run difficulty:** choose Easy, Medium or Hard in Settings. The selection changes pace, obstacle spacing, auto-ramp and which Space sections can appear.

## Controls

Swipe left/right to change lanes, up to jump, and down to slide or ground-pound. Double-tap to activate a stocked bubble shield. Use the on-screen boost when available. From the home screen, tap the globe icon to choose a starting biome; other campaign biomes remain in the normal shuffled rotation. Open Wardrobe to browse and equip cube skins. The Original Cube Run option is unchanged.

## Audio and diagnostics

World Mix remains available alongside the separately selectable Brazilian Phonk/Funk option. If the app closes unexpectedly, reopen Cube Run and use **Settings → Crash diagnostics → Copy crash report** to share local diagnostics; nothing is sent automatically.

## Build and test

Use JDK 17 and Android SDK Platform 35:

```sh
bash gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Instrumented Android tests live under `app/src/androidTest`; running them requires an emulator or connected Android device.

## License and third-party notices

The project is licensed under the GNU General Public License v3.0 (`LICENSE`). Third-party runtime components and bundled font notices are summarized in [`docs/THIRD-PARTY-NOTICES.md`](docs/THIRD-PARTY-NOTICES.md). The launcher artwork was generated for this project and contains no third-party logo or character.
