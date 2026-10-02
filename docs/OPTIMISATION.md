# Android optimisation

The September 30, 2026 pass enables release R8 code optimisation and resource
shrinking, skips unchanged 3D transform updates, removes a per-frame filtered
node list, and limits decorative light updates to 30 Hz while the lifecycle is
STARTED. The SceneView renderer still renders continuously; these changes do not
establish an idle-GPU or battery-life improvement by themselves.

The first successful shrunk build measured 90.48 MiB for the universal APK and
82.16 MiB for the AAB. Previous saved artifacts were approximately 106 MiB and
95 MiB respectively, from different source revisions. Those are historical
comparisons, not controlled before/after measurements. The APK included roughly
57.23 MiB of compressed assets and 26.20 MiB of native libraries. AAB size is not
Play download size; measure ABI-specific delivered and installed sizes separately.
Offline assets and ABI support remain available.

## Flowers

The original full flower size is restored on both the plate and the altar, with
no shrinking during flight. Six replenishing plate flowers and four visible
slots per deity bound the scene cost while retaining the full offering history.

Placement uses conservative 3D bounding spheres: a model fitted to maximum
dimension `u` has radius at most `sqrt(3) * u / 2` in scene units. Where projected
flower positions overlap, a one-time depth calculation separates their spheres
with a small gap. Large blooms can visually overlap as a pile without their
meshes intersecting. Tests check 3D separation across all shrine/idol combinations,
rotations (via sphere bounds), slot reuse, and preservation of the original size.

One flower flight runs at a time, its source is hidden during pickup, and flight
depth interpolates between plate and destination. This is bounded decorative
placement, not a gravity/contact physics simulation.

## Remaining measurement

Profile the release build on physical low/mid-range devices: cold/warm startup,
frame-time percentiles/jank, memory, idle foreground, a complete puja, background,
screen-off, and background audio. Keep brightness, refresh rate, network and
thermal conditions consistent for power comparisons. Emulator smoke tests cannot
establish real battery consumption or physical-device frame performance.

Before distribution, smoke-test minified JNI/3D loading, every catalogue model,
authentication and media playback. Dependency consumer rules are retained;
add narrow keep rules only if an actual reflection/JNI regression requires them.

## Verification in this workspace

- 38 release unit tests passed, including conservative flower-bound separation
  across all shrine/idol combinations and slot reuse.
- R8 release APK and AAB builds passed with release lint checks.
- An isolated minified release app launched on the Android emulator and loaded
  the temple's Filament models; no AndroidRuntime crash was logged during this
  smoke check. The final plate layout was visually checked and a flower offering
  settled successfully. This is not full catalogue, authentication or media validation.
- The personal `optimiser` skill was created and passed the skill validator.

## Phone model asset pass

All 17 runtime GLBs were regenerated from preserved sources (or the procedural
lota generator). Model files: 39.06 → 13.25 MiB, a 66.1% reduction. Catalogue
triangles: 335,916 → 156,225, a 53.5% reduction. Embedded texture pixels:
34,603,008 → 9,306,112, a 73.1% reduction. Pixel counts indicate potential decoded
texture savings, not a measured process/GPU allocation or battery improvement.

See ASSET_PIPELINE.md for per-category budgets and reproduction commands. All
original source hashes were checked unchanged. Before/after exports were rendered
with the same camera and lighting. 512px is the normal texture cap; peony/lily
retain 768px textures and 18k triangle budgets after visual review. The runtime
flower size/placement and ritual interactions are unchanged by this asset pass.

Final packaging after the model pass: universal release APK **66.39 MiB**;
release AAB **58.08 MiB**. Saved pre-pass artifacts were 90.48 MiB and 82.17 MiB
respectively. AAB bytes are not the Play-delivered device download size.
Validation: 38 release unit tests passed; the emulator instrumentation test loaded
all 17 final GLBs successfully; default-shrine visual smoke check passed with no
AndroidRuntime crash logged. Release APK and AAB packaging passed. The original
source archive remains unchanged. Physical-device FPS, GPU memory and battery
measurements remain outstanding.
