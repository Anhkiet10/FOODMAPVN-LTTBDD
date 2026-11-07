# Tải quán ăn có tên quanh trung tâm 34 tỉnh/thành từ OpenStreetMap (Overpass API).
# Dữ liệu OpenStreetMap: (c) OpenStreetMap contributors, giấy phép ODbL.
#
# Cách chạy (PowerShell, tại thư mục source):
#   powershell -ExecutionPolicy Bypass -File tools\fetch-osm-places.ps1
# Kết quả: tools\osm\<mã-tỉnh>.json (34 file). Chạy lại chỉ tải các tỉnh còn thiếu.
# Mất khoảng 3-6 phút (nghỉ giữa các lần gọi để không bị máy chủ chặn).

$ErrorActionPreference = "Continue"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$outDir = Join-Path $PSScriptRoot "osm"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# Máy chủ chính thử nhiều lần nhất (lỗi 502 = đang bận, thử lại sau ít giây thường được)
$servers = @(
    "https://overpass-api.de/api/interpreter",
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass-api.de/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
    "https://overpass-api.de/api/interpreter"
)
$radius = 5000   # mét quanh trung tâm tỉnh

$provinces = @(
    "ha-noi|21.0285|105.8542"
    "hue|16.4637|107.5909"
    "hai-phong|20.8449|106.6881"
    "da-nang|16.0544|108.2022"
    "ho-chi-minh|10.7769|106.7009"
    "can-tho|10.0452|105.7469"
    "an-giang|10.0125|105.0809"
    "bac-ninh|21.2731|106.1946"
    "ca-mau|9.1769|105.1524"
    "cao-bang|22.6657|106.2570"
    "dak-lak|12.6667|108.0500"
    "dien-bien|21.3860|103.0230"
    "dong-nai|10.9574|106.8427"
    "dong-thap|10.3600|106.3600"
    "gia-lai|13.7820|109.2196"
    "ha-tinh|18.3428|105.9057"
    "hung-yen|20.6464|106.0511"
    "khanh-hoa|12.2388|109.1967"
    "lai-chau|22.3964|103.4582"
    "lam-dong|11.9404|108.4583"
    "lang-son|21.8537|106.7610"
    "lao-cai|21.7229|104.9113"
    "nghe-an|18.6796|105.6813"
    "ninh-binh|20.2506|105.9745"
    "phu-tho|21.3227|105.4020"
    "quang-ngai|15.1205|108.7923"
    "quang-ninh|20.9590|107.0448"
    "quang-tri|17.4689|106.6223"
    "son-la|21.3256|103.9188"
    "tay-ninh|10.5359|106.4137"
    "thai-nguyen|21.5942|105.8482"
    "thanh-hoa|19.8067|105.7852"
    "tuyen-quang|21.8236|105.2180"
    "vinh-long|10.2537|105.9722"
)

$i = 0
foreach ($p in $provinces) {
    $i++
    # mỗi dòng "mã|vĩ độ|kinh độ"
    $parts = $p.Split("|"); $id = $parts[0]; $lat = $parts[1]; $lon = $parts[2]
    $file = Join-Path $outDir "$id.json"
    $radiusNow = $radius
    $v2Mark = "$file.v2"   # đã tải bằng truy vấn mới (quán ăn và cà phê tách riêng)
    if (Test-Path $file) {
        $count = 0
        try { $count = (Get-Content $file -Raw -Encoding UTF8 | ConvertFrom-Json).elements.Count } catch { }
        $isWide = Test-Path "$file.wide"
        if ((Test-Path $v2Mark) -or (($count -ge 10) -and ($count -lt 150)) -or (($count -gt 0) -and ($count -lt 10) -and $isWide)) {
            Write-Host "[$i/34] $id : đã có ($count quán), bỏ qua"
            continue
        }
        if ($count -ge 150) { Write-Host "[$i/34] $id : chạm giới hạn 150 (nhiều cà phê) -> tải lại tách quán ăn / cà phê" }
        else { Write-Host "[$i/34] $id : mới có $count quán -> tải lại bán kính 15 km"; $radiusNow = 15000 }
        Copy-Item $file "$file.bak" -Force
    }
    # Overpass QL dùng nháy đơn; ghi ra file tạm để không bị lỗi dấu nháy khi truyền cho curl.exe
    # Quán ăn tối đa 150 + cà phê/kem tối đa 30 (tránh nơi nhiều cà phê lấn hết chỗ)
    $around = "(around:$radiusNow,$lat,$lon)"
    $query = "[out:json][timeout:90];nwr['amenity'~'^(restaurant|fast_food|food_court)$']['name']$around;out center tags 150;nwr['amenity'~'^(cafe|ice_cream)$']['name']$around;out center tags 30;"
    $queryFile = Join-Path $env:TEMP "foodmapvn-overpass.txt"
    [IO.File]::WriteAllText($queryFile, $query)
    $ok = $false
    foreach ($attempt in 0..5) {
        $server = $servers[$attempt % $servers.Count]
        # curl.exe có sẵn trong Windows 10/11, hỗ trợ TLS mới hơn PowerShell 5.1
        & curl.exe -s -S -f -m 60 -A "FoodMapVN-student-project/1.0" --data-urlencode "data@$queryFile" -o $file $server
        if (($LASTEXITCODE -eq 0) -and (Test-Path $file) -and ((Get-Item $file).Length -gt 50)) { $ok = $true; break }
        Write-Host "   lỗi ở $server (mã $LASTEXITCODE) -> thử máy chủ khác"
        Start-Sleep -Seconds (5 * ($attempt + 1))
    }
    if ($ok) {
        New-Item -ItemType File -Force -Path $v2Mark | Out-Null
        if ($radiusNow -gt $radius) { New-Item -ItemType File -Force -Path "$file.wide" | Out-Null }
        Write-Host "[$i/34] $id : xong ($([int]((Get-Item $file).Length / 1024)) KB)"
    }
    else {
        Write-Host "[$i/34] $id : THẤT BẠI, chạy lại script sau"
        if (Test-Path "$file.bak") { Move-Item "$file.bak" $file -Force }
        else { Remove-Item $file -ErrorAction SilentlyContinue }
    }
    Remove-Item "$file.bak" -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}
Write-Host ""
Write-Host "Xong. Số file: $((Get-ChildItem $outDir -Filter *.json).Count)/34 trong $outDir"
