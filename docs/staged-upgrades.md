# Cube Run — completed upgrade stages

## Decisions preserved

- Galaxy replaces the former Space map/route. It is the visible name in the world picker and portal copy; no separate Space campaign map was added.
- The loading screen reaches 100% at the main menu and does not auto-start a run.
- Existing Cube Run identity/package (`cube.run`), campaign features, Railway, World Mix, Night/Hacker, and all established gameplay stay in place unless a request specifically changed them.
- The three portrait cube skins continue to use their chosen portrait across all six faces; the translucent shell is suppressed so no white face or washout is introduced.
- No linked YouTube recording is bundled. Brazilian Phonk/Funk remains a separate selectable style.

## Stage 1 — startup/loading handoff (v2.14)

- Replaced the timed cube-opening launch presentation with an asset-free, milestone-driven 0–100 loading screen.
- Progress is monotonic and reaches 100% only after fresh menu-scene frames render. The screen fades away to the main menu; normal launch does not start a run.
- Explicit restart/autostart intents retain their existing behavior. English, German, and Hebrew copy is present.

## Stages 2–6 — integrated game upgrade (v2.15)

### Galaxy and portal routes

- The visible campaign/world picker and bonus portal call the former Space route **Galaxy**. Existing numeric save IDs remain compatible; the former Space ID is an internal alias, not a second selectable map.
- A trip guarantees eight visually distinct planets and adds moons, satellites, stars, orbital scenery, and a longer route.
- A separate Glitch Nexus destination is selectable as a campaign world and unlocks as a portal bonus at best score 520. Its portal run has its own code-road palette, code-rain, server-pylon scenery, matching obstacle art, and data-fragment objective; it does not overwrite the Matrix Circuit/Hacker world or its campaign objective.

### Biomes, set-pieces and objectives

- Winter retains moving snowfall, firs, ice hazards, and a survival objective.
- Lava is selectable with cave/crystal scenery, molten obstacles, and a crystal objective.
- Jungle keeps dinosaur crossings, canopy scenery, and an evade objective.
- Railway, Beach, Night, Hacker/Matrix, and Air retain their distinct scenery, obstacle pools, and objectives. Beach gains its matching crab/surf/boardwalk obstacle art; Air includes a deterministic distant helicopter fly-by alongside its aircraft/bird content.
- New map names and the Galaxy-labelled section explorer/difficulty copy are localized in English, German, and Hebrew.

### Movement, audio, UX and performance review

- Rift Ranger has a deterministic, alternating arm/leg run cycle with lifted feet; the gait is scoped to that skin.
- The picker groups new campaign worlds up top and has dedicated map glyphs. Existing safe-arrival portal corridors and the 100%-to-menu handoff remain intact.
- Added an original, sample-free Urdu-inspired instrumental loop, with a restrained hand-drum pulse and original melody; no external recording or uncleared melody is included. Brazilian Phonk/Funk remains independently selectable.
- Code review favored reused renderer colors/buffers and deterministic decorative motion; no frame-rate or latency guarantee is made without hardware measurement.

## v2.15 verification

- `:app:testDebugUnitTest`: **76 tests passed, 0 failures**.
- `:app:assembleDebug`: **passed**; signed debug APK package `cube.run`, version 2.15/code 25, 14,250,394 bytes, SHA-256 `96c0fa51c69e6cf57596190d3b3b9e39d6de61a33b45cb8c8f1ce7949839c411`; APK signature verification passed.
- `:app:compileDebugAndroidTestKotlin`: **passed**. Instrumentation tests were compiled, not executed.
- The final Gradle run used JDK 17, Android SDK 35, one worker, Kotlin incremental compilation disabled, and a 640 MiB Gradle heap after larger settings exceeded the sandbox memory limit.
- No device/emulator was attached. Runtime gameplay, visual appearance, controls, audio audition, and actual-device performance have **not** been verified. Do not interpret unit tests or a successful build as hardware validation.
- The delivered artifact is a debug-signed APK. The sandbox debug key may differ from a previously installed build; Android may reject an in-place update. Back up local save data before uninstalling an older build.

No licensed Brazilian Funk master was supplied, so the original Urdu-inspired instrumental was used instead. The linked YouTube audio was not copied or bundled.

## Visual/content expansion and loading atlas (v2.16, 2026-10-17)

- Added a reusable far-plane `ShapeRenderer` panorama for every selectable campaign biome. It draws before the 3D world boxes, so volcano ridges, frost peaks, city silhouettes, jungle canopy, railway structures, beach/moon/cloud scenes, Nexus architecture, and Galaxy planets/nebulae add depth without increasing `WorldBoxBatch` occupancy.
- Reworked the selectable **Galaxy** presentation with a new cosmic backdrop and an authored campaign-only obstacle pool that uses normal-gravity-safe routes; the low-gravity Bonus Space section library remains separate. Star-trail rows also add reachable coin objectives on the road.
- Lava now has layered volcano scenery, erupting glow, roadside falling-lava particles, and additional magma/basalt obstacle patterns. Snowfall was increased and focused across the road; flakes descend to the road surface on a faster deterministic cycle. Additional snowdrift/icefall sections broaden the Winter route.
- Added more themed obstacle/coin-field patterns while preserving the established biome missions. Gameplay objectives and pickups remain on reachable routes; outside-road additions are scenery, not unreachable collectibles.
- The asset-free native loading view now contains a two-column illustrated atlas of all **11 campaign maps**. Progress still reaches 100% at the rendered main menu and does not auto-start gameplay.

### v2.16 verification

- `:app:testDebugUnitTest`: **78 tests passed, 0 failures**.
- `:app:assembleDebug`: **passed**. APK package `cube.run`, version **2.16/code 26**, 13,759,299 bytes, SHA-256 `79f5b806f116e03431034b9964806e2c2392bbff6bead4aec821e076ca02f0fa`; APK signature verified with v2. The debug certificate matches v2.15 (`7cc0c5889e8e3336863c322e90a6e0527a8ef7520088442519583d161c507ba5`).
- `:app:compileDebugAndroidTestKotlin`: **passed**; instrumentation tests were compiled, not executed.
- `:app:lintDebug`: **passed**, 0 errors and 23 warnings.
- JDK 17 / Android SDK 35; one Gradle worker, Kotlin incremental compilation disabled, and a 640 MiB Gradle heap.
- `adb devices -l` returned no attached devices. Gameplay, visual appearance, controls, audio, and performance have **not** been verified on hardware. This is a debug-signed build; always back up local save data before replacing an older installation.
