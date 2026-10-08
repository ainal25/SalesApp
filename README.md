# SalesApp — Android Native (Kotlin + Jetpack Compose)

Aplikasi manajemen salesman **asli Android (bukan WebView)** yang di-port dari versi web Anda.
Fitur: dashboard pencapaian & target, kelola toko (loyalty), produk, buat pesanan, cetak struk thermal 58mm via Bluetooth, backup/restore JSON. Database tersimpan lokal (SQLite via Room) — offline, cepat, stabil.

---

## 1) Yang Anda butuhkan sekali saja

| Perangkat lunak | Versi minimal | Link |
|---|---|---|
| **Android Studio** | Hedgehog (2023.1) atau lebih baru | https://developer.android.com/studio |
| **JDK 17** | sudah termasuk di Android Studio | — |
| HP Android | 7.0 (Nougat / API 24) ke atas | — |

> Tidak perlu install Node, Gradle, dsb. secara terpisah. Semua ditangani Android Studio.

---

## 2) Cara buka project

1. Download file ZIP yang saya kirim, lalu **extract** ke folder biasa (contoh: `C:\Projects\SalesApp` atau `~/AndroidProjects/SalesApp`).
2. Buka **Android Studio** → menu **File → Open…** → arahkan ke folder `SalesApp` yang baru di-extract → OK.
3. Android Studio akan otomatis:
   - men-download Gradle 8.9
   - men-download semua library (Compose, Room, Coil, dll.)
   - menyinkronkan project (indikator di pojok kanan bawah).
4. Tunggu sampai muncul teks **"Gradle sync finished"** di bagian paling bawah (sekitar 3-10 menit untuk pertama kali, tergantung koneksi).
5. Jika muncul popup "Trust Project?" → klik **Trust Project**.

> **Jika sync gagal:** biasanya karena koneksi internet / proxy. Coba menu **File → Sync Project with Gradle Files** sekali lagi.

---

## 3) Cara build APK

Setelah sync selesai:

**Opsi A — Debug APK (paling cepat, untuk tes di HP sendiri):**
1. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
2. Tunggu sampai muncul notifikasi hijau "APK generated successfully".
3. Klik tulisan **"locate"** → file `app-debug.apk` ada di:
   `SalesApp/app/build/outputs/apk/debug/app-debug.apk`
4. Pindahkan APK ke HP Android (via kabel USB atau kirim ke WhatsApp Diri Sendiri).
5. Di HP, buka file APK → izinkan install dari sumber tidak dikenal → **Install**.

**Opsi B — Release APK (untuk dibagikan ke tim sales, ter-signed):**
1. Menu **Build → Generate Signed Bundle / APK → APK → Next**.
2. **Create new keystore** (sekali seumur hidup project):
   - Key store path: `C:\Keys\salesapp.jks` (bebas lokasi, tapi **jangan hilang**!)
   - Password: buat password yang kuat, catat baik-baik.
   - Alias: `salesapp`, pakai password yang sama.
   - Validity: `25` tahun.
   - Isi First and Last Name secukupnya.
3. Setelah selesai, pilih **release** → **V1 + V2** → **Finish**.
4. APK ada di: `SalesApp/app/release/app-release.apk`.
5. **SIMPAN FILE `.jks` DAN PASSWORD-NYA!** Tanpa file ini Anda tidak bisa update aplikasi di masa depan.

---

## 4) Pakai aplikasinya di HP

1. Install APK. Buka aplikasi → ada 4 tab di bawah: **Beranda, Toko, Produk, Atur**.
2. **Langkah awal** (urutan disarankan):
   1. Tab **Atur → Profil Salesman** → isi nama + kode (akan tercetak di struk).
   2. Tab **Atur → Atur Target Bulanan** → isi target rupiah KSNI, Simba, Hello Panda bulan ini.
   3. Tab **Produk** → tombol **+** → tambahkan produk (nama, kategori, harga/CTN, BOX/CTN, PCS/BOX, diskon opsional).
   4. Tab **Toko** → tombol **+** → tambahkan toko. Centang **Loyalty** jika toko ikut program.
   5. **Pengaturan → Printer Bluetooth** → pairing printer thermal 58mm Anda di Pengaturan Android dulu, lalu di sini pilih printer.
3. **Buat pesanan:** Tab Toko → tap sebuah toko → **Buat Transaksi** → pilih produk → isi CTN/BOX/PCS → **Keranjang** → **Simpan & Cetak Struk**.

---

## 5) Struktur folder project (penjelasan lengkap)

```
SalesApp/
├── build.gradle.kts              ← konfigurasi versi plugin tingkat project
├── settings.gradle.kts           ← daftar module (hanya "app")
├── gradle.properties             ← flag kompilasi
├── gradle/wrapper/
│   └── gradle-wrapper.properties ← versi Gradle yang dipakai (8.9)
├── .gitignore
├── README.md                     ← file ini
└── app/
    ├── build.gradle.kts          ← dependency & build config aplikasi
    ├── proguard-rules.pro        ← aturan obfuscation untuk release build
    └── src/main/
        ├── AndroidManifest.xml   ← permission (Bluetooth), activity, launcher
        ├── res/
        │   ├── values/strings.xml   ← nama app, dsb.
        │   ├── values/colors.xml    ← warna brand
        │   ├── values/themes.xml    ← tema XML (statusbar hijau, dll.)
        │   ├── drawable/            ← logo vektor
        │   ├── mipmap-anydpi-v26/   ← ikon adaptif Android 8+
        │   ├── mipmap-*/            ← ikon fallback Android 7
        │   └── xml/*.xml            ← aturan auto-backup
        └── java/com/salesapp/android/
            ├── SalesApplication.kt      ← kelas Application (DI manual: Repository)
            ├── MainActivity.kt          ← entry point, pasang tema + navigasi
            ├── data/
            │   ├── AppDatabase.kt       ← Room database (SQLite di belakang layar)
            │   ├── Repository.kt        ← satu pintu akses data
            │   ├── entities/Entities.kt ← Store, Product, TransactionEntity, dsb.
            │   └── dao/Daos.kt          ← operasi query (observeAll, upsert, ...)
            ├── bluetooth/
            │   ├── BluetoothPrinter.kt  ← koneksi SPP (RFCOMM) ke printer BT
            │   └── ReceiptBuilder.kt    ← rangkai byte ESC/POS 58mm
            ├── backup/
            │   └── BackupManager.kt     ← export/import JSON via SAF
            ├── util/
            │   └── Fmt.kt               ← format Rp, tanggal, year-month
            ├── viewmodel/
            │   └── MainVM.kt            ← state global: cart, pilihan bulan, dsb.
            └── ui/
                ├── MainNav.kt           ← NavHost + bottom bar (4 tab)
                ├── theme/
                │   ├── Theme.kt         ← warna Material 3 (hijau brand)
                │   └── Type.kt          ← ukuran teks
                ├── components/
                │   └── Common.kt        ← BrandTopBar, StatPill (reusable)
                └── screens/
                    ├── DashboardScreen.kt       ← pencapaian KSNI/Simba/Hello Panda + toko loyalty
                    ├── StoresScreen.kt          ← list toko + form
                    ├── ProductsScreen.kt        ← list produk + form
                    ├── OrderScreen.kt           ← pilih produk + popup qty + cart
                    ├── ReceiptAndHistory.kt     ← preview struk + riwayat transaksi
                    └── SettingsScreens.kt       ← pengaturan, target, printer, profil, backup
```

### Penjelasan file-file inti (ringkas)

- **`AppDatabase.kt`** — SQLite via Room. Nama file DB di HP: `salesapp.db`.
- **`Entities.kt`** — definisi tabel: `stores`, `products`, `transactions`, `monthly_targets`, `profile`.
- **`Repository.kt`** — satu pintu untuk semua operasi data; dipanggil dari ViewModel.
- **`MainVM.kt`** — menyimpan isi keranjang pesanan, bulan terpilih, dsb. Live update dengan Kotlin Flow.
- **`BluetoothPrinter.kt`** — buka koneksi RFCOMM (UUID `00001101-...`) ke printer. Kompatibel dengan sebagian besar printer thermal 58mm murah (RPP02, Zjiang, Goojprt, MTP-II, dll.).
- **`ReceiptBuilder.kt`** — susun struk 32 kolom + perintah ESC/POS (bold, align, cut).
- **`BackupManager.kt`** — pakai Storage Access Framework. User pilih lokasi file sendiri (tidak minta izin storage ribet).

---

## 6) Permission (izin) yang diminta

Semua izin sudah terdaftar di `AndroidManifest.xml`:

| Izin | Untuk apa | Diminta ke user kapan? |
|---|---|---|
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | Konek ke printer thermal | Saat buka layar **Printer** |
| `ACCESS_FINE_LOCATION` (Android ≤ 11) | Wajib untuk scan BT di Android lama | Saat buka layar **Printer** |

**Kamera / Lokasi tidak diminta** — aplikasi tidak membutuhkan sensor lain.

---

## 7) Perubahan vs versi HTML (web) Anda

Semua fitur inti **tetap ada**:
- ✅ Dashboard target KSNI/Simba/Hello Panda per bulan + progress bar.
- ✅ Dashboard "Toko Loyalty" dengan progress %.
- ✅ Kelola toko (list, search, loyalty flag, target Rp).
- ✅ Kelola produk (list, filter kategori, search).
- ✅ Buat pesanan dengan qty CTN/BOX/PCS + diskon COD.
- ✅ Riwayat transaksi + edit + hapus + cetak ulang struk.
- ✅ Cetak struk thermal 58mm via Bluetooth.
- ✅ Backup & restore JSON.
- ✅ Profil salesman tercetak di struk.

**Yang disederhanakan** (jika Anda butuh, saya bisa tambahkan lagi):
- Diskon di versi ini disimpan **per-produk** (flat Rp/CTN) — tidak lagi grup Reguler/Strata/ROA bertingkat.
  Alasan: 90% user cuma pakai diskon flat. Kalau butuh promo bertingkat multi-tier, tinggal bilang → saya tambahkan modul grup diskon.
- Target ROA pemerataan di dashboard saat ini belum tampil (butuh grup ROA). Bisa saya tambahkan di iterasi berikut.
- Foto profil belum pakai image picker (field path sudah ada di DB, tinggal pasang `PickVisualMedia` kalau diperlukan).

---

## 8) Kalau mentok / error

| Masalah | Solusi |
|---|---|
| "Gradle sync failed" | Pastikan internet aktif. Menu **File → Invalidate Caches / Restart**. |
| "SDK not found" | Android Studio → **File → Project Structure → SDK Location** → install SDK API 34. |
| Printer tidak ketemu | Pairing dulu dari **Setelan Android → Bluetooth**, baru buka layar Printer di app. |
| APK tidak bisa diinstall | Aktifkan **"Install dari sumber tidak dikenal"** untuk file manager Anda. |
| Build error "duplicate class" | Hapus folder `build/` dan `.gradle/` lalu sync ulang. |

---

## 9) Mengubah nama aplikasi / warna / ikon

- **Nama aplikasi**: edit `app/src/main/res/values/strings.xml` → ganti nilai `app_name`.
- **Warna hijau utama**: edit `values/colors.xml` → ubah `brand_green`. Juga sync warna di `ui/theme/Theme.kt` (variabel `BrandGreen`).
- **Ikon aplikasi**: ganti vector `drawable/ic_launcher_fg.xml` atau gunakan **Image Asset Studio** (klik kanan folder `res` → **New → Image Asset**).

---

Selamat mencoba! Kalau ada bagian yang mau ditambahkan (grup diskon bertingkat, ROA, grafik, dsb.) tinggal bilang.
"# SalesApp" 
"# SalesApp" 
"# SalesApp" 
