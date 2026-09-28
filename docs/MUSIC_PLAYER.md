# Curated bhajan player

Android uses one Media3 ExoPlayer and a native playlist for both the sheet and mini-player. Media3 1.5.1 retains compatibility with this project's Kotlin 1.9 toolchain. Audio focus, headphone unplug handling, seeking, retries, and buffered playback use the native engine. Playback pauses with the temple screen's lifecycle.

## Caching and loading

A single process-wide SimpleCache persists audio in the app cache directory with a 200 MiB LRU limit. SQLite initialization happens on IO. Cache storage errors fall back to upstream streaming. Audio uses bounded 8-second connect/read timeouts. Current playback buffers 15–45 seconds, with a 1-second start threshold and 2.5-second rebuffer threshold.

After playback begins, prefetch waits three seconds and checks the current buffer before warming up to 512 KiB of the next queued song. It yields on weak connections and cancels on pause, skip, repeat, track transition, or release. It never downloads the entire next song in advance. Repeated playback reads cached bytes; eviction and the OS can remove cache data, so this is not a guaranteed offline download library.

The catalog is persisted by backend URL and displayed from disk before refreshing. Conditional requests use ETags. Network failure retains the cached catalog. The Go service loads and serializes metadata once at startup and serves `GET /api/v2/music` with a one-hour cache policy and ETag. Set `MUSIC_CATALOG_PATH` to override the bundled JSON and restart after editing it. Provider searches and album lookups never run on the phone.

Playback diagnostics in Android Logcat (`BhajanPlayback`) record startup milliseconds, buffer milliseconds, track ID and error code; they contain no account data or audio URLs. A bounded recovery pass skips failed tracks while playback is requested, and stops after exhausting eligible tracks. Retry clears that failure history. Playback metrics are local diagnostics, not an uploaded analytics system.

## Ordering and duplicates

Shuffle is the initial default and the chosen mode is saved. Sequence starts with the lowest-numbered eligible recording for the temple idol, followed by catalog order. Original and Ganesh/Hanuman temples use Ganesh; Ram Darbar uses Rama, including Rama songs in the mixed collection. With no match for the idol, sequence uses catalog order. Selecting a thumbnail overrides the starting song. Repeat repeats the current song. Mode changes preserve paused state.

Identical audio URLs appear once in the browser and queue. Original list numbers remain in backend records and the verification report. Entries 81–100 retain the mixed collection and their individual deity tags. The supplied list starts at 31; no entries 1–30 were invented.

## Recording curation

`python3 scripts/prepare_music_catalog.py scripts/music_requests.json resolved.json` searches title/Hindi/transliteration aliases and mines devotional collection filenames. It rejects named sped-up, 8D, remix, ringtone, lecture, karaoke and repeated-loop versions. It probes bounded audio samples, decodes 20 seconds, and checks bitrate, sample rate and duration. Quality measurements and source provenance stay in the backend. Performer credits come from audio tags; uploader metadata is stored separately as `sourceCreator`.

Sample-derived gain only attenuates loud recordings, by at most 12 dB; it does not amplify quiet material. This is a consistency improvement, not full-track mastering or a complete listening review. MP3-labelled files whose sampled data fails decoding are rejected. Research artifacts stay in `/tmp/music-curation`. Review candidates before replacing the catalog; unresolved songs are listed in `MUSIC_CATALOG_VERIFICATION.md`.

## Verification

Run Python matching tests with `python3 scripts/test_music_catalog.py`, Android unit tests with `:app:testDebugUnitTest`, and backend tests with `go test ./...`.

For device cache tests, use `:app:connectedDebugAndroidTest -PisolatedVerification=true`. This installs a separate `.verification` app and preserves the installed release app's data. The tests verify replay and a prefetched prefix after the test HTTP origin goes offline.
