# Somewhere to Nothing

A compass for the Nothing Phone (3) that points to the nearest physical Nothing store and
tells you how far away it is.

It lives in:

- **Home-screen widget**, in two sizes. 2×2: showing the direction and distance. 4×2: adds the store,
  whether it's open now, and the compass direction. On the widget, the top is north, like a map.
- **The app**: a full-screen compass whose arrow turns as you turn, with a button for directions.
  Tap the title at the top for a short note on what it all means.
- **Glyph Toy**: on the Glyph Matrix on the back. A block of dots on the edge points to the
  store and pulses faster as you get closer. Within 2 km a needle joins it to the centre.
  Long-press the Glyph Button to see the distance in numbers.

When you're within 50 m, it says **YOU'RE SOMEWHERE**.

## Getting the app onto your phone

1. On your phone, open **Releases → Latest** on this repo's GitHub page.
2. Tap **somewhere-to-nothing.apk** to download it.
3. Open the download and tap **Install** (allow your browser to install apps the first time).

Then:

- Open **Somewhere to Nothing** once and allow location.
- Add the widget: long-press the home screen → **Widgets** → **Somewhere to Nothing**.
- Pick the Glyph Toy: **Settings → Glyph Interface → Glyph Toys**.
- Optional: tap **LET THE WIDGET UPDATE ON ITS OWN** and choose **Allow all the time**, so the
  widget keeps up as you move without opening the app.

## How it works

| File | What it does |
|---|---|
| `Stores.kt` | The Nothing stores: where they are and when they're open. Add new stores here. |
| `Geo.kt` | Distance and direction to the nearest store, and how the distance is written. |
| `Where.kt` | Finds and remembers where the phone is. |
| `Heading.kt` | Which way the phone is pointing (compass sensor). |
| `Dots.kt` | The dot-matrix numbers (single-dot decimal point, three-dot comma) and the Glyph Matrix shape. |
| `Art.kt` | Draws the compass, numbers and labels, for the widget and the app. |
| `CompassWidget.kt` | The home screen widgets. |
| `CompassActivity.kt` / `CompassView.kt` | The full-screen compass, and the About card you get by tapping the title. |
| `AboutViews.kt` | The dotted "One job." heading and little arrow on the About card. |
| `MatrixArt.kt` / `CompassToyService.kt` | The Glyph Toy. |

The Glyph Matrix SDK (`app/libs/glyph-matrix-sdk-2.0.aar`) is from
[Nothing-Developer-Programme/GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) (MIT).
Space Mono is by Colophon Foundry, under the SIL Open Font Licence (`app/src/main/assets/SpaceMono-OFL.txt`).
This is a fan project and isn't made or endorsed by Nothing.
