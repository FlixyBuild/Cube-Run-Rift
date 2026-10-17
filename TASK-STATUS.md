Task: Fix issue 27 and adjacent layout and split-screen issues.

Follow-up finding: Entering split screen from Recents after Cube Run had launched
left a second Android splash view covering the live game indefinitely.

Status: Fixed the repeated splash exit handling. Verified the actual Recents →
Split screen → Settings flow on an emulator and an attached phone. The game
remains visible and responds to input in the split pane. Build, lint, and 18
targeted on-device tests passed.

Follow-up: Content-first compact layouts below 480 dp usable height now remove
shop/result showroom space, keep wardrobe actions pinned beside scrollable
details, condense achievements and the main menu, constrain pause/language
sheets, and reduce the gameplay HUD. Native controls survive live resizing;
full-height presentations are restored when space returns. Build/lint and
compact-window, purchase, reward and language regressions are verified on the
attached Moto G7 Power. See docs/compact-layout.md for the verification matrix.

## Follow-up: difficulty and obstacle variety (2026-10-14)

Easy, Medium and Hard are persistent Settings choices that affect pace, auto-ramp, spacing and Space section unlocks. The renderer now gives Jungle and Space different readable obstacle silhouettes while keeping the authored collision boxes. Space bonus portals reached from Jungle also use Space-specific art. See `docs/prompt-1-audit.md` for implementation and verification notes.

Verification for this follow-up: `assembleDebug`, `testDebugUnitTest` (17 passed), `assembleDebugAndroidTest` (compiled; not run) and `lintDebug` passed in separate low-memory Gradle invocations; bot self-tests passed. No device/emulator was attached (`adb devices -l` was empty), so runtime visual/input verification remains outstanding.

## Follow-up: biome, Space, and performance polish (2026-10-15)

Expanded the campaign to Jungle, Neon City, Winter Peaks, and Space. City now has traffic-car/bike obstacles, neon road markings, and an eight-blue-coin cache objective; Winter adds snow-covered roadside scenery, snowfall, firs, ice hazards, and a 24-row survival objective. Space has a ten-coin stardust objective, corrected tesseract edge alignment, anchored course obstacles during black-hole attraction, and reusable backdrop buffers. Added timed jump shoes with a 1.25× takeoff boost alongside the existing jetpack and skateboard, plus pickup-balance coverage.

Verification: `:app:testDebugUnitTest` passed (24 tests); `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest` passed, with the instrumentation APK compiled but not run. `git diff --check` is clean. No physical-device visuals, controls, FPS, or lag measurements are claimed. Buffer reuse is an allocation reduction, not a measured all-device performance guarantee.

## Follow-up: Tropical Beach, cosmetics, audio and portal diagnostics (2026-10-15)

Added Tropical Beach as the fifth campaign biome: warm road and sandy shoulders, surf/blue water, block-built palms, parasols and cabanas, plus a timed plane fly-by. Winter flakes now descend in staggered, deterministic cycles. Added four paid cosmetic cube designs (Deadpool, Spider-Man, Iron Sentinel and Frost Byte) while preserving the six free starters and keeping IDs clear of legacy save/ability IDs. Coin base payouts are up 10% without changing spawn frequency. Procedural effects/music use lower-register tones, a stronger bass bed, and a distinct beach cue. Added Space-exit state handling that remains active after Track clears its bonus ID, and fail-closed handling for invalid portal IDs. Crash diagnostics persist uncaught Java stack traces and record low-rate debug/memory-pressure breadcrumbs.

Prior verification: `:app:testDebugUnitTest` passed (35 tests); `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and `:app:lintDebug` passed. The Android-test APK compiled but instrumentation was not run. `git diff --check` was clean. No connected device or emulator was present (`adb devices -l` was empty). The Space-portal regression is unit-tested, but no crash report or device reproduction was available to confirm that this is the reported City-portal crash or the cause of spontaneous process closures; visual/audio/performance runtime checks remained outstanding.

## Corrective follow-up: user-reported City closure and missing content (2026-10-15)

The user reported that the delivered APK still closes on City entry and that Beach, skins, and powers are missing or inaccessible. This remains an open on-device acceptance item: the supplied screenshot only shows a live City frame at score 99, so it does not identify a crash cause.

Build 2.8 changes:

- The splash/loading shot now draws all ten existing map themes behind the existing animated foreground cube (the five active campaign worlds plus the preserved original themes).
- Tropical Beach is first and marked NEW in the starting-world picker; the selection persists and other campaign worlds remain in rotation.
- New Deadpool, Spider-Man, Iron Sentinel, and Frost Byte cosmetics are retained, with prices lowered to 100–450 coins. The six existing free starters and all original map/settings/features are retained.
- The first jetpack is guaranteed on its first regular pickup slot; later jetpacks and mystery boxes retain their score pacing.
- Android 11+ process-exit history is persisted (including low-memory/native-crash reasons where available). Settings has a local Copy Crash Report action; nothing is sent automatically.

Verification: build 2.8 `assembleDebug`, `assembleDebugAndroidTest` (instrumentation APK compiled, not run), `testDebugUnitTest` (36 passed), `lintDebug`, and `git diff --check` pass. The APK identifies as package `cube.run`, version 2.8/code 18, and its debug signature verifies. `adb devices -l` is empty and no `/dev/kvm` is available, so no device/emulator gameplay or City-crash test was possible. Do not claim the City closure is fixed or the game is lag-free/polished until the exact latest APK is retested on the user's device; if it still closes, copy the report from **Settings → Crash diagnostics → Copy crash report** and share it.

## Corrective follow-up: crash, sound and cosmetic 2.9 (2026-10-15)

The user's Android 10 report identifies `ConcurrentModificationException` in
`CubeRun.collide()` while iterating `Track.rows`. Entering a Space portal can
start the Black Hole challenge, whose reset/start removes and appends exit rows
on that same list. The challenge reset/start is now deferred until the collision
iterator finishes. A unit regression test uses a real `Track` and exercises
removal plus insertion during a row pass. No per-frame row-list copy was added.

Added the original optional Phonk / Funk instrumental in Settings → Sound;
World mix remains the default and existing world cues remain intact. Added the
purchasable cosmetic Bassline cube skin for 320 coins. Existing maps, options,
power-ups and effects remain in place.

Verification: `:app:testDebugUnitTest` passed (40 tests, 0 failures);
`:app:assembleDebug`, `:app:assembleDebugAndroidTest` (instrumentation APK
compiled, not run), and `:app:lintDebug` passed. APK package/version: `cube.run`,
2.9/code 19; debug signature verifies. No phone or usable emulator was
available, so no Infinix crash reproduction, sound audition, or lag/FPS/frame-time
measurement is claimed. The known collection mutation is covered by the unit
regression test; the user's device still needs a retest. The existing Low
Graphics setting targets 30 FPS and reduces render/decoration work, but does not
prove that lag is resolved on the Infinix.

The previous local debug keystore was not retained during cleanup. The 2.9 debug
APK therefore has a different signature from the delivered 2.8 APK; Android may
reject an in-place update. Uninstalling v2.8 first can erase local save data, so
back up anything important before installing.

## Release follow-up: Railway, Space hazards, powers, portals and UI polish (2026-10-15)

Prepared version 2.10/code 20. Added Phase and Time Warp pickups, including pickup art, HUD timers and gameplay effects; Time Warp is suspended during the timed singularity escape so it cannot make that objective harder. Added Comet Core (skin ID 16, 240 coins) and Signal Shift (ID 17, 280 coins), retaining legacy IDs. Railway Yard is in the six-world campaign rotation with train obstacle art and non-colliding signal-post workers. Riftbound Space is brighter, its Low Graphics effects are reduced, and three minefield section patterns use a new gravity-mine obstacle while leaving one lane safe. The old bead-ring portal art is replaced with biome-framed arch/prism gates, differentiated ornaments and map colors; a unit regression verifies every Bonus world queues its automatic return portal. Invalid portal destinations fail closed. Collision-pass deferrals now queue all actions FIFO until the Track.rows iterator is closed.

The launch mural has a new CUBE RUN title, ten-map card layout, campaign highlighting, road and progress bar; adaptive launcher artwork is enlarged. Map selection cards and the Map, Settings and Skins headers have small offline icons. The “Original Cube Run” option was not changed. Phonk/Funk remains in v2.9 Settings → Sound; the user's screenshot was from v2.8.

Verification on this source: `:app:testDebugUnitTest` passed (47 tests, 0 failures); `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebugAndroidTest`, and `:app:lintDebug` passed. Android-test APK was compiled but not run. The APK reports package `cube.run`, version 2.10/code 20, and its debug signature verifies. No device or emulator was attached (`adb devices -l` empty), so no Infinix crash reproduction, phone UI/audio review, or FPS/lag measurement is claimed.

The old v2.9 debug signing key was unavailable, so this debug APK has a different signing certificate; Android may reject installing it over v2.9. Uninstalling the older build can erase local save data; back up anything important first. `/home/user/Final-Workspace/` has been refreshed with the matching 2.10 APK and source ZIP and contains exactly those two files.

## Release follow-up: Night, Hacker, portal safety, skins and Brazilian Phonk/Funk (2026-10-15)

Prepared version 2.11/code 21. Night (Moonlit Wilds) and Hacker (Matrix Circuit) are selectable campaign maps; both remain in normal rotation with the existing campaign worlds, including Railway. Night uses animated wolf/tiger road hazards, a predator-evade objective, lamp-lit scenery and authored sections. Hacker has Matrix-style servers/code scenery, left/right route patterns, firewall/drone/data-gate hazards, and a mixed-data-coin objective. New Codebreaker and Rift Ranger cube skins use IDs 18/19; Rift Ranger has visible animated arms, hands, legs and boots. The existing superhero-style skins and Original Cube Run selection remain unchanged.

All Track-portal crossings start a 1.5-second safe-arrival window: nearby hazards/pickups/coins are cleared, blank scoreless rows are streamed in, objectives are hidden and collision/side-hit damage is suppressed. Settings retains World Mix and adds a separately selectable Brazilian Phonk/Funk option; the sample-free procedural instrumental has a quicker syncopated groove. The launch-time generator now emits splash palettes for the new maps/skins.

Verification on this source: `:app:testDebugUnitTest` passed (51 tests, 0 failures); `:app:assembleDebug`, `:app:assembleDebugAndroidTest` (compiled, not run), and `:app:lintDebug` passed. The APK reports package `cube.run`, version 2.11/code 21, and has a debug signature. `adb devices -l` was empty: no device gameplay, user-reported crash reproduction, UI/visual review, audio audition, or FPS/performance measurement is claimed. The portal buffer is covered by a track unit test but has not been verified on a phone.

The old v2.10 debug signing key was unavailable; this APK's certificate differs, so Android may reject an in-place update. Uninstalling the earlier build may erase local save data; back up anything important first. `/home/user/Final-Workspace/` must contain exactly the matching `Cube Run.apk` and `Source Code.zip`.

## Deliverables

Keep `/home/user/Final-Workspace/` to exactly `Cube Run.apk` and `Source Code.zip`, refreshed from the same source revision. Exclude build/cache output, generated native binaries and local credentials/config from the ZIP.

## Release follow-up: Air, photo cube skins, shuffled portals and Space safety (2026-10-16)

Prepared version 2.12/code 22. Added **Air** as the ninth selectable campaign biome without removing the prior campaign maps; its map picker card, localized name, launch palette, cloud scenery, cloud gaps/bridges/thermals, and eagle, hawk, gull and flock obstacles are connected. The **Sky Soarer** cube is automatically shown on Air and remains a free, selectable Wardrobe skin. Three more free Wardrobe skins put the three supplied portraits on the front face of the original cube mesh; the cube collision/shape remains unchanged.

Bonus portals now use a per-run shuffled destination bag built from the currently unlocked bonus worlds, so destinations are not repeated before the pool is exhausted; cycle boundaries avoid an immediate repeat whenever the pool has multiple destinations. The normal portal pool now includes every score-unlocked bonus, with Space available from the start. For the reported Space/Black Hole entry/exit problem, the escape clock used to pause its *distance* during the generic safe-arrival interval while the exit row continued to move, which could let the gate pass before the clock said the route was complete. During the 1.5-second arrival buffer, route distance now advances with the track while the deadline and gravity field pause; a regression test checks that the live gate remains aligned. Challenge entry and exit also use the common hazard-clearing safe corridor.

Verification on this source: `:app:testDebugUnitTest` passed (60 tests, 0 failures); `:app:assembleDebug`, `:app:assembleDebugAndroidTest` (compiled, not run), and `:app:lintDebug` passed. The APK reports package `cube.run`, version 2.12/code 22; its debug signature verifies. The delivered APK is 14,286,090 bytes (SHA-256 `a58bba6e321f1c810a8d92c3dfc876ba0c1c90914ff9837a5145f1af75013f67`). No device or emulator was attached, so Air visuals, portrait rendering, audio, performance, and the reported Black Hole behavior have not been verified on a phone. No device crash resolution or all-device polish is claimed.

The v2.12 debug certificate SHA-256 (`ca92890fd0d93f9c180e53e045c0774d7d7e2a39c3a24d73ccd91ce787342c04`) differs from v2.11; Android may reject an in-place update. Uninstalling an earlier build may erase local save data, so back up anything important first. `/home/user/Final-Workspace/` is refreshed with exactly the matching `Cube Run.apk` and `Source Code.zip`.

## Corrective release: portrait photos mapped across the cube (2026-10-16)

Prepared version 2.13/code 23 after the user reported that the portrait skins showed no photo and a white side. The previous front overlay could not display its image correctly because the cube/plate mesh had no texture-coordinate attribute. Photo skins now use dedicated cube meshes with UVs and the corresponding portrait texture repeated across all six faces; the body diffuse stays white and the translucent glow shell is suppressed for these photo skins, preventing tinting or a pale overlay. A new instrumentation regression test checks the diffuse texture, distinct image per skin, UV attribute and six-face mesh.

Verification on this source: `:app:testDebugUnitTest` passed (60 tests, 0 failures); `:app:assembleDebug`, `:app:assembleDebugAndroidTest` (including the new portrait test; compiled, not run), and `:app:lintDebug` passed. APK package/version is `cube.run`, 2.13/code 23. The delivered APK is 13,613,879 bytes (SHA-256 `6030b21a6b82965fbfb7ee8ba553b1f347249b9dcf0fe9ab4c4874034fefe6f0`). No Android device/emulator was attached, so portrait appearance is not visually verified on hardware; the fix is built and covered by a compiled instrumentation test, not an on-device run. The debug certificate (`ca92890fd0d93f9c180e53e045c0774d7d7e2a39c3a24d73ccd91ce787342c04`) matches v2.12 and differs from v2.11, so upgrading from v2.12 should not require uninstalling; `/home/user/Final-Workspace/` contains exactly `Cube Run.apk` and `Source Code.zip`.

## Stage 1: real loading screen and menu handoff (2026-10-16)

Prepared version **2.14/code 24** as the first staged delivery from `docs/staged-upgrades.md`. The old timed/native cube-opening presentation is no longer used on normal launch. A new asset-free native loading screen tracks monotonic startup milestones, draws a 0–100 progress bar, and holds at 100 until fresh menu-scene frames have rendered; it then fades to the main menu. Normal launch stays on the menu rather than auto-starting. Explicit restart/autostart intents retain their behavior. English, German, and Hebrew loading strings were added, along with `StartupLoadProgressTest`.

Verification: `:app:compileDebugKotlin` passed; `:app:testDebugUnitTest` passed (62 tests, 0 failures); `:app:assembleDebug` passed. `:app:compileDebugAndroidTestKotlin` passed, but `:app:assembleDebugAndroidTest` did not finish its DEX merge before the 10-minute command timeout; no instrumentation tests were run. `:app:lintDebug` reached `lintAnalyzeDebug` but timed out, so lint is **not** claimed as passing for this stage. The APK identifies as package `cube.run`, version 2.14/code 24, size 13,618,055 bytes, SHA-256 `786032024ebfb27502f4879e4959c7e06718d0856a13a03880f808b0f5280d56`; its APK signature verifies.

No Android device/emulator was attached. The new screen's appearance, actual device launch transition, audio, and runtime performance therefore remain unverified on hardware. The local sandbox generated a new debug signing key: certificate SHA-256 `5733e1dd38cb2ec6e1cc67f68aee2aa3f48436639571e000adff651bc2a906f1`, which differs from v2.13's `ca92890fd0d93f9c180e53e045c0774d7d7e2a39c3a24d73ccd91ce787342c04`. Android may reject installing v2.14 over v2.13; uninstalling the older APK can erase local save data. The prior private debug key was not available here, so this debug build cannot be re-signed as the same update.

## Final integrated upgrade: Galaxy, new biomes, Glitch Nexus, Rift Ranger gait and original Urdu theme (2026-10-17)

Prepared version **2.15/code 25** after completing the remaining requested stages. Galaxy replaces the visible Space campaign/portal route, while the legacy numeric route ID remains an internal compatibility alias. Each Galaxy trip guarantees eight distinct planets and includes satellites, moons and stars. Snow, Lava, Jungle, Railway, Beach, Night, Matrix Circuit/Hacker and Air content remains integrated; biome objectives/obstacles/decor are present, including the Air helicopter fly-by. Glitch Nexus is both a selectable world and a score-unlocked portal bonus (best score 520), with its own code styling and data objective, while Matrix Circuit/Hacker remains a separate established map. Rift Ranger's running arm/leg gait alternates deterministically. The picker and Galaxy-facing copy were polished/localized. The original sample-free Urdu-inspired instrumental is a separate selectable style; Brazilian Phonk/Funk is retained, and no YouTube recording was bundled.

Verification on the final source: `:app:testDebugUnitTest` passed (76 tests, 0 failures); `:app:assembleDebug` passed; `:app:compileDebugAndroidTestKotlin` passed (instrumentation tests compiled but were not run). The successful build used JDK 17, Android SDK 35, Kotlin incremental compilation disabled, and a 640 MiB Gradle heap after lower/higher settings proved unreliable in the sandbox. The signed debug APK is package `cube.run`, version 2.15/code 25, 14,250,394 bytes, SHA-256 `96c0fa51c69e6cf57596190d3b3b9e39d6de61a33b45cb8c8f1ce7949839c411`; APK signature verification passed. Its debug-certificate SHA-256 is `7cc0c5889e8e3336863c322e90a6e0527a8ef7520088442519583d161c507ba5`. See `docs/staged-upgrades.md` for integrated scope and verification limits.

No device/emulator was attached (`adb devices -l` was empty). Therefore gameplay runtime, menu/map appearance, portrait visuals, audio audition, controls, and FPS/lag are not verified on hardware; no all-device polish or performance guarantee is claimed. The debug signing key may differ from previous delivered APKs, so Android may reject an in-place update; back up local save data before uninstalling an older version. The final matching `Cube Run.apk` and `Source Code.zip` are in `/home/user/Final-Workspace/`.

## Visual/content expansion (2026-10-17)

Prepared version **2.16/code 26**. Added a far-plane panorama for all selectable campaign maps using the existing blended ShapeRenderer pass, keeping new background art out of the 900-box WorldBoxBatch. Galaxy receives a fresh cosmic look and its own normal-gravity campaign obstacle/coin-field pool; Bonus Space keeps its distinct low-gravity routes. Lava now shows volcano silhouettes, erupting glow and falling molten particles, with new magma/basalt sections. Snowfall is denser, spans the playable road down to its surface and has updated deterministic motion tests. Snow/Lava/Galaxy obstacle sections were expanded. The native loading screen now shows an illustrated atlas of all 11 campaign maps; the 100%-to-menu handoff is unchanged. Off-road additions are scenery; mission pickups remain reachable on the road.

Verification: `:app:testDebugUnitTest` passed (78 tests, 0 failures); `:app:assembleDebug` passed; `:app:compileDebugAndroidTestKotlin` passed (instrumentation tests compiled, not run); `:app:lintDebug` passed (0 errors, 23 warnings). APK package/version is `cube.run`, 2.16/code 26, 13,759,299 bytes, SHA-256 `79f5b806f116e03431034b9964806e2c2392bbff6bead4aec821e076ca02f0fa`; v2 signature verifies. The debug-certificate SHA-256 `7cc0c5889e8e3336863c322e90a6e0527a8ef7520088442519583d161c507ba5` matches v2.15. No device was attached, so gameplay, visuals, controls, audio, and performance remain unverified on hardware. Final APK and source ZIP were refreshed together in `/home/user/Final-Workspace/`.
