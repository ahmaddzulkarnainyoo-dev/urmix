# URMIX v1.0.2 — Closed Plane Execution Plan (UI Integration + Smoke Test)

> Status: PLAN TERTUTUP REVISI-1 (2026-10-06). Satu-satunya sumber kebenaran eksekusi.
> Aturan: Batch A (Langkah 0-3) -> STOP + assembleDebug + lapor. Batch B (Langkah 4-7) -> STOP + assembleDebug + lapor. Langkah 8 MANUAL user.
> Dilarang: rewrite penuh SVG; ubah ic_launcher_background (tetap #121212).
> Branch: `main`. Remote: `urmix`.

## Tabel Komit

| # | Judul | File | Pesan commit |
|---|---|---|---|
| 0 | Fix aset player icons | `aset_winatra/urmix_player_icons.svg` | `fix(assets): repair corrupted player icons svg` |
| 1 | Blueprint v1.0.2 | `URMIX_Winatra_Blueprint_v2.md` | `docs(blueprint): v1.0.2 revisions and audio-first specification update` |
| 2 | Deep Blue #1A56DB | `colors.xml`, `colors_services.xml`, `Color.kt` | `style(theme): migrate accent to Deep Blue #1A56DB` |
| 3 | Audio-First Player | `fragment_video_detail.xml`, `player.xml`, `VideoPlayerUi.java` | `feat(player): audio-first 9:16 layout with video toggle` |
| 4 | Homefeed reorder | `fragment_home.xml`, `HomeDataLoader.kt` | `feat(home): reorder feed Trending MadeForYou Podcasting cap 10` |
| 5 | About team + URL | `AboutPage.kt`, `Constants.kt`, `strings.xml` | `feat(about): WINATRA team header IG and fallback URLs` |
| 6 | i18n Indonesia | `values-in/strings.xml` | `feat(i18n): complete Bahasa Indonesia strings for v1.0.2` |
| 7 | Bump + tag + release | `ProjectConfig.kt`, `changelogs/102.txt` | `chore(release): bump to 1.0.2 (code 102)` + tag `v1.0.2` |

## Langkah 0 — Perbaikan Aset Wajib

1. Baca `aset_winatra/urmix_player_icons.svg` (3017 byte, Row 2 hilang).
2. Sisipkan sebelum `</svg>` blok Row 2 `#1A56DB` (grup translate(0,48), 7 ikon di x=0/48/96/144/192/240/288 — path lengkap ada di chat persetujuan).
3. Verifikasi (WAJIB lolos semua, assert angka spesifik — gagal satu = STOP, jangan commit):
```powershell
python -c "import xml.etree.ElementTree as ET; ET.parse('aset_winatra/urmix_player_icons.svg'); print('XML-OK')"
if ((Select-String -Path aset_winatra/urmix_player_icons.svg -Pattern '#1A56DB' | Measure-Object).Count -lt 1) { throw 'ASSERT GAGAL: #1A56DB < 1 match' }
if ((Select-String -Path aset_winatra/urmix_player_icons.svg -Pattern '</svg>' | Measure-Object).Count -ne 1) { throw 'ASSERT GAGAL: </svg> != 1 match' }
```
4. Commit:
```powershell
git add aset_winatra/urmix_player_icons.svg
git commit -m "fix(assets): repair corrupted player icons svg"
```

## Langkah 1 — Commit 1 Blueprint

```powershell
git status --short; git diff --stat
git add URMIX_Winatra_Blueprint_v2.md
git commit -m "docs(blueprint): v1.0.2 revisions and audio-first specification update"
```

## Langkah 2 — Commit 2 Deep Blue #1A56DB

1. `colors.xml`: `urmix_accent`, `dark_settings_accent_color`, `urmix_progress_played` `#1DB954` -> `#1A56DB`; `light_settings_accent_color` -> `#1A56DB`; tambah `urmix_accent_pressed #174EA6`. PENGECUALIAN: `ic_launcher_background` TETAP `#121212` (jangan diubah).
2. `colors_services.xml`: 5 service light -> `#1A56DB`, dark -> `#174EA6`.
3. `Color.kt`: `primaryDark/primaryContainerDark/tertiaryDark/inversePrimaryDark` -> `0xFF1A56DB`; `logoBackground` -> `0xFF1A56DB`; `onPrimary*` -> putih; family light maroon -> biru.
4. Verifikasi nol-sisa (wajib nol):
```powershell
Select-String -Path app/src/main/res/values/colors.xml,app/src/main/res/values/colors_services.xml -Pattern '1DB954|CD201F|e53935|992722'
Select-String -Path shared/src/commonMain/kotlin/com/winatra/urmix/shared/app/theme/Color.kt -Pattern '1DB954|CD201F|e53935|992722'
```
5. Commit:
```powershell
git add app/src/main/res/values/colors.xml app/src/main/res/values/colors_services.xml shared/src/commonMain/kotlin/com/winatra/urmix/shared/app/theme/Color.kt
git commit -m "style(theme): migrate accent to Deep Blue #1A56DB"
```

## Langkah 3 — Commit 3 Audio-First Player

1. `fragment_video_detail.xml`: `detail_thumbnail_image_view` centerCrop 9:16; `visibility=gone` pada `detail_view_count_view`, semua `detail_thumbs_*`, `detail_controls_popup/background/download`, `detail_secondary_control_panel`, `view_pager`, `tab_layout`, `detail_meta_info_separator/text_view`. Sederhanakan uploader: hanya thumbnail kecil + `detail_uploader_text_view`. Pertahankan `detail_video_title_view`, `overlay_layout`, `overlay_like_button`.
2. `player.xml`: tambah `album_art_full` (centerCrop 9:16, bawah surfaceView); `surfaceView/surfaceForeground/subtitleView` default GONE; tambah `btn_video_toggle` 40dp. Pertahankan id seekbar + Prev/Play/Next + repeat/shuffle/queue.
3. `VideoPlayerUi.java`: flag `audioFirst=true`; toggle swap visibility saja (jangan recreate player); load art via Coil dari thumbnailUrl; guard `isAdded()` pola Phase C.
4. Commit:
```powershell
git add app/src/main/res/layout/fragment_video_detail.xml app/src/main/res/layout/player.xml app/src/main/java/com/winatra/urmix/player/ui/VideoPlayerUi.java
git commit -m "feat(player): audio-first 9:16 layout with video toggle"
```

## Langkah 4 — Commit 4 Homefeed

1. `fragment_home.xml`: urut Quick Play -> Trending Audio -> Made For You -> Podcasting.
2. `HomeDataLoader.kt`: podcast `take(10)`; Trending sort audio-leaning; fallback channel bila kiosk kosong.
3. Commit:
```powershell
git add app/src/main/res/layout/fragment_home.xml app/src/main/java/com/winatra/urmix/home/HomeDataLoader.kt
git commit -m "feat(home): reorder feed Trending MadeForYou Podcasting cap 10"
```

## Langkah 5 — Commit 5 About Team + URL

1. `AboutPage.kt`: kartu header WINATRA `@ahmddzlkrn`, `@imamyahyaaaaa` + tombol IG via ShareUtils.
2. `Constants.kt`: `URL_GITHUB` -> repo aktif; `URL_WEBSITE/URL_FAQ/URL_PRIVACY` -> `https://winatra.com` (placeholder, tombol di-hide sampai URL asli siap); `URL_DONATION` = `https://saweria.co/winatra`.
3. Strings EN+ID: `winatra_team_title/members/instagram`.
4. Commit:
```powershell
git add shared/src/commonMain/kotlin/com/winatra/urmix/shared/app/screen/about/AboutPage.kt shared/src/commonMain/kotlin/com/winatra/urmix/shared/app/Constants.kt app/src/main/res/values/strings.xml
git commit -m "feat(about): WINATRA team header IG and fallback URLs"
```



## Langkah 6 — Commit 6 i18n Indonesia

1. `values-in/strings.xml` tambah/perbaiki: `tab_urmix_home=Beranda`, greeting Pagi/Siang/Malam, `home_made_for_you=Dibuat Untukmu`, `home_trending_audio=Audio Trending`, `home_podcasting=Podcast`, `like_song=Suka`, `donation_*`, `soft_update_banner_*`, `backup_*` penuh, `winatra_team_*`; perbaiki `title_activity_about` -> `Tentang URMIX`, donasi -> WINATRA/Saweria.
2. Verifikasi: diff key URMIX values vs values-in = 100 persen terisi.
3. Commit:
```powershell
git add app/src/main/res/values-in/strings.xml
git commit -m "feat(i18n): complete Bahasa Indonesia strings for v1.0.2"
```

## Langkah 7 — Verifikasi Akhir

```powershell
git status --short; git log --oneline -9
Select-String -Path app/src/main/res/values/colors.xml,app/src/main/res/values/colors_services.xml -Pattern '1DB954|CD201F' | Measure-Object
git push urmix main
gh run list --repo ahmaddzulkarnainyoo-dev/urmix --limit 3
```
Lolos bila: tree bersih, CI hijau.

## Langkah 8 — Rilis v1.0.2

```powershell
git add buildSrc/src/main/kotlin/ProjectConfig.kt fastlane/metadata/android/en-US/changelogs/102.txt
git commit -m "chore(release): bump to 1.0.2 (code 102)"
git push urmix main
git tag -a v1.0.2  -m "URMIX v1.0.2 - Deep Blue audio-first + Bahasa Indonesia"
git push urmix v1.0.2
gh run list --repo ahmaddzulkarnainyoo-dev/urmix --limit 3
gh release view v1.0.2 --repo ahmaddzulkarnainyoo-dev/urmix
```
Kriteria: release.yml 13/13 hijau, APK + Telegram keluar, SHA = SIGNER_SHA256_HEX.

## Loop Konfirmasi Tertutup REVISI-1

Eksekusi dibagi 2 batch + 1 manual:
- Batch A: Langkah 0-3 -> STOP, `git push urmix main` + `./gradlew assembleDebug`, lapor error, tunggu konfirmasi.
- Batch B: Langkah 4-7 -> STOP, `git push urmix main` + `./gradlew assembleDebug`, lapor error, tunggu konfirmasi.
- Langkah 8 (di bawah): MANUAL user, tidak auto-execute.

## Langkah 8 — Rilis v1.0.2 [MANUAL USER — JANGAN AUTO-EXECUTE]
