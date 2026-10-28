# Nối app Food Map VN với Firebase

Chưa có `app/google-services.json` thì app chạy bằng **dữ liệu giả** (FakeRepositories).
Có file đó thì `AppContainer` tự chuyển sang Firebase, không phải sửa code.

## Các file "lược đồ" của Firebase (tương đương file .sql)

| File | Vai trò | Tương đương SQL |
|---|---|---|
| `firebase/firestore.rules` | Phân quyền + kiểm tra kiểu, trường bắt buộc, giá trị hợp lệ, khoá ngoại | CREATE TABLE (kiểu cột, NOT NULL, CHECK, FOREIGN KEY) + GRANT |
| `firebase/firestore.indexes.json` | Khai báo index phức hợp (hiện không cần) | CREATE INDEX |
| `app/src/debug/assets/seed-data.json` | Dữ liệu mẫu (chỉ có trong bản debug) | INSERT INTO |
| Tài liệu đặc tả mục 8 | Collection, Document ID, từ điển dữ liệu, truy vấn | Thiết kế CSDL |

## 1. Tạo project và nối app
1. https://console.firebase.google.com → **Create a project** → `foodmapvn` (gói Spark, không cần thẻ).
2. **Add app → Android**, package name `com.example.foodmapvn`.
3. Tải **google-services.json** → chép vào thư mục `app/`. Bỏ qua bước "Add Firebase SDK" (đã có sẵn).
4. Android Studio → **Sync Project with Gradle Files** → Run.

## 2. Bật dịch vụ
- **Authentication → Sign-in method → Email/Password → Enable** (không bật Email link).
- **Firestore Database → Create database**: Standard, `(default)`, vị trí `asia-southeast1`, production mode.
- **Firestore → Rules**: dán toàn bộ `firebase/firestore.rules` → **Publish**.
  Mỗi lần sửa file rules trong dự án phải dán lại và Publish.

## 3. Nạp dữ liệu mẫu (không cần nhập tay)
1. Chạy app bản debug, vào tab **Hồ sơ → Đăng nhập/Đăng ký** để tạo tài khoản.
2. Firestore Console → `users` → mở document của bạn → sửa `role` từ `user` thành `admin`.
   (Security Rules chỉ cho Admin ghi vùng, món, quán.)
3. Trong app: tab **Hồ sơ → "Nạp dữ liệu mẫu (debug, cần role admin)"**.
   App ghi 3 vùng, 7 món, 8 quán, cấu hình, và dữ liệu demo của chính bạn
   (món muốn ăn, quán đã lưu, huy hiệu, thông báo, 2 đánh giá). Bấm lại nhiều lần không bị nhân đôi.
4. Muốn đổi dữ liệu mẫu: sửa `app/src/debug/assets/seed-data.json` (đúng tên trường và kiểu theo mục 8.2), chạy lại app, bấm nạp lại.

Quy ước trong seed-data.json: `{"_geo": [vĩ độ, kinh độ]}` = geopoint, `{"_serverTimestamp": true}` = thời điểm ghi,
`{"_date": "yyyy-MM-dd"}` = timestamp, khoá bắt đầu bằng `_` (như `_readme`) bị bỏ qua.

## 4. Cloudinary (lưu ảnh, miễn phí)

Gói Free, **không cần thẻ**. App tải ảnh thẳng lên Cloudinary bằng *unsigned upload preset*, Firestore chỉ lưu URL.
1. Đăng ký tại https://cloudinary.com (Sign up for free) → vào **Dashboard**, chép **Cloud name**.
2. ⚙ **Settings → Upload → Upload presets → Add upload preset**:
   - **Signing mode: Unsigned**
   - **Folder**: `foodmapvn`
   - (nên đặt) **Allowed formats**: `jpg, png, webp`; **Max file size**: 5 MB
   - Save, chép **tên preset** (vd. `foodmapvn_unsigned`).
3. Điền vào `app/src/main/res/values/cloudinary.xml`: `cloudinary_cloud_name`, `cloudinary_upload_preset`.
   **Không đặt API Secret của Cloudinary trong app** — unsigned preset không cần.
4. Ảnh đại diện được app thu nhỏ còn ~512 px trước khi tải lên (ImageCompressor), nên rất nhẹ.

## Gỡ lỗi (Logcat)
- Tag `FoodMapData`: document có trường **sai kiểu** (app bỏ qua trường đó thay vì crash) – sửa lại trong Console.
- Tag `FoodMapFirestore`: truy vấn bị lỗi, thường do Rules chặn (`PERMISSION_DENIED`).
- Tag `FoodMap`: "đang dùng dữ liệu giả" = app chưa thấy google-services.json.
- Nạp dữ liệu báo `PERMISSION_DENIED`: tài khoản chưa là admin, hoặc Rules chưa Publish bản mới.

## Đăng nhập bên thứ ba: Google, Facebook, số điện thoại

Code đã có sẵn (màn Đăng nhập → 3 nút). Mỗi cách cần bật trên Firebase Console một lần.
Sau khi đổi cấu hình trên Console: tải lại `google-services.json`, chép đè vào `app/`, Sync rồi Run.

### 0. Thêm SHA-1 của máy (bắt buộc cho Google và số điện thoại)
1. Lấy SHA-1 của khoá debug (chọn 1 cách):
   - Tab **Gradle** → nút **Execute Gradle Task** (ô vuông có ▶) → gõ `signingReport` → Enter.
   - Tab **Terminal**: `.\gradlew signingReport`
   - PowerShell (nhanh nhất):
     `& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android`

   Chép dòng `SHA1:` (của `Variant: debug` nếu dùng signingReport).
2. Firebase Console → ⚙ **Project settings** → **Your apps** → app Android → **Add fingerprint** → dán SHA-1.
   Mỗi thành viên build trên máy riêng có khoá debug riêng → **ai cũng phải thêm SHA-1 của mình**.

### 1. Google
1. **Authentication → Sign-in method → Add new provider → Google** → Enable → chọn email hỗ trợ → Save.
2. Tải lại `google-services.json` (lúc này file có Web client ID; plugin tự sinh `default_web_client_id`).
3. Máy ảo phải có Google Play và đã đăng nhập một tài khoản Google (Settings → Passwords & accounts).

### 2. Facebook
1. Vào https://developers.facebook.com → bấm **Bắt đầu** (góc trên phải) → đăng nhập Facebook →
   làm theo các bước **đăng ký nhà phát triển** (xác minh email/SĐT, đồng ý điều khoản). Chỉ làm 1 lần.
2. Sau khi đăng ký, góc trên có mục **Ứng dụng của tôi** (My Apps) → **Tạo ứng dụng** (Create App):
   - Tên ứng dụng: `Food Map VN`, email liên hệ → Tiếp.
   - Trường hợp sử dụng: chọn **Xác thực và yêu cầu dữ liệu từ người dùng bằng Đăng nhập Facebook**
     (Authenticate and request data from users with Facebook Login) → Tiếp.
   - Doanh nghiệp: chọn **Tôi chưa muốn kết nối danh mục doanh nghiệp** → Tiếp → **Tạo ứng dụng** (có thể phải nhập lại mật khẩu).
3. Lấy thông tin trong **Cài đặt ứng dụng → Thông tin cơ bản** (App settings → Basic):
   **ID ứng dụng** (App ID) và **Khoá bí mật** (App Secret, bấm Hiển thị).
   **Cài đặt ứng dụng → Nâng cao** (Advanced) → **Mã ứng dụng khách** (Client token).
4. Cũng ở **Thông tin cơ bản** → kéo xuống cuối → **+ Thêm nền tảng** → **Android**:
   - Tên gói (Package name): `com.example.foodmapvn`
   - Tên lớp (Class name): `com.example.foodmapvn.MainActivity`
   - **Key hash**: đổi SHA-1 ở bước 0 sang Base64 bằng PowerShell:
     `$sha1 = "AA:BB:...dán SHA-1..."; [Convert]::ToBase64String(($sha1 -split ':' | % { [Convert]::ToByte($_, 16) }))`
     → được chuỗi 28 ký tự (kết thúc bằng `=`), dán vào. Mỗi thành viên thêm key hash của máy mình.
   - Bấm **Lưu thay đổi**.
5. Firebase Console → **Authentication → Sign-in method → Facebook** → Enable → dán **App ID** và **App Secret** → Save.
   Firebase hiện một link *OAuth redirect URI* → quay lại Facebook: **Trường hợp sử dụng → Đăng nhập Facebook → Tuỳ chỉnh / Cài đặt**
   → dán vào **URI chuyển hướng OAuth hợp lệ** → Lưu.
6. Điền vào `app/src/main/res/values/social_login.xml`: `facebook_app_id`, `facebook_client_token`,
   `fb_login_protocol_scheme` = `fb` + App ID (vd. `fb1234567890`).
   **App Secret chỉ dán vào Firebase Console, không bao giờ đặt trong app.**
7. App Facebook đang ở chế độ **Phát triển** (Development): chỉ tài khoản có vai trò mới đăng nhập được →
   **Vai trò ứng dụng → Vai trò** (App roles → Roles) → thêm Facebook của 3 thành viên (và của thầy nếu demo) làm *Người kiểm thử* / *Quản trị viên*.

### 3. Số điện thoại (SMS OTP)
1. **Sign-in method → Phone** → Enable.
2. **Gói Spark (miễn phí) không gửi được SMS thật** — gửi SMS thật cần gói Blaze (gắn thẻ).
   Để demo miễn phí: trong mục Phone → **Phone numbers for testing** thêm số thử nghiệm, ví dụ
   `+84 912 345 678` – mã `123456`. Trong app nhập `0912345678`, bấm Gửi mã, nhập `123456`.
   Số thử nghiệm không gửi SMS và không tốn hạn mức.
3. Lần đầu bấm Gửi mã, máy ảo có thể mở trang reCAPTCHA trong trình duyệt — đó là bước chống spam bình thường.

### Lỗi hay gặp
| Thông báo trong app | Cách sửa |
|---|---|
| "Cách đăng nhập này chưa được bật" | Bật provider ở Sign-in method |
| "App chưa được cấp phép: thêm mã SHA-1…" | Làm bước 0, tải lại google-services.json |
| "Chưa cấu hình đăng nhập Google…" | Bật Google + SHA-1, tải lại google-services.json |
| "Chưa cấu hình Facebook…" | Điền social_login.xml (bước 2.5) |
| "Gửi SMS thật cần gói Blaze…" | Dùng số điện thoại thử nghiệm |
| "Email này đã đăng ký bằng cách khác…" | Đăng nhập bằng cách đã dùng lần đầu |

### Firestore Rules
`firebase/firestore.rules` đã cho phép hồ sơ `users/{uid}` không có email (đăng nhập bằng SĐT/Facebook)
và có thêm trường `phone`, `authProvider`. **Nhớ Publish lại Rules.**
