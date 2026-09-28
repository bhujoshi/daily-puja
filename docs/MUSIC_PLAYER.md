# Curated bhajan player

The Android player loads `GET /api/v2/music` once per player session. The Go service embeds `backend/internal/music/catalog.json`, serializes it once at startup, and serves it with a one-hour cache policy and an ETag. Set `MUSIC_CATALOG_PATH` to a mounted JSON file to override the bundled catalog; restart the service after editing it.

Song titles, original list numbers, collection, deity, artwork asset paths, artist credits, source pages, and direct HTTPS MP3 URLs live in the backend. The importer queries Internet Archive only during catalog preparation. Playback never performs provider searches or album lookups. Audio streams directly from Archive. Thumbnail images reuse packaged idol artwork, decode off the main thread, and use a bounded memory cache.

Shuffle is the initial default and the chosen mode is saved on the device. A shuffle cycle contains each catalog entry once. Sequence mode places the lowest-numbered available song for the temple idol first, followed by the remaining songs in their original list order. Original and Ganesh/Hanuman temples use Ganesh first; Ram Darbar uses Rama, including Rama-tagged songs in the mixed collection. If no recording matches the idol, sequence falls back to catalog order. Selecting a thumbnail explicitly starts that song. Repeat repeats the current song.

## Updating recordings

Run `python3 scripts/prepare_music_catalog.py requested.json resolved.json` with a catalog-shaped input. It searches metadata, matches song titles, filters lectures/ringtones/short clips, and probes audio bytes before retaining a recording. Review the matches before replacing the catalog. Missing matches are omitted rather than shown as unplayable cards. An omitted song means no matching accessible recording was verified by this import, not that no recording exists anywhere on Archive. Archive availability can change; playback failures expose retry and next controls.

The supplied list begins at 31. No songs numbered 1–30 were invented. Entries 81–100 remain in the mixed collection while retaining individual deity tags. Repeated titles in the supplied list retain their original numbers.

Deploy the backend endpoint before releasing the Android build. Configure `accountApiUrl` to that backend. For an emulator with a local backend on port 18080, build with `-PaccountApiUrl=http://10.0.2.2:18080`.
