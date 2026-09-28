# Shrine assets and customization

## What is available

The Android picker contains 24 choices in seven categories:

- Four shrines: original wood, marble arch, ivory alcove, carved sanctuary.
- Six idol arrangements: original Ganesha/Lakshmi, Ganesha/Hanuman, Shiva, Lakshmi, Durga and Ram Darbar.
- Five flower choices: original sunflower/peony mix, orchid, rose, azalea and lily.
- Two oil lamps, two shankhs, two aarti lamps and three prasad choices (laddu bowl, halwa bowl, 3D Tirupati laddu).

Use **Temple options → Customize temple**, or **My progress & temple package → Browse temple designs**. All users can preview a design. Applying premium options requires a signed-in, unlocked account and a successful server save. Closing the picker discards the draft. Applying restarts the current puja; already credited days are unchanged. On a fresh app process, sign in to restore server selections. Single idols and the Ram Darbar group use one ritual target; paired idols use two. Music is unchanged.

## Folders

| Folder | Purpose |
| --- | --- |
| `temple_models/source/` | Original supplied files, grouped and named by category; never altered by the preparation pipeline |
| `android/app/src/main/assets/shrine/` | Runtime GLBs, transparent sprites, empty portrait backgrounds and thumbnails |
| `shared_assets/catalog/` | Shared catalog, source migration manifest and optimization report |
| `backend/internal/community/catalog.json` | Generated catalog embedded in the Go server |

The original `imgres.html` is preserved under `source/references/`; it is not treated as an image or bundled in the app. Unused flame model is preserved under `source/legacy/`. Original full-resolution GLBs no longer duplicate optimized assets inside the APK. The original water jug derivative remains in the source inventory, but Abhishek now uses a procedural copper lota based on the supplied visual reference. Regenerate it with `python3 scripts/generate_copper_lota.py`. The lota is a ritual animation asset, not a selectable category.

## Model preparation

Run Blender with `--background --python scripts/prepare_models.py`. The script reads the independent source inventory in `model-sources.json`, welds disconnected scan triangles before decimation, targets 30,000 flower faces / 18,000 other faces, limits embedded textures to 1024px, exports self-contained GLB and renders thumbnails. Original UV/material data is retained. The azalea source's detached colour calibration cube is removed. Node transforms and model normalization remain compatible with SceneView.

The 16 source GLBs total approximately **195 MiB**; mobile derivatives total approximately **39 MiB** (about 80% smaller). Exact sizes and polygon counts are in `shared_assets/catalog/model-report.json`. Thumbnails are previews, not extra scene geometry. Re-run visual checks after changing budgets or materials.

Run `python3 scripts/build_catalog.py` after changing catalog definitions. It emits byte-identical Android, Go and shared copies. Run `python3 scripts/verify_assets.py` to check catalog synchronization, file existence, and GLB structure/embedded dependencies. The server rejects unknown asset IDs and mismatched category selections.

## Image preparation

Prepared with the built-in **imagegen** skill/tool, not the fallback API. Source photographs were low-resolution references; generated derivatives reconstruct detail and are not pixel-identical retouches. Original photos remain in the source archive.

Final prompt set:

- Idol cutouts: extract the complete idol/group and base, preserve identity, pose, ornaments and material; remove all surrounding objects; output true transparent PNG with a small margin. Lakshmi's clipped lower base was reconstructed.
- Original wooden shrine: remove only the two central freestanding idols and reconstruct the shelf/back panel; preserve architecture, decorations, carpet and framing.
- Additional shrines: remove idols and offerings; preserve architectural identity and lighting; reframe to 940:1672 portrait with the altar near 39% of image height and expansive empty floor below. Runtime placement offsets were measured for each background.
- Halwa: extract the glass bowl and contents; remove text, flowers, floor, all background and external glow; output a clean transparent sprite. Earlier halo-containing outputs were rejected.
- Original idol pair: extract only the two central figures and lotus bases, keeping their relative heights and spacing, on transparent alpha.

Final images are in runtime `backgrounds/`, `idols/`, and `prasad/`. Generated candidates remain outside the runtime bundle. Review reconstructed religious imagery before public release; provenance/usage permissions still belong to the source supplier.

## Local development

Backend: from `backend`, run `go run ./cmd/server`; it binds `127.0.0.1:8080`. From `android`, build with `./gradlew assembleDebug -PaccountApiUrl=http://10.0.2.2:8080`. Only debug builds allow HTTP to emulator loopback/localhost. Release account URLs require HTTPS; a build without an API URL keeps free puja and previews available, and explains that accounts are not connected.

Local mock purchases are available with `MOCK_MODE=true`: sign in with a mobile number and OTP `1234`, then choose “Unlock with mock payment · ₹0”. No charge is made. Real store billing remains pending. Asset selection alone does not create an entitlement.

## Verified in this iteration

- Debug APK builds; 26 Android JVM tests pass, including a complete single-idol eight-step session and preservation of paired-idol requirements.
- Go tests pass with race detection. Catalog tests cover locked-account rejection, invalid asset rejection, persisted selection restoration and six-category client compatibility.
- Catalog validator confirms all 24 choices, thumbnails and self-contained GLBs exist and app/server catalogs match.
- Emulator check against an isolated local backend: signed in, qualified referral unlocked the test account, selected and saved all seven custom categories through Android, confirmed persisted selections, completed the custom single-idol puja and verified one server-credit day.
- Visually inspected cutouts, empty backgrounds, model thumbnails, catalog controls, live preview and the saved shrine. This is a single-emulator smoke test, not a full device/performance/accessibility certification.
