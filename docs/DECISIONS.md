# Decision log

Newest first. Each entry: what was decided, why, and what it costs.

## 2026-11-12: Computing the Severe band top
CPCB defines Severe as open-ended (for example PM2.5 "250+"). We assume the Severe band is as wide as the Very poor band and clamp the AQI at 500. For PM2.5 that gives 250 to 380, for PM10 430 to 510. This is **our assumption, not an official CPCB value**. It only affects readings above the Very poor band.

## 2026-11-12: Modelled data first, stations later
Open-Meteo needs no API key and works for any coordinates, which keeps v0.1 small. The cost is accuracy: it is a model, not a monitor, and the app says so on screen. v0.2 adds station readings (OpenAQ or similar) with the station name and distance shown.

## 2026-11-12: No backend in v0.1
The app calls Open-Meteo directly. There is no key to protect and nothing to host, so a proxy would add cost and moving parts for no benefit yet. Revisit when adding a provider that needs a key. Open-Meteo's free tier is non-commercial, which is fine for a free open-source app.

## 2026-11-12: CPCB computed on the device
Open-Meteo returns US AQI but not India's. The app computes CPCB AQI from hourly pollutant data, using 24-hour means for PM2.5, PM10, NO₂, SO₂ and 8-hour means for O₃ and CO, plus CPCB's validity rules. The logic lives in the `core` module with unit tests. NH₃ and Pb are excluded for lack of an hourly source.

## 2026-11-12: Location without background permission
Coarse location only. The widget refreshes in the background using the last location the app saw, so no background-location permission is needed. This keeps the Play review simple and the privacy story short. Trade-off: after travelling, the user must open the app once.

## 2026-11-12: Default scale is CPCB, US AQI is a toggle
Indian news and government sources quote CPCB. Showing a different default would make the app look wrong next to what people read. The toggle covers people who compare with international sources. Whichever scale is on is always labelled.

## 2026-11-12: Native Kotlin, Jetpack Compose and Glance
Widget quality matters most here, and Glance is Google's current widget toolkit. Native keeps the APK small and suits F-Droid. Cross-platform options would still need native code for the widget.

## 2026-11-12: Sunscreen advice uses today's peak UV
At 7 am the current UV is near zero but the day's peak is what matters for planning. The screen shows the peak and the hours when UV is 3 or above.

## 2026-11-12: Licence
MIT, with the copyright holder set to Viknesh Vijayakumar. Change before publishing if you prefer Apache 2.0.
