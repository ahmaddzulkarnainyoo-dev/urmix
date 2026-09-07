# URMIX — Technical Blueprint v2
**Developed & fully owned by WINATRA • Built on the NewPipe codebase (forked, all official NewPipe network access removed)**

> Dokumen ini adalah **single source of truth** untuk AI coding agent (Cline, Claude Code, Copilot) yang mengerjakan proyek ini. Setiap section ditulis biar bisa langsung dieksekusi jadi task tanpa perlu tebak-tebakan konteks.

---

## 0. Executive Summary & Philosophy

URMIX adalah **fork penuh dari NewPipe** yang di-*repackage* jadi produk milik Winatra sepenuhnya — bukan kolaborasi, bukan upstream-tracking. Prinsip intinya:

- **NewPipe ketinggalan jaman di UI/UX** — engine-nya (extraction, parsing, playback core) sudah battle-tested dan tetap dipakai apa adanya. Yang diubah total adalah **lapisan presentasi**: dari "grid video YouTube" jadi "streamer audio ala Spotify".
- **Semua akses resmi NewPipe diputus total.** Tidak ada update checker, notification service, RSS/Notice, atau telemetry apa pun yang mengarah ke server/infra resmi NewPipe. Semua kontrol (update, pengumuman, donasi) sepenuhnya milik dan dikendalikan Winatra lewat remote config sendiri.
- **Bukan rewrite backend.** Fitur seperti trending, subscription feed, watch history, search, playlist — semua reuse mesin NewPipe yang sudah ada, cuma tampil dengan UI baru.
- **Tim internal Winatra** memegang penuh kontrol source, branding, distribusi, dan monetisasi. Tidak ada ketergantungan pihak ketiga selain infra yang dipilih sendiri (Supabase, Saweria).

**Constraint kunci yang mengunci seluruh arsitektur di bawah:**

| Aspek | Keputusan |
|---|---|
| Engine | Fork NewPipe — reuse extraction/parsing/feed engine, reskin UI, tambah fitur baru |
| Akun/library | Local-only (Room DB per-device), tanpa cloud sync |
| Backup data | Export/import manual (file JSON), dengan panduan jelas di dalam app |
| Konten carousel | Reuse feed/trending/subscription/history NewPipe, direskin jadi tampilan audio |
| Monetisasi | Notifikasi dukungan 1x/hari (Saweria/QRIS), non-blocking, bisa di-dismiss |
| Mekanisme update | Remote config (`force_update` / `min_extractor_version`) via Supabase — pengganti Play Store auto-update |
| Distribusi | Source code di GitHub (repo privat/tim), APK disebar lewat grup Telegram |
| Branding | Splash "Developed by WINATRA", About section transparan soal basis NewPipe |

---

## 1. Network Detachment & Remote Control System

### 1.1 Cut Upstream NewPipe Network
Task untuk agent:
- Cari dan **hapus total** (bukan cuma di-disable via flag) semua referensi ke:
  - `AppUpdateChecker` bawaan NewPipe dan endpoint update resminya
  - `NotificationService` / notice fetcher bawaan
  - Endpoint RSS/blog/Notice resmi NewPipe (biasanya di `NewPipeSettings` / `ReleaseChannel` sejenisnya)
- Audit seluruh base URL hardcoded yang menunjuk ke domain NewPipe resmi (`newpipe.net`, dst) — pastikan tidak ada satupun request keluar ke sana.
- Ganti seluruh konstanta branding string (`App name`, `about text`, `github link`) ke identitas Winatra/URMIX.

### 1.2 Winatra Remote Control Config

Config diambil dari Supabase setiap app dibuka. **Beda dari draft awal:** sekarang punya versioning, minimum-extractor-version check, dan wajib ada local cache/fallback biar app tidak stuck kalau server lambat/down.

```json
{
  "config_schema_version": 1,
  "app_version": "1.0.0",
  "min_extractor_version": "0.24.0",
  "force_update": false,
  "update_url": "https://winatra.com/urmix/download",
  "announcement": {
    "active": true,
    "title": "URMIX Update v1.0",
    "message": "Sistem audio player baru telah aktif."
  },
  "donation": {
    "title": "Dukung Winatra",
    "saweria_url": "https://saweria.co/winatra",
    "qris_url": "https://winatra.com/donate/qris.png",
    "daily_message": "Bantu kami menjaga kelangsungan server URMIX."
  }
}
```

**Field baru dan alasannya:**
- `config_schema_version` — biar app lama tau kalau struktur JSON berubah, dan bisa fallback aman kalau versi tidak dikenal (jangan crash parsing).
- `min_extractor_version` — kalau NewPipeExtractor lokal di bawah versi ini (karena YouTube ubah struktur dan extraction mulai gagal), app minta user update lewat jalur yang sama dengan `force_update`. Ini penting karena tidak ada Play Store auto-update.

**Task untuk agent:**
1. Buat `RemoteConfigRepository` yang fetch config dengan timeout pendek (misal 4 detik).
2. **Cache config terakhir** di `SharedPreferences`/DataStore. Kalau fetch gagal/timeout, pakai cache lama dan **jangan blokir user masuk ke player**, kecuali cache sebelumnya memang bilang `force_update: true`.
3. Kalau `force_update == true` **dan** versi app di bawah `app_version` yang diminta → tampilkan dialog non-dismissable, blokir akses ke player utama, arahkan ke `update_url`.
4. Kalau `force_update == false` → tampilkan `announcement` sebagai banner/dialog yang bisa di-skip, lalu lanjut normal.
5. Validasi tiap field config sebelum dipakai (null-safety, format URL) — kalau config korup, treat sebagai "tidak ada perubahan", jangan crash.

---

## 2. Splash Screen & Brand Identity

- `SplashActivity` sebagai launcher activity di `AndroidManifest.xml`.
- Durasi tampil: **1.5–2.0 detik** (pakai `Handler.postDelayed` atau `lifecycleScope.launch { delay(...) }`, bukan `Thread.sleep`).
- Visual: dark background `#121212`, logo URMIX teranimasi (fade-in/scale sederhana), teks footer "Developed by WINATRA" dan "Built on NewPipe Core" (transparan, bukan menyembunyikan asal-usul).
- Selama splash tampil, boleh sekalian trigger fetch remote config di background (§1.2) biar hasil sudah siap pas masuk main screen.

---

## 3. UI/UX Redesign — Spotify-Style Layer di Atas Engine NewPipe

**Prinsip:** ini adalah reskin, bukan penulisan ulang data layer. Setiap section di bawah menyebutkan sumber data NewPipe yang direuse.

### 3.1 Layout Paradigm
Ubah dari "YouTube Video Grid" jadi "Spotify Audio Streamer Layout":

| Section UI | Sumber Data (reuse dari NewPipe) | Catatan |
|---|---|---|
| Mini Player (sticky bottom bar) | `PlayerService` / `PlayQueue` NewPipe yang sudah ada | Thumbnail bulat/rounded, judul track, tombol Play/Pause/Next |
| "Made For You" carousel | Subscription feed + watch history NewPipe | Reskin `FeedFragment`/history data jadi horizontal card, bukan bikin algoritma baru |
| "Trending Audio" carousel | Trending page per-service NewPipe (`KioskFragment`/trending extractor) | Filter opsional berdasarkan durasi/kategori biar condong ke konten audio |
| "Podcasting" carousel | **Fitur baru** — tidak ada di NewPipe native | Perlu didefinisikan sendiri (lihat §3.3) |

### 3.2 Theme Palette
```
Primary Background : #121212
Card Surface        : #181818
Accent Brand         : #1DB954
Text Primary         : #FFFFFF
Text Secondary       : #B3B3B3 (tambahan — dibutuhkan buat subtitle/metadata track)
```

### 3.3 "Podcasting" — Definisi Fitur Baru
Karena NewPipe tidak punya konsep podcast bawaan, agent perlu treat ini sebagai kategori konten, bukan mesin baru:
- Implementasi paling murah: kurasi channel/playlist YouTube tertentu yang berisi konten panjang (podcast) via `RemoteConfig` (daftar channel ID dikirim dari Supabase, sama seperti donation config) — jadi kurasi bisa diubah kapan saja tanpa update APK.
- UI-nya reuse komponen carousel yang sama dengan "Trending", cuma sumber datanya beda channel/playlist list.

### 3.4 Full Player Screen
- Expand dari mini player (swipe up / tap).
- Cover art besar, judul, artist/channel, progress bar seekable, tombol shuffle/repeat/queue.
- **Bukan wajib di v1:** lyrics, equalizer — bisa jadi fitur v2, tandai di roadmap (§9).

---

## 4. Playback Engine

Ini bagian paling krusial yang tidak ada di draft awal — tanpa ini, "audio player" cuma kulit tanpa nyawa.

### 4.1 Background Playback
- Implementasi `MediaSessionCompat` + `MediaBrowserServiceCompat` (atau Media3 `MediaSessionService` kalau agent memutuskan migrasi ke Media3/ExoPlayer terbaru).
- Foreground service wajib aktif selama playback, dengan notification media controls (play/pause/next/prev) yang tetap muncul di lockscreen & shade.
- Audio focus handling: duck/pause otomatis saat ada panggilan telepon atau notifikasi suara lain, resume setelah selesai (sesuai preferensi user).

### 4.2 Audio-Only Extraction
- Ambil audio-only stream dari NewPipeExtractor (itag audio-only) sebagai default untuk mode "audio player".
- **Fallback wajib:** kalau audio-only stream tidak tersedia untuk video tertentu, fallback ke video stream tapi mainkan sebagai audio (video track di-mute/tidak dirender) — jangan biarkan track gagal total tanpa fallback.

### 4.3 Queue & Playback State
- `PlayQueue` reuse dari NewPipe, tambahkan: shuffle, repeat (off/one/all), "play next" insert.
- Simpan playback state terakhir (track + posisi) di local storage biar bisa resume setelah app di-kill.

### 4.4 Error Handling & Graceful Degradation
- Extraction gagal (karena YouTube berubah / video private / region lock) → tampilkan pesan jelas ke user + skip otomatis ke track berikutnya di queue (jangan diam/hang).
- Kalau kegagalan terjadi berturut-turut (misal 3x dalam satu sesi) → munculkan hint halus ke user untuk cek update (terhubung ke §1.2 `min_extractor_version`), karena kemungkinan besar extractor sudah usang.

---

## 5. Library & Data (Local-Only + Backup)

### 5.1 Local Storage
- Room DB menyimpan: liked tracks, playlist buatan user, recently played/history, downloaded tracks (kalau ada fitur download).
- **Tidak ada akun, tidak ada cloud sync** — semua data terikat ke device.

### 5.2 Backup & Restore (Export/Import Manual)
Karena data lokal-only, wajib ada jalan keluar biar user tidak kehilangan data saat ganti device/uninstall.

**Format backup — JSON terstruktur:**
```json
{
  "backup_schema_version": 1,
  "exported_at": "2026-09-07T12:00:00Z",
  "liked_tracks": [
    { "video_id": "xxxx", "title": "...", "artist": "...", "added_at": "..." }
  ],
  "playlists": [
    { "name": "My Playlist", "track_ids": ["xxxx", "yyyy"] }
  ],
  "history": [
    { "video_id": "xxxx", "played_at": "..." }
  ]
}
```

**Task untuk agent:**
1. Buat menu **Settings → Backup & Restore** dengan dua tombol jelas: "Export Data" dan "Import Data".
2. Export → generate file `.json` (nama file: `urmix_backup_YYYYMMDD.json`), simpan lewat Storage Access Framework (`ACTION_CREATE_DOCUMENT`) biar user pilih sendiri lokasinya (Download folder, share ke Telegram Saved Messages, dll).
3. Import → `ACTION_OPEN_DOCUMENT`, validasi `backup_schema_version` sebelum parsing, tampilkan preview ringkas ("Ditemukan 42 liked tracks, 5 playlist — timpa data sekarang?") sebelum benar-benar overwrite/merge data lokal.
4. **Wajib ada in-app guide** (bisa berupa dialog step-by-step atau halaman bantuan singkat) yang menjelaskan ke user: *"Sebelum uninstall atau ganti HP, export dulu data kamu di Settings → Backup, simpan filenya (misal kirim ke diri sendiri di Telegram), lalu import lagi di HP baru."* Bahasa harus awam, bukan istilah teknis.

---

## 6. Daily Donation & Support Notification

- `WorkManager` `PeriodicWorkRequest` interval 24 jam, nama unik `WinatraDonationWorker`.
- Cek `SharedPreferences` (`last_donation_prompt_date`) supaya notifikasi **maksimal 1x per hari**.
- Notifikasi non-intrusive, bisa di-dismiss tanpa konsekuensi apa pun (beda total dari `force_update` yang blocking — jangan sampai ketuker di implementasi).
- Klik notifikasi → buka `DonationBottomSheetDialogFragment`, isinya dua opsi dari remote config: link Saweria (`saweria_url`) dan gambar QRIS (`qris_url`). Keduanya dark-themed sesuai palette §3.2.

---

## 7. Update & Distribution Strategy

### 7.1 Signing
- Gunakan **satu keystore yang konsisten** untuk semua rilis. Ini wajib — kalau keystore beda, APK update tidak bisa install menimpa versi lama (user harus uninstall dulu, dan **kehilangan data lokal** karena §5 local-only). Simpan keystore aman di luar repo publik.

### 7.2 Distribusi
- **Source code** → GitHub, repository milik tim Winatra (privat atau publik sesuai kebutuhan §8).
- **APK rilis** → disebar manual lewat grup Telegram Winatra. Setiap rilis idealnya disertai changelog singkat di pesan Telegram, sinkron dengan `announcement` di remote config.
- Tidak ada distribusi ke Play Store — jadi tidak perlu optimasi untuk kebijakan Play Store (Cline tidak perlu khawatir soal itu).

### 7.3 Alur Update (ringkasan)
```
App dibuka
  → fetch remote config (timeout 4s, fallback ke cache)
  → bandingkan app_version lokal vs app_version di config
  → bandingkan extractor version lokal vs min_extractor_version
      ├─ kalau outdated & force_update=true → blokir, arahkan ke update_url
      ├─ kalau outdated & force_update=false → tampilkan banner "update tersedia", app tetap jalan
      └─ kalau up to date → lanjut normal, tampilkan announcement (kalau ada)
```

---

## 8. About Section & Transparansi (Basis Legal Praktis)

NewPipe berlisensi GPLv3 — karena URMIX adalah derivative work, klausa transparansi di About section (dari draft awal) **wajib dipertahankan**, dan source code harus tetap dapat diakses (§7.2) sebagai bentuk kepatuhan lisensi:

> "URMIX dikembangkan oleh WINATRA.
> Aplikasi ini menggunakan fondasi arsitektur dan engine open-source dari NewPipe Core.
> WINATRA merancang ulang seluruh pengalaman UI/UX, tata kelola notifikasi, serta mengoptimalkan pemutaran fokus audio bergaya Spotify."

Task untuk agent: pastikan file lisensi asli NewPipe (LICENSE, GPLv3) tetap ada di repo, dan halaman About punya link ke source code repo.

---

## 9. App Identity & Permissions

- Package name baru (bukan `org.schabi.newpipe` bawaan) — sarankan: `com.winatra.urmix`.
- Icon, nama app, dan seluruh string resource yang menyebut "NewPipe" sebagai product name diganti ke "URMIX" (kecuali di About section §8 yang memang harus menyebut kredit).
- Permission yang dibutuhkan:
  - `INTERNET` — extraction & remote config
  - `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (Android 14+) — playback service §4.1
  - `POST_NOTIFICATIONS` (Android 13+) — media controls & donation notif
  - `WAKE_LOCK` — playback tidak putus saat screen off

---

## 10. Testing & QA Checklist (Sebelum Setiap Rilis ke Telegram)

Karena tidak ada Play Store crash reporting otomatis dan tim kecil/zero-budget, checklist manual ini wajib dijalani tiap rilis:

- [ ] Playback jalan normal di background (screen off, app di-swipe dari recents tapi service tetap hidup)
- [ ] Audio focus benar (di-duck/pause saat ada telepon masuk)
- [ ] Extraction fallback teruji (coba video yang audio-only stream-nya tidak tersedia)
- [ ] Force update dialog benar-benar blocking, soft update tidak mengganggu
- [ ] Remote config fetch gagal → app tetap bisa dipakai (pakai cache)
- [ ] Export lalu import backup di device lain → data identik
- [ ] APK baru bisa install menimpa versi lama tanpa uninstall (cek signing §7.1)
- [ ] Notifikasi donasi muncul maksimal 1x/hari, bisa di-dismiss

---

## 11. Implementation Roadmap (Urutan Kerja untuk Agent)

1. **Fase 1 — Fondasi:** cut upstream network (§1.1), rebrand identity (§2, §9), setup RemoteConfigRepository dasar (§1.2 tanpa fitur lanjutan dulu).
2. **Fase 2 — Playback Core:** background service, MediaSession, audio-only extraction + fallback (§4).
3. **Fase 3 — UI Reskin:** mini player, full player, carousel yang reuse data NewPipe (§3).
4. **Fase 4 — Library & Backup:** Room DB local library, export/import (§5).
5. **Fase 5 — Monetization & Update Loop:** donation worker (§6), force/soft update lengkap dengan `min_extractor_version` (§1.2, §7.3).
6. **Fase 6 — QA & Rilis:** jalankan checklist §10, build signed APK, publish ke GitHub + Telegram.
7. **Backlog v2 (tidak wajib di v1):** lyrics, equalizer, download offline, podcast source yang lebih canggih.

---

*End of blueprint. Dokumen ini adalah acuan tunggal — kalau ada perubahan keputusan produk, update dokumen ini dulu sebelum lanjut instruksikan agent.*
