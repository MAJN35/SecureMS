# پیام‌رسان پیامکی امن آفلاین (SecureSMS — Offline E2EE SMS Messenger)

<div dir="rtl">

## معرفی پروژه
**SecureSMS** یک پیام‌رسان پیامکی کاملاً آفلاین، بدون نیاز به اینترنت و بدون سرور میانی (Serverless) برای سیستم‌عامل اندروید است که امکان **گفتگوهای شخصی، گروه‌های امن پیامکی و کانال‌های پخش (Broadcast Channels)** را با رمزنگاری سرتاسری (End-to-End Encryption) بر بستر شبکه سلولار (SMS) فراهم می‌سازد.

این برنامه با بهره‌گیری از الگوریتم‌های استاندارد رمزنگاری **ECDH (منحنی NIST P-256)**، **HKDF-SHA256** و **AES-256-GCM** به همراه **موتور استتار متون رمزشده به الفبای فارسی (Base32)** طراحی شده است تا پیام‌های محرمانه بدون وابستگی به اینترنت به صورت امن مبادله شوند.

---

## ویژگی‌های کلیدی (Key Features)

### ۱. رمزنگاری سرتاسری آفلاین (Offline End-to-End Encryption)
- **تبادل کلید دیفی-هلمن (ECDH - secp256r1 / P-256):** تولید جفت‌کلید نامتقارن برای هر دستگاه و محاسبه کلید مشترک (Shared Secret) بدون ارسال کلید خصوصی روی شبکه.
- **مشتق‌سازی کلید (HKDF-SHA256):** استخراج کلید متقارن ۲۵۶ بیتی یکتا به ازای هر پیام با استفاده از شناسه پیام، شماره توالی (Sequence Number) و اثر انگشت مرتب‌شده طرفین.
- **رمزنگاری اصالت‌سنجی‌شده (AES-256-GCM):** استفاده از بردار مقداردهی اولیه تصادفی ۹۶ بیتی (`SecureRandom IV`) و تگ اصالت‌سنجی ۱۲۸ بیتی (`GCM Authentication Tag`) برای جلوگیری از هرگونه دستکاری، جعل یا حمله تکرار (Replay Attack).
- **ذخیره‌سازی سخت‌افزاری کلیدها:** حفاظت از کلید خصوصی هویت کاربر و پایگاه داده محلی به کمک `AndroidKeyStore` (`MasterKey` مبتنی بر AES-256-GCM).

### ۲. گفتگوهای شخصی، گروه‌های امن و کانال‌های پیامکی (Direct, Groups & Broadcast Channels)
- **گفتگوی شخصی (Direct):** ارتباط دوطرفه سرتاسر رمزنگاری‌شده یا پیامک استاندارد بین دو مخاطب.
- **گروه‌های امن پیامکی (SMS Group Chats):** ایجاد گروه با چندین عضو، مدیریت لیست اعضا و ارسال همزمان (Multi-recipient SMS Fan-out) بسته‌های رمزنگاری‌شده به تمامی اعضای گروه.
- **کانال‌های پخش پیامکی (Broadcast Channels):** انتشار یک‌طرفه اطلاعیه‌ها و پیام‌ها برای فهرست مشترکین با امکان تغییر نقش بین «مدیر کانال» (امکان انتشار) و «مشترک» (فقط خواندنی).

### ۳. پاسخ به پیام، واکنش‌های ایموجی و رسید خواندن اختیاری (Replies, Reactions & Per-Contact Seen Status)
- **پاسخ به پیام (Quoted Reply):** امکان انتخاب هر پیام در گفتگو، گروه یا کانال و ارسال پاسخ مستقیم همراه با پیش‌نمایش پیام مرجع و پرش سریع به پیام اصلی.
- **واکنش‌های سریع (Emoji Reactions):** ثبت و ارسال واکنش‌های ایموجی (`👍 ❤️ 😂 😮 😢 🙏 🔥`) روی پیام‌ها از طریق بسته‌های کنترلی فشرده پیامکی.
- **رسید «دیده شد» (Seen / Read Receipts) با کنترل اختصاصی هر مخاطب:** به صورت پیش‌فرض برای حفظ حریم خصوصی **غیرفعال (OFF)** است و کاربر می‌تواند آن را به صورت مجزا برای هر گفتگو یا از بخش تنظیمات فعال/غیرفعال کند.

### ۴. استتار فارسی و مدیریت پیام‌های حجیم (>1KB Chunking Engine)
- **کدگذاری حروف فارسی (Persian Base32 Camouflage):** تبدیل بایت‌های خروجی رمزنگاری به ۳۲ حرف الفبای فارسی (پیشوند `پر۱:`) یا اعداد و علائم فارسی (`پن۱:`) جهت جلوگیری از حساسیت فیلترهای اپراتوری نسبت به رشته‌های Base64 لاتین (`SSMS1:`).
- **قطعه بندی و تجمیع خودکار (Multi-Part SMS Fragmentation):** تقسیم خودکار پیام‌های طولانی (بیشتر از ۱ کیلوبایت) به قطعات استاندارد پیامکی همراه با هدر پروتکل ۲۹ بایتی، بررسی CRC16 و نمایش گرافیکی وضعیت ارسال و دریافت قطعات (`Chunk Stack`).

### ۵. تبادل کلید خارج از باند (Out-of-Band Key Exchange)
- اشتراک‌گذاری کلید عمومی بدون نیاز به ارسال پیامک از طریق **کد QR اختصاصی**، **بلوتوث**، **تلگرام / واتساپ** یا کلیپ‌بورد.
- نمایش **کد امنیتی ۶۰ رقمی (Safety Number)** و اثر انگشت SHA-256 برای احراز هویت حضوری یا تلفنی مخاطب و تشخیص تغییر کلید (`Identity Changed Warning`).

### ۶. رابط کاربری حرفه‌ای شیشه‌ای و فیزیک مایع (Professional Frosted Glass & Liquid Physics UI)
- طراحی مدرن و چشم‌نواز در دو پوسته **روشن (Nordic Slate & Sapphire)** و **تیره (Executive Obsidian & Emerald)** با هماهنگی کامل رنگ آیکون‌های ساعت، باتری و نوار وضعیت سیستم‌عامل اندروید (`Edge-to-Edge SystemBarStyle`).
- حباب‌های پیام مجهز به **فیزیک مایع (Liquid Touch & Scroll Physics)** با موج نوری تعاملی در نقطه لمس و انعطاف فنری نرم هنگام اسکرول.
- بدون هیچ‌گونه سایه مستطیلی سیاه یا سفید در زیر سطوح شیشه‌ای (`Custom Path Outline Glass Rendering`).

---

## ساختار بسته پروتکل پیامک (29-Byte Binary Envelope)

| فیلد (Field) | اندازه (Bytes) | توضیحات |
| :--- | :---: | :--- |
| `Magic` | `2` | بایت‌های جادویی پروتکل (`0x53, 0x50`) |
| `Version` | `1` | نسخه پروتکل (`0x01`) |
| `Flags` | `1` | بیت‌های کنترلی (نوع کدگذاری، فشرده‌سازی و کلید) |
| `Message ID` | `2` | شناسه تصادفی پیام برای تجمیع قطعات |
| `Fragment Index` | `1` | شماره قطعه فعلی (`0..N-1`) |
| `Fragment Total` | `1` | تعداد کل قطعات پیام (`N`) |
| `Sequence Number` | `4` | شماره ترتیبی ضدتکرار (Anti-Replay Counter) |
| `Sender Key Hash` | `3` | ۲۴ بیت اول هش SHA-256 کلید عمومی فرستنده |
| `GCM IV` | `12` | بردار تصادفی ۹۶ بیتی AES-GCM |
| `Payload Length` | `2` | طول بایت‌های رمزشده در قطعه فعلی |
| `Ciphertext + Tag` | `Variable` | متن رمزشده به همراه تگ اصالت‌سنجی ۱۲۸ بیتی GCM |

---

## راهنمای ساخت و خروجی APK (Build & Export)

برای کامپایل پروژه و تولید فایل نصبی اندروید (`APK`) در محیط خط فرمان:

```bash
# ساخت نسخه Debug و تولید خودکار فایل نصبی در پوشه export
gradle assembleDebug

# اجرای تست‌های واحد و تست‌های یکپارچه Robolectric
gradle :app:testDebugUnitTest
```

فایل‌های نصبی خروجی به صورت خودکار در مسیرهای زیر ذخیره می‌شوند:
- `export/securesms.apk`
- `export/app-debug.apk`
- `app/build/outputs/apk/debug/app-debug.apk`

</div>

---

# English Overview — SecureSMS (Offline E2EE SMS Messenger)

**SecureSMS** is a fully offline, serverless End-to-End Encrypted (E2EE) SMS messenger for Android written in Kotlin and Jetpack Compose. It operates strictly over cellular SMS without requiring internet connectivity or central servers.

### Highlights
- **End-to-End Cryptography:** ECDH key agreement over `secp256r1` (NIST P-256), per-message key derivation via `HKDF-SHA256`, and authenticated encryption using `AES-256-GCM` with 96-bit random IVs and 128-bit authentication tags.
- **Hardware-Backed Key Storage:** Identity keys and local message stores are protected with Android Keystore (`AES256_GCM` `MasterKey`).
- **Direct Chats, Secure Groups & Broadcast Channels:** Full support for 1-to-1 encrypted conversations, multi-member SMS group fan-out, and 1-to-many SMS broadcast channels.
- **Interactive Messaging:** Quoted message replies, emoji reactions (`👍 ❤️ 😂 😮 😢 🙏 🔥`), and optional per-contact Read Receipts (`Seen`, disabled by default for privacy).
- **Persian Base32 Ciphertext Camouflage:** Encodes binary envelopes into natural-looking Persian script (`پر۱:`), Persian numerals (`پن۱:`), or compact Base64 (`SSMS1:`).
- **Multi-Segment (>1KB) Fragmentation Engine:** Automatically splits and reassembles large payloads across multiple SMS PDUs with a 29-byte binary protocol header and replay protection.
- **Executive Frosted Glass & Liquid UI:** Refined Light (`Nordic Slate & Sapphire`) and Dark (`Obsidian Slate & Emerald`) themes with synchronized Android system status/navigation bar appearance (`SystemBarStyle`), vector outline glass rendering, and touch/scroll-reactive liquid message bubble physics.
