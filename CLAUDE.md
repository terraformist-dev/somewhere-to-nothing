# Somewhere to Nothing: notes for Claude

A compass that points to the nearest physical Nothing store and says how far away it is.
Three places: a home-screen widget (2×2 and 4×2), a full-screen compass (the app), and a
Glyph Toy on the Phone (3)'s Glyph Matrix.

Owner has no coding experience and works only from a phone. Explain changes in plain English,
never ask them to run commands, and always finish by pushing to `main` and confirming the
GitHub Actions build went green (the build publishes `somewhere-to-nothing.apk` as the latest release).
The owner also has Glyph Pet (`terraformist-dev/glyph-pet`); this project copies its setup.

## Project

Android app (Kotlin, no AndroidX, no Compose), package `dev.terraformist.somewhere`,
minSdk 34, target Phone (3) on Nothing OS. Builds only in GitHub Actions
(`.github/workflows/build.yml`); the sandbox cannot reach Google's Maven or Gradle servers.

The debug build is signed with `app/somewhere.keystore` (committed on purpose) so updates
install over the old app. Don't change the key or applicationId.

## Stores

`Stores.kt` lists Nothing-owned stores only (no Best Buy, Kith, pop-ups or collaborations).
As of October 2026: Soho, London (opened 2022) and Indiranagar, Bengaluru (opened 14 Feb 2026).
New York and Tokyo were announced in January 2026 with no dates: add them when they open,
with coordinates and hours from their Google Maps listing. Soho's own web page gives two
different weekday hours (11–18 and 10–19); the code uses 11–18, which matches Google Maps.

## Design (agreed with the owner from a mockup)

- Nothing look: black, `#1A1A1A` widget cards, white dots, faint "ghost" dots, one red accent
  (`#D71921`) which is only ever the store. Labels in Space Mono (`assets/`, OFL licence).
- Numbers are drawn as dots (`DotFont.LARGE` 5×7, `DotFont.SMALL` 3×5 for the matrix).
  **The decimal point is one dot. The comma is three dots swooping down to the left.**
  The owner asked for exactly this; don't swap in a font.
- `Art.kt` draws the compass, numbers and labels for both the widget and the full-screen view.
  The compass geometry is a 120-unit box: ring of 48 dots at r54, ghost disc r44, 15-dot arrow,
  red store dot at the top of the ring.
- Distance format (`Geo.format`): metres under 1 km (to 10 m), one decimal under 10 km,
  whole km with commas after. Arrived = within 50 m ("YOU'RE SOMEWHERE"). Over 100 km,
  opening hours and bearing are hidden ("NEAREST STORE").

## How each surface behaves

- **Widget**: north is up (white dot at top of ring), because a widget can't follow the phone
  turning. Refreshes every 30 min, when the app closes, and with "Allow all the time" location
  via `Where.followInBackground` (PendingIntent to `CompassWidget.Small`, action `MOVED`).
  The whole widget is one bitmap drawn at the widget's real size; layout switches at aspect 1.6.
- **Full screen** (`CompassActivity` + `CompassView`): live heading from `Heading`
  (rotation vector + magnetic declination). Arrow = bearing − heading; white ring dot = north.
  Directions button opens Google Maps (walking if under 5 km).
- **Glyph Toy** (`CompassToyService`, `MatrixArt`): edge block points to the store, live for
  60 s after a Glyph Button touch or while the screen is on, then a still frame. Needle from the
  centre within 2 km. Pulse speeds up with closeness (`MatrixArt.periodFor`: 3 s far → 0.6 s near).
  Long-press shows the distance for 4 s. `Facing.BACK`: held upright, "up" means the way you face.

## Glyph Matrix SDK 2.0 (same .aar as Glyph Pet, verified there)

- `GlyphMatrixManager.getInstance(ctx)`, `init(Callback)`, `register(Glyph.DEVICE_23112)`,
  `setMatrixFrame(IntArray)`, `turnOff()`, `unInit()`.
- Raw frames are 625 ints (25×25, row-major), **0..4095** per LED.
- `GlyphToy` constants: `MSG_GLYPH_TOY`, `MSG_GLYPH_TOY_DATA`, `EVENT_CHANGE` (long press),
  `EVENT_AOD` (once a minute when AOD toy), `EVENT_ACTION_DOWN`, `EVENT_ACTION_UP`.
- Matrix shape: row indents `9,7,5,4,3,2,2,1,1,0,0,0,0,0,0,0,1,1,2,2,3,4,5,7,9` (`GlyphShape`).
- Toy service only lives while selected; return `Messenger.binder` from `onBind`.

## Permissions

Location is asked for on first open; everything must still work if refused (shows "TAP TO START").
"Allow all the time" is optional and only offered by a small text button while it's missing.
Picker previews and the launcher icon are PNGs in `res/drawable-nodpi/` (drawn to match `Art.kt`).
