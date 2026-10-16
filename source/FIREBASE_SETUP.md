# Nối app Food Map VN với Firebase

Chưa làm các bước dưới thì app vẫn chạy bằng **dữ liệu giả** (FakeRepositories).
Làm xong thì `AppContainer` tự chuyển sang Firebase, không phải sửa code.

## 1. Tạo project Firebase (gói Spark, miễn phí, không cần thẻ)
1. Vào https://console.firebase.google.com → **Add project** → đặt tên `foodmapvn`.
2. **Add app → Android**, package name: `com.example.foodmapvn`.
3. Tải **google-services.json**, chép vào thư mục `app/` (cạnh `app/build.gradle.kts`).
4. Android Studio → **Sync Project with Gradle Files** → Run.

## 2. Bật dịch vụ
- **Authentication → Sign-in method → Email/Password → Enable.**
- **Firestore Database → Create database** (chọn vùng `asia-southeast1`), bắt đầu ở *production mode*.
- **Firestore → Rules**: dán nội dung file `firestore.rules` ở thư mục gốc dự án → **Publish**.

## 3. Nhập dữ liệu mẫu (Firestore Console → Start collection)

`regions/hue`
| Trường | Kiểu | Ví dụ |
|---|---|---|
| name | string | Huế |
| dishCount | number | 7 |
| placeCount | number | 12 |
| isOpenNow | boolean | true |
| quickFilters | array | ["Bún bò", "Cơm hến", "Chè"] |

`dishes/bun-bo-hue`
| Trường | Kiểu | Ví dụ |
|---|---|---|
| regionId | string | hue |
| name | string | Bún bò Huế |
| verifiedPlaceCount | number | 12 |
| nearestHint | string | 350 m |
| priceP25 / priceP75 | number | 35000 / 50000 |
| bestTime | string | Ngon nhất buổi sáng |
| unit | string | tô |
| art | string | BUN_BO (hoặc CHE, COM_HEN) |
| imageUrl | string | (link ảnh Cloudinary, có thể bỏ trống) |

`places/o-hanh`
| Trường | Kiểu | Ví dụ |
|---|---|---|
| regionId | string | hue |
| name | string | Bún bò O Hạnh |
| location | geopoint | 16.4655, 107.5905 |
| status | string | active |
| trustScore | number | 89 |
| priceWarning | boolean | false |

Dữ liệu của từng người dùng nằm dưới `users/{uid}` (tạo tự động khi đăng ký):
`wishlist/{dishId}` (dishName, placeName, distanceHint, eaten, art, imageUrl),
`savedPlaces/{placeId}`, `badges/{regionId}` (regionName, checkIns, placeCount, achievedAt),
`notifications/{id}` (title, read).
Đánh giá nằm ở `reviews/{placeId}_{uid}` (placeId, placeName, userId, stars, price, hasCheckIn, createdAt).

**Tạo tài khoản Admin:** đăng ký trong app, rồi trong Firestore sửa `users/{uid}.role` thành `admin`.

## 4. Cloudinary (lưu ảnh, miễn phí)
1. Đăng ký https://cloudinary.com → lấy **Cloud name**.
2. Settings → Upload → **Add upload preset** → *Signing mode: Unsigned*.
3. Điền vào `app/src/main/res/values/cloudinary.xml`. **Không** đưa API secret vào app.

## Gỡ lỗi
- Logcat lọc tag `FoodMapFirestore` để xem lỗi truy vấn (thường là Rules chặn).
- Logcat tag `FoodMap`: "đang dùng dữ liệu giả" nghĩa là app chưa thấy google-services.json.
