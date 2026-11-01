<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:050505,35:171717,70:303030,100:050505&height=190&section=header&text=HeadScroll&fontSize=54&fontColor=ffffff&fontAlignY=38&desc=SCROLL%20REELS%20WITH%20A%20NOD&descAlignY=62&descSize=13" width="100%" />

<p align="center"><img src="docs/logo.svg" width="110" alt="HeadScroll"></p>

<br/>

<img src="https://img.shields.io/badge/ANDROID-111111?style=for-the-badge&logo=android&logoColor=white" /> <img src="https://img.shields.io/badge/KOTLIN-111111?style=for-the-badge&logo=kotlin&logoColor=white" /> <img src="https://img.shields.io/badge/MEDIAPIPE-111111?style=for-the-badge&logo=google&logoColor=white" /> <img src="https://img.shields.io/github/stars/TheROMZ52/headscroll?style=for-the-badge&color=111111&labelColor=050505&logo=github" /> <img src="https://img.shields.io/github/last-commit/TheROMZ52/headscroll?style=for-the-badge&color=111111&labelColor=050505" />

<br/>

### For the moments when your hands are busy but your feed must go on. Nod responsibly.

<img src="https://skillicons.dev/icons?i=kotlin,android,androidstudio,gradle,githubactions&theme=dark" />

</div>

---

An Android app for controlling short-form video feeds (Instagram Reels, TikTok, YouTube Shorts, and more) using **head movements** and **hand gestures**.

---

## 01 / DEFAULT GESTURES

| Gesture | Default action |
|---|---|
| Move head up | Next short video |
| Move head down | Previous short video |
| Move head right | Next horizontal swipe |
| Move head left | Previous horizontal swipe (optional) |
| OK hand sign | Like (optional) |
| Pinch | Play / pause (optional) |
| ✋ ✌️ 😄 | Optional gestures |

---

## 02 / TECHNOLOGY

- CameraX (front camera) + ML Kit Face Detection (head angle and smile detection)
- MediaPipe Hand Landmarker (hand gestures)
- AccessibilityService (swipes and taps), Foreground Service, and Quick Settings Tile

---

## 03 / BUILD THE APK

Push the project to GitHub, then open **Actions → Build APK**.

The hand model (`hand_landmarker.task`) is downloaded automatically during CI and bundled into the APK.

For a local build, manually place the model in `app/src/main/assets/`:

```text
https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
```

---

## 04 / INSTALL

1. Install the APK from [Releases](../../releases/tag/latest).
2. In the app's setup screen, enable Accessibility, camera, and notification permissions.
3. Tap **Start setup and run**; the remaining setup is automatic.
4. Open Instagram or another supported feed and move your head up or down.

---

<details>
<summary><b>05 / CALIBRATION</b></summary>
<br/>

Calibration is automatic. Your normal head position is continuously learned from the beginning. Optional manual fine-tuning is available under **Gestures → Advanced**.

Font: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) (OFL). See [CHANGELOG.md](CHANGELOG.md) for changes.

</details>

---

## 06 / SECURITY AND PRIVACY

- The APK is signed with a fixed release key. See the [Privacy Policy](PRIVACY.md).
- The app does not use internet access; camera and Accessibility access are used only for gesture control.
- If Play Protect displays a warning, choose **More details → Scan app**.

---

<div align="center">

<a href="https://github.com/TheROMZ52?tab=repositories"><img src="https://img.shields.io/badge/MORE%20PROJECTS-111111?style=for-the-badge&logo=github&logoColor=white" /></a>

<br/><br/>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:050505,50:202020,100:050505&height=110&section=footer" width="100%" />

<sub>© TheROMZ52 • Built with code, curiosity, and a suspicious number of tabs.</sub>

</div>
