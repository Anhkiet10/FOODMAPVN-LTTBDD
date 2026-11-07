"""Dựng app/src/debug/assets/seed-data.json cho 34 tỉnh/thành.

Chạy (tại thư mục source):  python tools/build_seed.py
Đầu vào:
  - tools/dishes_vn.py            món đặc sản từng tỉnh
  - tools/osm/<mã-tỉnh>.json      quán ăn thật tải từ OpenStreetMap (tools/fetch-osm-places.ps1)
  - seed-data.json hiện có        giữ config, món Huế, quán demo "(Mẫu)" và dữ liệu currentUser

Quán thật (OpenStreetMap, ODbL) được để TRUNG TÍNH: chưa có đánh giá, uy tín 0 (app hiện "Chưa có
đánh giá"), không cảnh báo giá, không menu/giá bịa. Vài quán demo ở Huế có tên "(Mẫu)" là quán
tưởng tượng, giữ số liệu giả để demo màu viền uy tín và cảnh báo giá.
"""
import json
import math
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TOOLS = ROOT / "tools"
SEED = ROOT / "app/src/debug/assets/seed-data.json"
PROVINCES_KT = ROOT / "app/src/main/java/com/example/foodmapvn/data/local/VietnamProvinces.kt"
MAX_REAL_PLACES = 30      # quán thật (khớp món đặc sản) tối đa mỗi tỉnh
PLACES_PER_DISH = 3       # mỗi món đặc sản có ít nhất 3 quán (thật + "(Mẫu)")
QUICK_WORDS = ["Bánh", "Bún", "Phở", "Cơm", "Chè", "Lẩu", "Cá", "Gà", "Hủ tiếu", "Mì", "Cháo", "Xôi", "Nem",
               "Gỏi", "Thịt", "Kẹo", "Cà phê", "Chả", "Lợn", "Vịt", "Dê", "Bò", "Mực"]

sys.path.insert(0, str(TOOLS))
from dishes_vn import DISHES, EXTRA, KEYWORDS  # noqa: E402

# Quán demo ở Huế: đổi sang tên tưởng tượng để không gắn số liệu giả cho quán có thật
DEMO_RENAMES = [
    ("Bánh canh Nam Phổ Mụ Đợi", "Bánh canh Mẫu F (Mẫu)"),
    ("Bánh khoái Lạc Thiện", "Bánh khoái Mẫu G (Mẫu)"),
    ("Bánh bèo Bà Cụ", "Bánh bèo Mẫu H (Mẫu)"),
    ("Bún bò O Hạnh", "Bún bò Mẫu A (Mẫu)"),
    ("Bún bò Mệ Kéo", "Bún bò Mẫu B (Mẫu)"),
    ("Cơm hến Bà Nở", "Cơm hến Mẫu E (Mẫu)"),
    ("Chè Hẻm Huế", "Chè Mẫu D (Mẫu)"),
    ("Chè Hẻm", "Chè Mẫu D (Mẫu)"),
    ("Hoàng Cung", "Nhà hàng Mẫu C (Mẫu)"),
    ("Lạc Thiện", "Bánh khoái Mẫu G"),
    ("O Hạnh", "Bún bò Mẫu A"),
    ("Bà Nở", "Cơm hến Mẫu E"),
]


def rename_demo(obj):
    if isinstance(obj, dict):
        return {k: rename_demo(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [rename_demo(v) for v in obj]
    if isinstance(obj, str) and "(Mẫu)" not in obj and "Mẫu " not in obj:
        for old, new in DEMO_RENAMES:
            if old in obj:
                return obj.replace(old, new)
    return obj


def fold(text: str) -> str:
    """Bỏ dấu, viết thường: 'Phở Bò' -> 'pho bo'."""
    t = unicodedata.normalize("NFD", text.lower()).replace("đ", "d")
    return "".join(c for c in t if unicodedata.category(c) != "Mn")


def provinces():
    src = PROVINCES_KT.read_text(encoding="utf-8")
    rows = re.findall(r'Province\("([a-z-]+)", "([^"]+)", (?:true|false), LatLng\(([\d.]+), ([\d.]+)\)', src)
    assert len(rows) == 34, len(rows)
    return [(pid, name, float(lat), float(lon)) for pid, name, lat, lon in rows]


# ---- loại món của quán từ tên + thẻ cuisine của OpenStreetMap ----
SINGLE_WORD_DISHES = {"pho", "chao", "xoi", "lau", "oc", "nem", "kem", "cua", "de"}

CATEGORY_RULES = [
    ("drink", ["ca phe", "caphe", "coffee", "cafe", "tra sua", "milk tea", "tea", "sinh to", "juice", "nuoc ep"]),
    ("dessert", ["che ", "kem ", "ice cream", "ice_cream", "banh ngot", "bakery", "cake", "dessert", "sua chua",
                 "tau hu", "waffle"]),
    ("banh_mi", ["banh mi", "banhmi", "sandwich"]),
    ("traditional_cake", ["banh xeo", "banh cuon", "banh beo", "banh can", "banh khot", "banh trang", "banh uot",
                          "banh bot loc", "banh hoi", "banh gio", "banh duc", "banh khoai"]),
    ("noodle_soup", ["pho ", "bun ", "hu tieu", "hu tiu", " mi ", " my ", "mien ga", "mien luon", "banh canh",
                     "chao ", "noodle", "ramen", "udon", "banh da", "cao lau"]),
    ("hotpot_grill", ["lau ", "nuong", "bbq", "barbecue", "grill", "hotpot", "hot pot", "nem lui", " de ",
                      "buffet", "korean"]),
    ("seafood", ["hai san", "seafood", " oc ", " cua ", " tom ", " ghe ", "fish", " ca ", " muc ", "sushi"]),
    ("rice", [" com ", "xoi ", " rice", "chicken rice"]),
]
# Tiền tố chung không nói gì về món ("Cửa hàng" bỏ dấu thành "cua hang" -> dễ nhầm là cua)
NAME_PREFIXES = ["cua hang", "nha hang", "quan an", "quan", "tiem", "restaurant"]
CAFE_AMENITIES = {"cafe"}
DESSERT_AMENITIES = {"ice_cream"}


def category_of(tags):
    name = fold(tags.get("name", ""))
    for prefix in NAME_PREFIXES:
        name = name.replace(prefix + " ", " ")
    text = " " + name + " " + fold(tags.get("cuisine", "").replace(";", " ").replace("_", " ")) + " "
    text = re.sub(r"[^a-z0-9]+", " ", text)
    text = " " + text.strip() + " "
    for cat, keys in CATEGORY_RULES:
        if any(k in text for k in keys):
            return cat
    amenity = tags.get("amenity")
    if amenity in CAFE_AMENITIES:
        return "drink"
    if amenity in DESSERT_AMENITIES:
        return "dessert"
    return "other"


def dish_keywords(dish_name: str, dish_id: str = ""):
    if dish_id in KEYWORDS:
        return KEYWORDS[dish_id]
    """'Bún chả Hà Nội' -> ['bun cha']; khớp quán có tên chứa từ khoá này."""
    words = fold(dish_name).split()
    kws = [" ".join(words[:2])] if len(words) >= 2 else list(words)
    # Món mà chữ đầu đã đủ nhận ra (Phở Hà Nội -> "pho"): khớp cả quán tên "Phở 10 Lý Quốc Sư"
    if words and words[0] in SINGLE_WORD_DISHES:
        kws.append("^" + words[0])   # "^" = chỉ khớp ở đầu tên quán ("Phố cổ" không bị nhầm là "Phở")
    return kws


def dish_ids_of(tags, region_dishes):
    text = " " + fold(tags.get("name", "")) + " "

    def hit(k):
        return text.startswith(" " + k[1:] + " ") if k.startswith("^") else f" {k} " in text
    return [d["id"] for d in region_dishes if any(hit(k) for k in d["kw"])]


HOURS_RE = re.compile(r"(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})")


def hours_of(tags):
    """Chỉ lấy giờ mở cửa dạng đơn giản 'HH:mm-HH:mm' (có thể kèm 'Mo-Su')."""
    m = HOURS_RE.search(tags.get("opening_hours", ""))
    if not m:
        return None, None
    h1, m1, h2, m2 = m.groups()
    close_h = int(h2) % 24
    return f"{int(h1):02d}:{m1}", f"{close_h:02d}:{m2}"


def address_of(tags, province_name):
    number, street_name = tags.get("addr:housenumber", ""), tags.get("addr:street", "")
    if tags.get("addr:full"):
        return tags["addr:full"]
    if street_name.startswith(number) or "," in street_name:   # tên đường đã kèm số nhà / cả địa chỉ
        number = ""
    if "," in street_name:
        return street_name
    street = " ".join(x for x in [number, street_name] if x)
    area = tags.get("addr:subdistrict") or tags.get("addr:ward") or tags.get("addr:quarter") \
        or tags.get("addr:district") or tags.get("addr:city")
    parts = [p for p in [street, area] if p and not (p == area and street and area in street)]
    return ", ".join(parts) if parts else f"Khu trung tâm {province_name} (chưa có số nhà trên OpenStreetMap)"


def distance_m(lat1, lon1, lat2, lon2):
    r = 6371000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def pick_places(elements, pid, pname, clat, clon, region_dishes):
    """Chọn ~10 quán: ưu tiên quán khớp món đặc sản, món Việt, có giờ mở cửa; đa dạng loại; gần trung tâm."""
    seen_names, candidates = set(), []
    region_cats = {d["id"]: d["cat"] for d in region_dishes}
    for el in elements:
        tags = el.get("tags", {})
        name = tags.get("name:vi") or tags.get("name")
        if not name:
            continue
        lat = el.get("lat") or el.get("center", {}).get("lat")
        lon = el.get("lon") or el.get("center", {}).get("lon")
        if lat is None or lon is None:
            continue
        key = fold(name)
        if key in seen_names:   # bỏ chuỗi cửa hàng trùng tên
            continue
        seen_names.add(key)
        tags = dict(tags, name=name)
        dish_ids = dish_ids_of(tags, region_dishes)
        # Khớp được món đặc sản -> icon theo loại của món đó (bún chả = nướng), không thì đoán từ tên/cuisine
        cat = region_cats[dish_ids[0]] if dish_ids else category_of(tags)
        score = (3 if dish_ids else 0) + (1 if cat not in ("other", "drink") else 0) \
            + (1 if "opening_hours" in tags else 0) + (0.5 if "addr:street" in tags else 0) \
            - distance_m(clat, clon, lat, lon) / 5000
        candidates.append((score, el, tags, cat, dish_ids, lat, lon))
    candidates.sort(key=lambda c: -c[0])

    # App về đặc sản vùng: chỉ giữ quán bán ít nhất 1 món đặc sản của tỉnh
    matched = [c for c in candidates if c[4]]
    chosen, per_dish = [], {}
    for c in matched:                       # lượt 1: mỗi món tối đa 6 quán để món nào cũng có quán
        if all(per_dish.get(d, 0) >= 6 for d in c[4]):
            continue
        chosen.append(c)
        for d in c[4]:
            per_dish[d] = per_dish.get(d, 0) + 1
        if len(chosen) >= MAX_REAL_PLACES:
            break

    places = {}
    for _, el, tags, cat, dish_ids, lat, lon in chosen:
        open_t, close_t = hours_of(tags)
        doc = {
            "regionId": pid,
            "name": tags["name"],
            "address": address_of(tags, pname),
            "location": {"_geo": [round(lat, 6), round(lon, 6)]},
            "status": "active",
            "trustScore": 0,
            "isVerified": False,
            "priceWarning": False,
            "reviewCount": 0,
            "checkInCount": 0,
            "dishIds": dish_ids,
            "category": cat,
            "source": "OpenStreetMap",
            "osmId": f"{el['type']}/{el['id']}",
            "createdAt": {"_serverTimestamp": True},
        }
        if open_t:
            doc["openTime"], doc["closeTime"] = open_t, close_t
        places[f"osm-{el['type'][0]}{el['id']}"] = doc
    return places


# Tên chủ quán TƯỞNG TƯỢNG (kiểu đặt tên quán phổ biến); luôn kèm "(Mẫu)" nên không nhầm với quán thật
OWNERS = ["Cô Lan", "Chú Hai", "Bà Tư", "Anh Tuấn", "Chị Mai", "Út Hiền", "Cô Ba", "Chú Sáu", "Dì Năm",
          "Anh Khoa", "Chị Hạnh", "Bác Bảy", "Cô Thu", "Chú Tám", "Mợ Hoa", "Anh Phúc", "Chị Nga", "Ông Chín",
          "Cô Duyên", "Anh Long", "Chị Thảo", "Bà Mười", "Cô Hằng", "Chú Lộc", "Em Vy"]
STREET_HINTS = ["gần chợ", "gần trường học", "gần công viên", "gần bến xe", "trong hẻm", "mặt đường lớn",
                "gần bệnh viện", "gần nhà văn hoá", "khu dân cư", "gần bờ sông"]


def fictional_places(pid, pname, lat, lon, region_dishes, dish_names, real_places, anchors):
    """Bù quán TƯỞNG TƯỢNG '(Mẫu)' để mỗi món đặc sản có ít nhất PLACES_PER_DISH quán.
    Đặt cách 80–400 m quanh một quán ăn có trên OpenStreetMap (để nằm trong khu dân cư, trên đất
    liền); tỉnh không có dữ liệu thì đặt trong bán kính ~1,5 km quanh trung tâm.
    Để trung tính: chưa có đánh giá, không menu/giá."""
    import random
    rnd = random.Random(pid)          # cố định theo tỉnh: chạy lại ra cùng kết quả
    have = {}
    for v in real_places.values():
        for d in v["dishIds"]:
            have[d] = have.get(d, 0) + 1
    places, used = {}, set()
    for d in region_dishes:
        for n in range(max(0, PLACES_PER_DISH - have.get(d["id"], 0))):
            for _ in range(30):
                name = f"{dish_names[d['id']]} {rnd.choice(OWNERS)} (Mẫu)"
                if name not in used:
                    break
            used.add(name)
            if anchors:
                alat, alon = rnd.choice(anchors)
                dist, angle = rnd.uniform(0.0008, 0.0035), rnd.uniform(0, 2 * math.pi)
            else:
                alat, alon = lat, lon
                dist, angle = rnd.uniform(0.002, 0.013), rnd.uniform(0, 2 * math.pi)
            doc = {
                "regionId": pid,
                "name": name,
                "address": f"Quán tưởng tượng để demo – {rnd.choice(STREET_HINTS)}, {pname}",
                "location": {"_geo": [round(alat + dist * math.sin(angle), 6), round(alon + dist * math.cos(angle), 6)]},
                "status": "active",
                "trustScore": 0,
                "isVerified": False,
                "priceWarning": False,
                "reviewCount": 0,
                "checkInCount": 0,
                "dishIds": [d["id"]],
                "category": d["cat"],
                "createdAt": {"_serverTimestamp": True},
            }
            if rnd.random() < 0.7:
                open_h = rnd.choice([6, 6, 7, 9, 10, 15, 16])
                close_h = min(23, open_h + rnd.choice([5, 8, 12, 14]))
                doc["openTime"], doc["closeTime"] = f"{open_h:02d}:00", f"{close_h:02d}:00"
            places[f"mau-{pid}-{d['id']}-{n + 1}"] = doc
    return places


def main():
    seed = json.loads(SEED.read_text(encoding="utf-8"))
    old_places = seed.get("places", {})
    # Chỉ giữ 8 quán demo cố định ở Huế; quán "(Mẫu)" bù cho các tỉnh được sinh lại mỗi lần chạy
    demo_places = {k: v for k, v in old_places.items() if k.startswith("mau-") and v.get("regionId") == "hue"
                   and not k.startswith("mau-hue-")}
    seed = rename_demo(seed)
    demo_places = rename_demo(demo_places)
    for doc in demo_places.values():
        doc["address"] = doc["address"].replace("Dữ liệu mẫu – ", "Quán tưởng tượng để demo – ")

    regions, dishes, places = {}, {}, {}
    missing = []
    hue_dishes = {k: v for k, v in seed["dishes"].items() if v.get("regionId") == "hue" and k not in {x[0] for x in EXTRA["hue"]}}

    def dish_doc(pid, cat, name, unit, p25, p75, desc, best):
        doc = {
            "regionId": pid, "name": name, "description": desc, "unit": unit,
            "art": {"noodle_soup": "BUN_BO", "dessert": "CHE", "drink": "CHE"}.get(cat, "COM_HEN"),
            "priceP25": p25, "priceP75": p75, "verifiedPlaceCount": 0,
            "status": "active", "category": cat,
        }
        if best:
            doc["bestTime"] = best
        return doc

    for pid, pname, lat, lon in provinces():
        if pid == "hue":
            region_dishes = [{"id": k, "kw": dish_keywords(v["name"], k), "cat": v.get("category", "other")}
                             for k, v in hue_dishes.items()]
            dishes.update(hue_dishes)
            names = [v["name"] for v in hue_dishes.values()]
            for did, name, unit, cat, p25, p75, desc, best in EXTRA["hue"]:
                dishes[did] = dish_doc(pid, cat, name, unit, p25, p75, desc, best)
                region_dishes.append({"id": did, "kw": dish_keywords(name, did), "cat": cat})
                names.append(name)
        else:
            region_dishes = []
            names = []
            for did, name, unit, cat, p25, p75, desc, best in DISHES[pid] + EXTRA[pid]:
                dishes[did] = dish_doc(pid, cat, name, unit, p25, p75, desc, best)
                region_dishes.append({"id": did, "kw": dish_keywords(name, did), "cat": cat})
                names.append(name)

        osm_file = TOOLS / "osm" / f"{pid}.json"
        region_places, anchors = {}, []
        if osm_file.exists():
            elements = json.loads(osm_file.read_text(encoding="utf-8")).get("elements", [])
            region_places = pick_places(elements, pid, pname, lat, lon, region_dishes)
            # vị trí mọi quán ăn trên OSM (kể cả không khớp món) làm mốc đặt quán "(Mẫu)"
            anchors = [(e.get("lat") or e.get("center", {}).get("lat"), e.get("lon") or e.get("center", {}).get("lon"))
                       for e in elements if e.get("tags", {}).get("amenity") in ("restaurant", "fast_food", "food_court")]
            anchors = [a for a in anchors if a[0] and distance_m(lat, lon, a[0], a[1]) < 6000]
        else:
            missing.append(pid)
        dish_names = dict(zip([d["id"] for d in region_dishes], names))
        region_places.update(fictional_places(pid, pname, lat, lon, region_dishes, dish_names, region_places, anchors))
        if pid == "hue":
            region_places.update(demo_places)
        places.update(region_places)

        # số quán thật có bán từng món (chưa xác minh) -> giữ verifiedPlaceCount = 0
        # Lọc nhanh ở Khám phá = chuỗi con của tên món -> lấy các từ món phổ biến có trong vùng
        if pid == "hue":
            quick = ["Bún bò", "Cơm hến", "Chè", "Bánh"]
        else:
            counts = {w: sum(1 for n in names if n.startswith(w)) for w in QUICK_WORDS}
            quick = [w for w, c in sorted(counts.items(), key=lambda x: -x[1]) if c > 0][:4]
        regions[pid] = {
            "name": pname,
            "center": {"_geo": [lat, lon]},
            "dishCount": len(region_dishes),
            "placeCount": len(region_places),
            "isOpenNow": True,
            "quickFilters": quick[:4],
            "status": "active",
        }

    seed["_readme"] = (
        "Dữ liệu mẫu cho Food Map VN (≈ phần INSERT INTO của file .sql), sinh bằng tools/build_seed.py. "
        "Quán có id 'osm-…' là quán thật lấy từ OpenStreetMap (© OpenStreetMap contributors, ODbL), "
        "để trung tính: chưa có đánh giá, không menu/giá. Quán id 'mau-…' tên '(Mẫu)' là quán tưởng tượng "
        "để demo màu uy tín/cảnh báo giá. Giá món đặc sản là ước lượng. "
        "Quy ước: {\"_geo\": [vĩ độ, kinh độ]} = geopoint, {\"_serverTimestamp\": true} = thời điểm ghi, "
        "{\"_date\": \"yyyy-MM-dd\"} = timestamp. Tên trường theo mục 8.2 tài liệu đặc tả."
    )
    out = {"_readme": seed["_readme"], "regions": regions, "dishes": dishes, "places": places}
    for k, v in seed.items():
        if k not in out:
            out[k] = v
    SEED.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    real = sum(1 for k in places if k.startswith("osm-"))
    fake = sum(1 for k, v in places.items() if k.startswith("mau-") and v["regionId"] != "hue")
    print(f"Quán '(Mẫu)' bù cho đủ {PLACES_PER_DISH} quán/món: {fake}")
    print(f"Vùng: {len(regions)}  Món: {len(dishes)}  Quán: {len(places)} (thật từ OSM: {real}, demo: {len(places) - real})")
    if missing:
        print(f"Chưa có dữ liệu OSM ({len(missing)} tỉnh): {', '.join(missing)}")


if __name__ == "__main__":
    main()
