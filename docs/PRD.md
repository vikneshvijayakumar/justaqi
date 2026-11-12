# PRD: AQI India (working title)

## Problem
Most AQI apps are crowded: maps, news, ads, five competing numbers. Someone in Delhi in November wants one answer: is the air bad right now, and should I do anything about it? The existing widgets are either buried in feature-heavy apps or do not refresh reliably.

## Who it is for
People in Indian cities who check air quality daily in the smog season, often on mid-range Android phones. Secondary: parents and people with asthma who need a quick yes/no.

## Principles
1. **One number first.** Everything else is secondary to the AQI on the home screen.
2. **Honest about the data.** Always show which scale, where the data comes from, and how old it is.
3. **No clutter.** No ads, no account, no feeds.
4. **Colour is never the only signal.** Number and label always sit with it.

## MVP scope (v0.1)
- Home-screen widget (2x2, resizable): AQI number, category, place, last updated.
- App screen: same number large, main pollutant, scale toggle (CPCB / US), place picker, refresh.
- Sunscreen guidance from today's peak UV, with the hours when UV is 3 or above.
- Coarse location, or pick from 24 cities. Works offline with the last known value, marked as old.

## Deliberately cut
Maps, historical graphs, forecasts, news, push alerts, accounts, social sharing, purifier shopping. Each one is a candidate for later only if it earns its place.

## Widget states
| State | What shows | Notes |
|---|---|---|
| No data yet | "Tap to set up" on a neutral background | First launch, before the first fetch |
| Fresh | Number, category, "Place · 12 min ago" on the band colour | Data under 2 hours old |
| Old | Number, category, "Old data · 5 h ago" | Data over 2 hours old; number still shown, age stated plainly |
| Scale has no value | Falls back to "Tap to set up" | Rare; for example the US value missing |

Still to design: offline and location-denied treatments inside the app, a dark-mode variant, a 4x1 layout.

## Success (for the case study)
- Published on Play and the repo is public.
- A before/after story of the widget design iterations.
- Real feedback from at least 10 users during the winter smog season.
