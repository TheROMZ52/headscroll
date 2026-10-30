<p align="center"><img src="docs/logo.svg" width="128" alt="HeadScroll"></p>

# HeadScroll

For the moments when your hands are busy but your feed must go on. Nod responsibly.

An Android app for controlling short-form video feeds (Instagram Reels, TikTok, YouTube Shorts, and more) using **head movements** and **hand gestures**.

## Default gestures

| Gesture | Default action |
|---|---|
| Move head up | Next short video |
| Move head down | Previous short video |
| Move head right | Next horizontal swipe |
| Move head left | Previous horizontal swipe (optional) |
| OK hand sign | Like (optional) |
| Pinch | Play / pause (optional) |
| ✋ ✌️ 😄 | Optional gestures |

## Technology

- CameraX (front camera) + ML Kit Face Detection (head angle and smile detection)
- MediaPipe Hand Landmarker (hand gestures)
- AccessibilityService (swipes and taps), Foreground Service, and Quick Settings Tile

## Build the APK

Push the project to GitHub, then open **Actions → Build APK**.

The hand model (`hand_landmarker.task`) is downloaded automatically during CI and bundled into the APK.

For a local build, manually place the model in `app/src/main/assets/`:

```text
https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
```

## Install

1. Install the APK from [Releases](../../releases/tag/latest).
2. In the app's setup screen, enable Accessibility, camera, and notification permissions.
3. Tap **Start setup and run**; the remaining setup is automatic.
4. Open Instagram or another supported feed and move your head up or down.

## Calibration

Calibration is automatic. Your normal head position is continuously learned from the beginning. Optional manual fine-tuning is available under **Gestures → Advanced**.

Font: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) (OFL). See [CHANGELOG.md](CHANGELOG.md) for changes.

## Security and privacy

- The APK is signed with a fixed release key. See the [Privacy Policy](PRIVACY.md).
- The app does not use internet access; camera and Accessibility access are used only for gesture control.
- If Play Protect displays a warning, choose **More details → Scan app**.
