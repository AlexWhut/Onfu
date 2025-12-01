Firebase Storage Emulator — quick start

This project can use the Firebase Storage Emulator for local development (no billing required).

Prerequisites
- Node.js and npm
- `firebase-tools` CLI
- Android emulator (or a device) for running the app

Install firebase-tools (if not installed):
```bash
npm install -g firebase-tools
```

Initialize emulators (only need to do once per repo):
```bash
# run from the project root
firebase init emulators
# Select Storage (and Firestore if you want). Default port for Storage emulator is 9199.
```

Start the Storage emulator:
```bash
firebase emulators:start --only storage
```

How to connect the Android app to the emulator
- For Android emulator (AVD): use host `10.0.2.2` and port `9199`.
- For a physical device, use your PC's LAN IP and ensure the device can reach the host.

This project already enables the emulator automatically in debug builds for the upload fragment. The code calls:
```kotlin
FirebaseStorage.getInstance().useEmulator("10.0.2.2", 9199)
```

How to test
1. Start the Storage emulator with `firebase emulators:start --only storage`.
2. Run the app on an Android emulator.
3. Use the upload UI (Profile -> Upload) to select and upload an image.
4. Check the emulator console (where `firebase emulators:start` is running) — it will show uploaded files, and Logcat in Android Studio for tag `STORAGE_DEBUG`.

Notes
- The emulator stores files locally only for the emulator session.
- Do not use the emulator for production; switch back to production Storage by removing or guarding the `useEmulator` call when ready.

Troubleshooting
- If uploads fail, check Logcat for `STORAGE_DEBUG` and also check the emulator console output.
- If using a physical device, replace `10.0.2.2` with the host machine IP and open port 9199 in firewall.

More information
- https://firebase.google.com/docs/emulator-suite
- https://firebase.google.com/docs/storage/emulator
