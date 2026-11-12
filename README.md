# AQI India (working title)

A no-nonsense air quality app for India. One number, one colour, on your home screen.

- **Widget first.** The Android widget shows the AQI, its category, the place and how fresh it is. Nothing else.
- **India's scale by default.** Shows the CPCB National AQI, with a US AQI toggle. The two scales give different numbers for the same air, so the app always labels which one you are looking at.
- **Sun, too.** Today's peak UV and plain sunscreen guidance, from the same data source.
- **Open source, no ads, no account, no tracking.** Coarse location only.

> Status: v0.1 skeleton. See [Known limits](#known-limits) before relying on the numbers.

## Where the numbers come from

v0.1 uses **modelled** air quality from [Open-Meteo](https://open-meteo.com) (CAMS atmospheric model), not readings from a ground monitor. The app computes India's AQI itself from the hourly pollutant values, following the CPCB method:

- PM2.5, PM10, NO₂ and SO₂ use a 24-hour mean; O₃ and CO use an 8-hour mean.
- The overall AQI is the worst sub-index. At least 3 pollutants are needed, one of them PM2.5 or PM10.
- Breakpoints are from the CPCB National AQI table and are checked in `core/src/test`.

Station data (CPCB through OpenAQ or similar) is planned for v0.2. See [docs/DECISIONS.md](docs/DECISIONS.md).

Data by [Open-Meteo.com](https://open-meteo.com) (CC BY 4.0), based on Copernicus Atmosphere Monitoring Service data.

## Known limits

- **Modelled, not measured.** Model values can differ noticeably from a monitor on your street, in either direction. The app says "Modelled estimate" on screen for this reason.
- **NH₃ and Pb are not included** (no hourly source), so the AQI can be lower than CPCB's official figure when those dominate. This is rare.
- **The "Severe" band top is an assumption.** CPCB leaves it open-ended, so the AQI is capped at 500 using a documented rule.
- **The widget uses the last location the app saw.** Open the app after travelling to update it. This avoids the background-location permission.
- **Open-Meteo's free tier is for non-commercial use.** Fine for a free open-source app; check their terms before adding ads or a paid tier.
- **Sunscreen guidance is general**, based on UV index bands. It is not medical advice.

## Build and run

You need Android Studio. Step-by-step setup is in [docs/ANDROID_SETUP.md](docs/ANDROID_SETUP.md).

```
./gradlew :core:test            # AQI maths and sunscreen logic (plain JVM, fast)
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug    # debug APK in app/build/outputs/apk/debug/
```

## Layout

```
core/   Pure Kotlin: CPCB AQI calculation, categories, sunscreen advice. No Android code, fully unit tested.
app/    Android app: Compose screen, Glance widget, WorkManager refresh, Open-Meteo client.
docs/   PRD, widget states, decision log, Android setup, AI-assisted work log.
```

## Roadmap

1. v0.1: modelled AQI + UV, widget, CPCB/US toggle, 24 cities.
2. v0.2: ground-station data with source and distance shown, Hindi and Tamil.
3. v0.3: dark theme polish, accessibility audit, F-Droid listing.

## Contributing

Issues and pull requests are welcome. Please keep the scope small: this app deliberately has no maps, news feeds, graphs or social features.

## License

MIT. See [LICENSE](LICENSE).
