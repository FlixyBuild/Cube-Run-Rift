# Prompt 1 — Repository audit and safe upgrade foundation

- **Audit date:** 2026-10-08
- **Base:** `main` at `7246aebe38ab54efec09cf1dab4f114ed0df7f73`
- **Review branch:** `agent/prompt-1-audit-foundation-20261008`
- **Scope:** audit and build foundation only. No gameplay feature work or engine migration.

## 1. Checkout, guidance and recovery

- The repository was cloned from the supplied URL into the prescribed Worktrees location. The checkout was clean before work began; there were no user edits to preserve.
- `AGENTS.md` was read and followed. No `CLAUDE.md` or additional `AGENTS.md` files were present. `.codex/config.toml` contains host-specific sandbox write-root configuration; it was left unchanged.
- A dedicated branch was created before editing. No commit was made. Build products and generated libGDX native files remain ignored by Git.
- The Android package identity remains `cube.run` (namespace `cube.run`), version `2.6` / code `16`; `minSdk 28`, `targetSdk 35`, portrait orientation. No identity or gameplay source was changed.

## 2. Architecture and feature inventory

### Build and platform

| Area | Current implementation |
|---|---|
| Android app | Single `:app` module; `GameActivity` hosts the libGDX surface and Android `Hud` overlay; `App` initializes persistence, audio and haptics. |
| Shared gameplay/test model | `:bot` is a Kotlin/JVM module, also consumed by the app. It contains the deterministic planning/model code and a host-side `selfTest` task. |
| Build tools | Wrapper Gradle 8.13; Android Gradle Plugin 8.7.3; Kotlin Android/JVM 2.0.21; libGDX 1.13.1; Java/Kotlin bytecode target 17. Compile/target SDK 35. |
| Native libraries | libGDX natives are extracted during the build for `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`; generated `app/libs/` is gitignored. |
| App permissions | Manifest declares `VIBRATE`; no `INTERNET` permission is declared. The game is local/offline. |

### Game loop, rendering and input

- `core.Gdx3DGame` owns the libGDX GL-thread lifecycle, camera, frame loop and ordered render passes. `FrameStepper` divides elapsed time into bounded simulation steps so hitches do not simply tunnel through rows. Rendering uses `ModelBatch`, `ShapeRenderer`, custom mesh batches (`WorldBoxBatch`, `PrismBatch`, facet/crystal/capsule batches), pooled shards and inline shader code. It is a real 3D renderer, not a static scene.
- `game.CubeRun` is the run conductor: start/death state, input routing, track updates, collision/pickups, score callbacks, power-ups, worlds and render composition. `game.Player` owns lane position, jump physics, roll/duck and airborne slam, flight/hover modes, cosmetic pose and trail.
- Touch handling is in `core.gfx.TouchInput` (tap/drag/swipe, single active pointer); keyboard/gamepad handling is in `core.PhysicalInput`. Left/right move lanes; up jumps; down rolls while grounded or slams while airborne. The existing five-lane/portal worlds also reshape lane geometry through `game.Lanes`.
- `game.track.Track` generates and scrolls rows using `Sections` and `ObstacleFactory`. `Ob`/`Row` are the obstacle/row data model. Solid collision is evaluated in `CubeRun.collide`: sideways and vertical overlap of the player and obstacle boxes; pads/platforms/decorations and timed/moving hazards have their existing specialized behavior. `Difficulty` owns the run speed/tier ramp. The code documents safe-lane sequencing and recovery spacing; current balance and section behavior are covered by instrumentation and bot-model tests.

### Screens, game state, scoring and persistence

- The Android `Hud` hosts the in-run HUD and pages/sheets including main menu, pause, run-over/rewards, shop, wardrobe, achievements, language and section views. The live 3D scene and UI are retained across menu/retry flows.
- `core.GameSession` is the game-to-host callback contract. `GameHostSession` bridges GL-thread events to the UI thread and handles run banking/challenges; atomics protect values shared with UI reads. `core.Stage` is the UI-to-GL request/state bridge (including page mode, pause and preview requests) using atomic/volatile fields.
- `data.Progress` stores coins, upgrades, wardrobe, achievements, shards and rewards in `SharedPreferences`; `data.Scores` stores local high scores; `data.Settings` stores player-facing options such as sound, haptics and controls. Persistence is already substantial; future economy or key changes must preserve/migrate existing saves.
- Audio is procedural: `core.SoundFx` synthesizes PCM/WAV effects into the app cache and plays them with `SoundPool`; it is not loading bundled music/effect files. `core.Haptics` dispatches vibration on a background handler and coalesces pending effects. Both respect settings. Space/other GLSL is embedded in source; no external shader files were found.

### Assets, licensing and release

- The project `LICENSE` is GPL-3.0. The bundled Fredoka font has its full SIL OFL 1.1 license at `app/src/main/assets/fonts/OFL-Fredoka.txt`. libGDX 1.13.1 Maven POMs declare Apache-2.0.
- `app/src/main/res/drawable-nodpi/portal_pop.webp` is a lossless launcher-art image used by the adaptive icon, approximately 1.0 MB. No separate provenance/attribution/license note for this artwork was found in the repository; confirm its origin before reusing it in new distribution assets. Existing file was not modified.
- Store icon/screenshots and fastlane metadata are under `fastlane/metadata/android/en-US/`. The debug-only `app/src/debug/res/raw/intro_probe.mp4` is a test fixture, not a main asset.
- `.github/workflows/build.yml` builds debug + lint + test APKs on non-tag builds and uploads an artifact. `v*` tag builds require signing secrets and publish a GitHub Release; no signing material is checked into the project. Local unsigned release builds are supported by the Gradle script. `fastlane` has local debug/release lanes.

### Tests and QA already in the repository

- `app/src/androidTest` contains 90 Kotlin files and 303 `@Test` annotations covering game, input, rendering, economy/persistence, UI, lifecycle, performance and bot replay. There is no `app/src/test` JVM test source tree; consequently `testDebugUnitTest` resolves to `NO-SOURCE`.
- `tools/bot` has its own runnable `:bot:selfTest` suite. Device instrumentation tests require a connected device/emulator; build/assembly alone does **not** execute them.
- `TASK-STATUS.md` records prior issue-27/split-screen and compact-layout verification on an emulator/phone. Those are historical project results, not device tests run for this audit.
- `docs/performance.md` contains detailed device benchmark and APK-size history. Its explicit older baseline (commit `2cc97bd`, Moto G7 Power, 60 Hz) records maximum-speed cruise frame p50/p95/p99/max of **16.72 / 19.30 / 23.60 / 26.64 ms**, rolling hills **16.75 / 18.80 / 19.84 / 21.73 ms**, and repeated Second Wind **16.67 / 19.43 / 22.93 / 42.62 ms**. Those are historical, device-specific figures—not a new measurement of this checkout. No fresh frame-time benchmark was possible here.

## 3. Build baseline, verified fix and status

### Baseline failures (before the foundation change)

1. The sandbox initially had only Java 11, no Android SDK and no `adb`. The repository requires Gradle/AGP to run on Java 17 (`gradle/gradle-daemon-jvm.properties` sets toolchain version 17). The initial CI build command failed before compilation with: **“No defined toolchain download url for LINUX on x86_64 architecture.”** This was a missing local JDK/toolchain setup, not a Kotlin/game source diagnostic.
2. After supplying a temporary JDK 17 and SDK outside the repository, the original Gradle heap settings (`-Xmx4g`, 1 GiB metaspace, parallel execution) caused a verified global OOM in this 2 GiB worker. The Gradle daemon was killed while dex/build work was in progress. This established a reproducible local-build blocker for constrained workers.
3. A combined low-memory retry initially timed out during Android instrumentation Kotlin compilation. That attempt was not treated as a pass. After applying the bounded settings below, the Android test APK was separately built successfully, then the CI-equivalent task group completed.

### Foundation change and final verification

`gradle.properties` now uses one bounded JVM for Gradle/Kotlin, a 900 MiB heap, a 384 MiB metaspace cap, one worker and no parallel project execution. This trades some build throughput for a stable memory ceiling. No SDK/plugin/engine/gameplay dependencies were changed.

| Command / check | Result |
|---|---|
| `./gradlew --no-daemon --no-build-cache clean :app:assembleDebug --stacktrace` | **Passed from clean outputs** with the repository memory settings; 42 tasks executed; about 1m41s. |
| `./gradlew --no-daemon :app:assembleDebugAndroidTest --stacktrace` | **Passed**; instrumentation Kotlin sources compiled and test APK packaged; about 1m10s. |
| `./gradlew --no-daemon assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest :bot:selfTest --stacktrace` | **Passed**; includes the exact CI debug/lint/unit-task/test-APK task group plus bot self-tests. Android instrumentation tests were assembled, not executed. |
| `./gradlew buildDebug --stacktrace` | **Passed**; documented alias for `assembleDebug` (up-to-date on this invocation; clean assemble separately passed above). |
| `lintDebug` | **0 errors, 20 warnings**. Warnings include draw-time allocations, plural/resource suggestions, a long vector path, custom-view/accessibility suggestions, capitalization/localization, and API-guard suggestions. They were not changed in this scope. |
| `testDebugUnitTest` | **NO-SOURCE**; there is no JVM unit-test source set. Do not interpret this as the Android instrumentation tests running. |
| `:bot:selfTest` | **Passed**: low-gravity jumps/course files, unreachable states, committed input handoff, springboard interior, stride reconstruction, safe/fatal loot, timing centering, flight expiry, Zero-G jet priority/glide and fixture codec. |
| Android device execution / install | **Not run**: `adb devices -l` reported no attached device; no emulator was configured. |
| Release-signed APK | **Not run**: the tag release path needs maintainer signing secrets; none were read or added. |

The clean debug APK baseline is **11,830,505 bytes (about 11.28 MiB)**; uncompressed ZIP entries total 12,125,987 bytes. `aapt` confirms package `cube.run`, version code 16/name 2.6, min SDK 28 and target SDK 35. The APK includes all four libGDX ABI natives and the debug intro probe video. This is a **debug APK size**, not a release-size claim. The built test APK is 2,230,367 bytes.

Other non-fatal build observations: Kotlin reports existing deprecation/condition warnings; Gradle reports features deprecated for Gradle 9; Android packaging says `libgdx.so` cannot be stripped and will package it as-is. None blocked the verified build.

### Reproducing the local build

Use an installed JDK 17 and Android SDK Platform 35. Set `JAVA_HOME` and `ANDROID_SDK_ROOT` to their local install locations (do not commit either path), then:

```sh
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
./gradlew clean :app:assembleDebug
./gradlew assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest :bot:selfTest
```

`assembleDebugAndroidTest` compiles/packages the instrumentation APK; executing those Android tests additionally requires a connected authorized device or emulator.

## 4. Compatible interfaces for later phases

These boundaries already exist and are preferable to adding empty abstraction layers before a feature needs them:

| Later concern | Existing compatible seam | Safe extension rule |
|---|---|---|
| Player controller | `game.Player` verbs (`moveToLane`, `jump`, `downAction`) and update events; `TouchInput` / `PhysicalInput` route commands. | Add a separate controller interface only if a second real controller implementation is introduced. Keep collision-driving state on the GL thread. |
| Obstacles and collision | `game.track.Ob`, `Row`, `ObType`, `ObstacleFactory`, `Track`; `CubeRun.collide` resolves game interactions. | Keep obstacle data/generation separate from the collision pass. Add deterministic boundary tests for any changed collision rule. |
| Score and progression | `core.GameSession` callbacks, `GameHostSession`, `data.Scores`, `data.Progress`. | Send run events through `GameSession`; keep durable economy in `Progress`; never award/bank twice across UI presentation. |
| Game state and pause | `CubeRun` owns run state; `Stage.paused`/mode are the cross-thread request seam; `Hud` and `GameActivity` own UI/lifecycle. | Preserve the GL/UI threading contract and pause/resume semantics; do not introduce a second source of truth without a migration plan. |
| Settings and persistence | `data.Settings` and `data.Progress` / `Scores` over app `SharedPreferences`. | Add defaults and migration for any new saved key; do not clear existing player data during tests. |
| Audio and haptics | `core.SoundFx`, `SoundPlaybackQueue`, `core.Haptics`, gated by `Settings`. | Route effects through these services; preserve off-thread dispatch and mute/haptic settings. |
| Performance configuration | `GameActivity` configures libGDX (`useGL30`, samples, depth); `Gdx3DGame`, `FrameStepper`, `PerfMonitor` and batched renderer classes own frame/perf behavior. | Make renderer/device changes only against measured device data and rendering regressions; no unmeasured quality downgrade. |

### File-by-file change map for Prompt 2

No feature scope was supplied yet, so the only implementation edit in Prompt 1 is `gradle.properties`. The following are candidate ownership files, **not blanket approval to rewrite them**:

- Gameplay/control: `app/src/main/kotlin/cube/run/game/CubeRun.kt`, `Player.kt`, `Difficulty.kt`, `game/track/{Track,Obstacles,Sections}.kt`, plus a focused regression under `app/src/androidTest/kotlin/cube/run/game/` or `.../track/`.
- Rendering/performance: `core/Gdx3DGame.kt`, `core/FrameStepper.kt`, only the affected `core/gfx/*` renderer, and matching render/performance tests.
- Cross-thread state/services: `core/{GameSession,GameHostSession,Stage,SoundFx,Haptics}.kt`; change only if the feature crosses those boundaries.
- Persistence/progression: `data/{Settings,Progress,Scores}.kt` and matching `androidTest` data tests; preserve current keys/balances unless a migration is explicitly designed.
- UI/localization: `ui/Hud.kt` plus the one affected view/sheet and matching `app/src/main/res/values*` strings; preserve English/German/Hebrew resources and compact-window behavior.
- Build/dependencies: `app/build.gradle.kts` only for a justified SDK/dependency need. Keep `applicationId`, libGDX, Android app module and four native ABIs unless a requirement demonstrates otherwise.

## 5. Risks, acceptance criteria and rollback

### Main risks

- GL-thread ownership and UI-thread callbacks are deliberate; do not mutate libGDX/GL objects from Android UI or instrumentation threads.
- `CubeRun`, `Player` and `Progress` are large but established production classes. Large rewrites risk changing timing, existing section solvability, economy, saved data, visuals and screen transitions. Prefer narrow changes with regression tests.
- Instrumentation test APK compilation is verified, but tests were not run on hardware. Visual/rendering, touch latency, audio/haptic behavior and device performance still need a physical device or emulator.
- The 900 MiB / single-worker Gradle setup is verified on the constrained worker; it may build more slowly on larger CI hosts. Revisit only with measurements if future compilation outgrows the cap.
- The portal artwork’s provenance remains to be confirmed. Release signing remains controlled by maintainer secrets; never add signing credentials to the repository.

### Measurable phase checklist

- [x] Inspect actual checkout and project instructions; preserve the existing branch baseline.
- [x] Create recovery branch before editing; retain the existing package/engine/architecture.
- [x] Record toolchain/SDK/dependency versions, feature ownership, tests, release flow and historical performance reference.
- [x] Reproduce and separately classify local JDK/SDK and memory failures.
- [x] Bound build memory; verify a clean debug APK, lint task, no-source unit task, bot self-test and Android test APK assembly.
- [ ] Prompt 2 scope approval: list the exact requested features, priority/order and measurable gameplay/UI/persistence/performance acceptance criteria.
- [ ] Implement one feature slice at a time, reusing the interfaces above; do not add placeholder systems or remove current features.
- [ ] For each slice, add deterministic/gameplay regression tests and run the complete build/test task group; run instrumentation on a device/emulator before calling visual/input acceptance complete.
- [ ] Compare final APK size and device performance against the measured baseline; check release signing only through the existing secret mechanism.

### Rollback

The branch contains uncommitted reviewable changes. To inspect them use `git diff main` and `git status`. If, after review, this Prompt 1 work should be discarded, restore `gradle.properties`, remove the new audit note, switch back to `main`, and delete the branch only after confirming there are no later user edits on it. No rollback or branch deletion was performed.

## Follow-up: Riftbreak campaign replacement (2026-10-14)

This follow-up intentionally supersedes the audit's earlier “no gameplay feature work” scope and the old roster's player-facing availability; the recovery branch/history and existing system core were retained.

- The live campaign and World Picker now expose only **Jungle Canopy** and **Riftbound Space**. Legacy biome definitions remain available to debug/test routes but are no longer in normal world rotation or player selection.
- Jungle has an eight-green-coin harvest objective. Space is entered through a world gate or portal and begins the playable 150-unit / 14.5-second black-hole escape, with a moving tesseract, coloured coins, nearby-object gravity and a fatal timeout.
- Six replacement cube skins, a timed skateboard pickup/effect/model/HUD timer and the jetpack integration are part of the active run. The old `portal_pop.webp` launcher artwork was removed and replaced by new project-specific Riftbreak icon artwork; stale storefront screenshots were removed and metadata updated.
- GPL-3.0 project license, Fredoka OFL, Apache-2.0 and JUnit EPL notices are now documented in `docs/THIRD-PARTY-NOTICES.md` and `THIRD_PARTY_LICENSES/`.
- Final verification passed: `testDebugUnitTest assembleDebug assembleDebugAndroidTest :bot:selfTest` completed successfully; all 13 JVM tests passed, the Android instrumentation APK compiled, and bot self-tests passed. `lintDebug` also passed separately (22 non-fatal warnings). An earlier combined invocation was killed at lint analysis by the constrained Gradle daemon; rerunning lint alone with a lower-heap/larger-metaspace profile succeeded.
- The delivered debug APK is freshly built for package `cube.run`, version 2.6 / code 16 (15,074,732 bytes); its SHA-256 and package/SDK metadata were checked. `Source Code.zip` passed ZIP CRC and required-source/license checks (404 files, approximately 6.2 MB). The delivery folder contains exactly those two files and totals approximately 21.3 MB.
- No `adb` executable/device or `/dev/kvm` is available here, so Android instrumentation tests were compiled but not executed, and device rendering/input/audio/gameplay remain unverified.

## Follow-up: difficulty profiles and Jungle/Space obstacle variety (2026-10-14)

This is the user's narrow gameplay follow-up: retain the existing Cube Run presentation and make the repeated box-style hazards more varied, while adding Easy, Medium and Hard options with real gameplay effects in both campaign biomes.

- Added persistent Easy/Medium/Hard profiles in Settings. Profiles vary starting pace, auto-ramp ceiling/rate, run speed, dodge spacing and which Space section tiers can unlock. Hard Space patterns are available earlier; Easy keeps the gentler tier-0 Space set. Existing hitboxes, collision handling, sound core and campaign routing are unchanged.
- Added a pure obstacle-art resolver and Jungle/Space render paths. Jungle hazards now read as thickets, logs, vine gates/swings, snapping plants, falling pods, root bridges, mushroom pads and swamp gaps. Space hazards read as asteroids/meteors, crystal/energy walls, lasers, solar arrays, plasma spires, drop pods, orbital drones, station hull/panels, gravity rings and rifts. The Space art path is selected for both the dedicated Space world and Space bonus portals reached from another biome. Decorative details are bounded and the new visuals reuse the existing collision geometry.
- Added localization for English, German and Hebrew, a Settings-flow instrumentation regression, and JVM checks for profile pacing/ramp, tier availability, Space spacing and biome-art mapping.
- Verification passed in separate low-memory Gradle invocations: `assembleDebug`, `testDebugUnitTest` (17 tests, zero failures), `assembleDebugAndroidTest` (instrumentation APK compiled) and `lintDebug`; `:bot:selfTest` also passed. A combined invocation was killed by the constrained worker while compiling the unit-test classpath, so the build and verification tasks were run separately with a lower heap. This is a resource limitation, not a source/test failure.
- `adb devices -l` reports no attached device. The instrumentation tests and in-game visuals/input/audio for this follow-up therefore were not executed on a phone or emulator; the user's earlier report about the previous build remains their own verification.
