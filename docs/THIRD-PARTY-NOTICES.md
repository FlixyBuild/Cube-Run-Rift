# Third-party notices

Cube Run's original source is licensed under the GNU GPL v3.0; see [`../LICENSE`](../LICENSE).

## Bundled assets

- **Fredoka** font — SIL Open Font License 1.1. The complete font license and copyright notice ship beside the font at `app/src/main/assets/fonts/OFL-Fredoka.txt`.
- **Launcher artwork** — generated specifically for this project for the Riftbreak campaign; no third-party logo, character, or image source was used.
- **Sound** — the project retains its existing procedural sound/music core. No third-party audio files are bundled in `app/src/main/assets`.

## Runtime and test dependencies

The exact dependency versions are declared in `app/build.gradle.kts` and the root Gradle build files.

- **libGDX 1.13.1** (`gdx`, `gdx-backend-android`, and native runtime) — Apache License 2.0. Full text: [`../THIRD_PARTY_LICENSES/Apache-2.0.txt`](../THIRD_PARTY_LICENSES/Apache-2.0.txt).
- **AndroidX runtime libraries** — Apache License 2.0; same full text above.
- **Kotlin standard library** — Apache License 2.0; same full text above.
- **JUnit 4.13.2** (test-only) — Eclipse Public License 1.0. Full text: [`../THIRD_PARTY_LICENSES/JUnit-4.13.2.txt`](../THIRD_PARTY_LICENSES/JUnit-4.13.2.txt). An additional copy is at [`../THIRD_PARTY_LICENSES/EPL-1.0.html`](../THIRD_PARTY_LICENSES/EPL-1.0.html).
- **Android SDK Platform/Build Tools** are build-time tools, not app runtime assets; their use is subject to the Android SDK License accepted by the developer during setup.

Dependency metadata and notices remain available through each Maven artifact's POM. The source archive includes the project license, font license, and the Apache/EPL license texts listed above.