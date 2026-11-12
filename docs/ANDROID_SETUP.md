# Android setup (first time)

## 1. Install Android Studio
Download it from https://developer.android.com/studio and install with the default options. It bundles the JDK and the Android SDK tools you need.

## 2. Open the project
1. Start Android Studio and choose **Open**.
2. Select the `aqi-india` folder (the one containing `settings.gradle.kts`).
3. Wait for **Gradle sync** to finish. The first sync downloads a lot and can take several minutes.
4. If Studio asks to install an SDK platform or build tools, accept.
5. If sync complains about versions, use the suggestion Studio offers (Android Gradle Plugin upgrade assistant). Versions in `gradle/libs.versions.toml` were chosen from memory and are the first thing to update.

## 3. Run the tests
In the terminal at the bottom of Studio:

```
./gradlew :core:test
./gradlew :app:testDebugUnitTest
```

## 4. Run the app
**On your phone (recommended):**
1. Settings > About phone > tap **Build number** 7 times.
2. Settings > Developer options > turn on **USB debugging**.
3. Connect by USB and tap **Allow** on the phone.
4. Pick your phone in the device list at the top of Studio and press the green **Run** button.

**On an emulator:** Tools > Device Manager > Create device, choose a Pixel with a recent system image, then press Run.

## 5. Add the widget
Long-press the home screen, open **Widgets**, find **AQI India**, and drag it out. The first refresh happens within a few seconds if the phone is online.

## Before publishing to Play
- Bump `compileSdk` and `targetSdk` in `app/build.gradle.kts` to the level Play currently requires.
- Rename the app (`app/src/main/res/values/strings.xml`) and replace the placeholder icon.
- Create a release signing key and keep it safe (never commit it).
- Prepare a privacy policy URL and the Data safety form. The app collects no personal data; location stays on the device and is used only to request air quality for that area.
- New personal developer accounts must run a closed test with a minimum number of testers before applying for production. Check Play Console for the current numbers and start recruiting testers early.
