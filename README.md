[فارسی](README.md) · [English](README.en.md)

<p align="center"><img src="docs/logo.svg" width="128" alt="HeadScroll"></p>

# HeadScroll

برای وقت‌هایی که دستت درگیر پاپ‌کورنه ولی هنوز می‌خوای ریلز بعدی رو ببینی؛ سرت رو تکون بده، نه گوشی رو.

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

## فناوری
- CameraX (دوربین جلو) + ML Kit Face Detection (زاویه سر، لبخند)
- MediaPipe Hand Landmarker (ژست دست)
- AccessibilityService (سوایپ و تپ) + Foreground Service + Quick Tile

## ساخت APK
پروژه رو روی GitHub پوش کن، تب **Actions ← Build APK**. مدل دست (`hand_landmarker.task`) خودکار تو CI دانلود و داخل APK گذاشته میشه.
برای بیلد لوکال، مدل رو دستی بذار تو `app/src/main/assets/`:
`https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task`

## نصب
1. APK رو نصب کن (از [Releases](../../releases/tag/latest))
2. اپ ← کارت «راه‌اندازی»: Accessibility، دوربین و اعلان رو فعال کن
3. دکمه‌ی «🚀 راه‌اندازی و شروع» رو بزن، بقیه خودکاره
4. اینستا رو باز کن و سرتو بده بالا یا پایین

## کالیبراسیون
کاملاً خودکاره: حالت عادی سرت از همون لحظه‌ی اول و به‌صورت پیوسته یاد گرفته میشه. تنظیم دقیق دستی اختیاریه (تب ژست‌ها ← پیشرفته).

فونت: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) (OFL). تغییرات: [CHANGELOG.md](CHANGELOG.md)

## امنیت و حریم خصوصی
- APK با کلید release ثابت امضا میشه ([Privacy Policy](PRIVACY.md))
- اپ اینترنت نداره؛ دوربین و Accessibility فقط برای کنترل ژست‌ها استفاده میشن
- اگه Play Protect هشدار داد: More details ← Scan app
