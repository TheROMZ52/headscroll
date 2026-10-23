# HeadScroll

اپ اندروید: سر بالا = ریلز بعدی، سر پایین = ریلز قبلی (اینستاگرام).

- دوربین جلو + ML Kit Face Detection (زاویه سر)
- AccessibilityService برای زدن سوایپ
- Foreground Service برای کار در پس‌زمینه

## ساخت APK
پروژه رو روی GitHub پوش کن. تب Actions > Build APK > آرتیفکت `HeadScroll-apk`.
(یا لوکال: `gradle assembleDebug` با JDK 17 و Android SDK)

## نصب
1. APK رو نصب کن
2. اپ رو باز کن > «باز کردن تنظیمات دسترسی» > HeadScroll رو روشن کن
3. «روشن کردن» رو بزن و اجازه دوربین بده
4. اینستا رو باز کن و تو ریلز سرتو تکون بده
