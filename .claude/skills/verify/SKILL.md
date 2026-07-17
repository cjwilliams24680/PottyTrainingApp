---
name: verify
description: Build, install, and drive the PottyTraining app on the Android emulator to verify changes at the UI surface.
---

# Verifying PottyTrainingApp changes
Details how to verify changes work on a local emulator. This can be slow so make sure to verify with the user that they want you to do this (rather than doing it themselves).

## Build & install

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
"$ADB" devices                          # expect an emulator (AVD: Pixel_10_Pro); boot with
                                        # "$HOME/Library/Android/sdk/emulator/emulator" -avd Pixel_10_Pro &
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n com.cjwilliams.pottytraining/.MainActivity
```

## Server dependency

Creating/editing/refreshing logs hits the GraphQL server; the app targets `http://10.0.2.2:3000/graphql` (host's `localhost:3000`). The server lives at `~/Development/PottyTrainingServer` (NestJS + docker-compose). Health check:

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST -H 'Content-Type: application/json' \
  -d '{"query":"{ __typename }"}' http://localhost:3000/graphql   # expect 200
```

A login session usually persists on the emulator; if the app opens on the Login screen you'll need credentials from the user.

## Driving & capturing

```bash
"$ADB" shell input tap X Y              # coordinates from a screenshot (screen is 1280x2856)
"$ADB" shell input keyevent 4           # system back
"$ADB" exec-out screencap -p > shot.png # then Read the png
```

Flows worth driving: Create Log form → Success → Continue → History; bottom-nav switching between all three tabs (watch selection state); History → tap a log → "Log Options" sheet → Edit Log → Update Log pops back to History.

Gotcha: History list entries open a bottom sheet (Edit/Delete), not the edit screen directly.
