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
1. Đăng ký https://cloudinary.com → lấy **Cloud name**.
2. Settings → Upload → **Add upload preset** → *Signing mode: Unsigned*.
3. Điền vào `app/src/main/res/values/cloudinary.xml`. **Không** đưa API secret vào app.

## Gỡ lỗi (Logcat)
- Tag `FoodMapData`: document có trường **sai kiểu** (app bỏ qua trường đó thay vì crash) – sửa lại trong Console.
- Tag `FoodMapFirestore`: truy vấn bị lỗi, thường do Rules chặn (`PERMISSION_DENIED`).
- Tag `FoodMap`: "đang dùng dữ liệu giả" = app chưa thấy google-services.json.
- Nạp dữ liệu báo `PERMISSION_DENIED`: tài khoản chưa là admin, hoặc Rules chưa Publish bản mới.
