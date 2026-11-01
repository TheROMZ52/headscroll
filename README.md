<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:050505,35:171717,70:303030,100:050505&height=190&section=header&text=HeadScroll&fontSize=54&fontColor=ffffff&fontAlignY=38&desc=SCROLL%20REELS%20WITH%20A%20NOD&descAlignY=62&descSize=13" width="100%" />

<p align="center"><img src="docs/logo.svg" width="110" alt="HeadScroll"></p>

<br/>

<img src="https://img.shields.io/badge/ANDROID-111111?style=for-the-badge&logo=android&logoColor=white" /> <img src="https://img.shields.io/badge/KOTLIN-111111?style=for-the-badge&logo=kotlin&logoColor=white" /> <img src="https://img.shields.io/badge/MEDIAPIPE-111111?style=for-the-badge&logo=google&logoColor=white" /> <img src="https://img.shields.io/github/stars/TheROMZ52/headscroll?style=for-the-badge&color=111111&labelColor=050505&logo=github" /> <img src="https://img.shields.io/github/last-commit/TheROMZ52/headscroll?style=for-the-badge&color=111111&labelColor=050505" />

<br/>

[فارسی](README.md) · [English](README.en.md)

### برای وقت‌هایی که دستت درگیر پاپ‌کورنه ولی هنوز می‌خوای ریلز بعدی رو ببینی؛ سرت رو تکون بده، نه گوشی رو.

<img src="https://skillicons.dev/icons?i=kotlin,android,androidstudio,gradle,githubactions&theme=dark" />

</div>

---

اپ اندروید برای کنترل ریلز (اینستاگرام، تیک‌تاک، یوتیوب Shorts…) با **حرکت سر** و **ژست دست**.

| ژست | پیش‌فرض |
|---|---|
| ⬆️ سر بالا | ریلز بعدی |
| ⬇️ سر پایین | ریلز قبلی |
| ➡️ سر به راست | سوایپ افقی بعدی |
| ⬅️ سر به چپ | (اختیاری) سوایپ افقی قبلی |
| 👌 علامت OK | (اختیاری) لایک |
| 🤏 پینچ | (اختیاری) پخش / توقف |
| ✋ ✌️ 😄 | اختیاری |

---

## 01 / فناوری

- CameraX (دوربین جلو) + ML Kit Face Detection (زاویه سر، لبخند)
- MediaPipe Hand Landmarker (ژست دست)
- AccessibilityService (سوایپ و تپ) + Foreground Service + Quick Tile

---

## 02 / ساخت APK

پروژه رو روی GitHub پوش کن، تب **Actions ← Build APK**. مدل دست (`hand_landmarker.task`) خودکار تو CI دانلود و داخل APK گذاشته میشه.
برای بیلد لوکال، مدل رو دستی بذار تو `app/src/main/assets/`:
`https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task`

---

## 03 / نصب

1. APK رو نصب کن (از [Releases](../../releases/tag/latest))
2. اپ ← کارت «راه‌اندازی»: Accessibility، دوربین و اعلان رو فعال کن
3. دکمه‌ی «🚀 راه‌اندازی و شروع» رو بزن، بقیه خودکاره
4. اینستا رو باز کن و سرتو بده بالا یا پایین

---

<details>
<summary><b>04 / کالیبراسیون</b></summary>
<br/>

کاملاً خودکاره: حالت عادی سرت از همون لحظه‌ی اول و به‌صورت پیوسته یاد گرفته میشه. تنظیم دقیق دستی اختیاریه (تب ژست‌ها ← پیشرفته).

فونت: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) (OFL). تغییرات: [CHANGELOG.md](CHANGELOG.md)

</details>

---

## 05 / امنیت و حریم خصوصی

- APK با کلید release ثابت امضا میشه ([Privacy Policy](PRIVACY.md))
- اپ اینترنت نداره؛ دوربین و Accessibility فقط برای کنترل ژست‌ها استفاده میشن
- اگه Play Protect هشدار داد: More details ← Scan app

---

<div align="center">

<a href="https://github.com/TheROMZ52?tab=repositories"><img src="https://img.shields.io/badge/MORE%20PROJECTS-111111?style=for-the-badge&logo=github&logoColor=white" /></a>

<br/><br/>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:050505,50:202020,100:050505&height=110&section=footer" width="100%" />

<sub>© TheROMZ52 • ساخته‌شده با کد، کنجکاوی و تعداد مشکوکی تب باز.</sub>

</div>
