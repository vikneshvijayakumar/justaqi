# AI-assisted work log

An honest record of what was done with AI, what was checked, and what was changed by hand. For the case study: the corrections are the interesting part, so log them.

## Entry template
- **Date and task**
- **What I asked for**
- **What the AI produced**
- **What I rejected or changed, and why**
- **How it was verified**

---

## 2026-11-12: Project skeleton
**Asked for:** a starting project for a minimal India AQI app with a widget, using the decisions in DECISIONS.md.

**AI produced:** repo structure, the CPCB AQI calculator with tests, the Open-Meteo client and parser, a Compose screen, a Glance widget, a WorkManager refresh job, and these docs.

**Verified:**
- CPCB breakpoints were checked against the table on ies.gov.in, including its two worked PM2.5 examples (45 gives 75, 150 gives 323).
- The expected numbers in the unit tests were re-derived with a separate Python script and matched.

**Not verified (do this first):**
- Nothing was compiled or run. The build environment could not download Gradle or Android dependencies, so none of the Kotlin has been built, and the unit tests have not been executed.
- Library versions in `gradle/libs.versions.toml` were chosen from memory and may need updating.
- Widget rendering has not been seen on a device.

**Needs a human decision:** app name, icon, final colour palette (contrast check), copy tone, licence choice.
