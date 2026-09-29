# URMIX Release Checklist — `v1.0.0` (FASE 6 §10 + blueprint §10)

> Komit rilis: `08fade509` — `feat(release): FASE 6 §6/§7/§10`
> Pipeline rilis: `.github/workflows/release.yml` (trigger: push tag `v*`,
> fallback manual `workflow_dispatch` + input `tag`).

## A. Push state (`Winatra/urmix`)

| Item | Expected remote state | Verified |
|---|---|---|
| `main` | commit `08fade509` | ☐ via `git ls-remote urmix` |
| tag `v1.0.0` | points to `08fade509` | ☐ via `git ls-remote urmix` |
| `build.yml` (push→main) | run green | ☐ Actions tab |
| `ci.yml` (push→main) | run green | ☐ Actions tab |
| `release.yml` (push tag `v1*` / dispatch) | run green, APK signed + published | ☐ Actions tab |

> Preflight non-interaktif gagal (`repository not found` / 401): berarti
> perlu sign-in GitHub interaktif sebagai `winatra`
> (`gh auth login -h github.com` atau Credential Manager). Setelah auth OK,
> jalankan: `git push urmix main` → `git push urmix v1.0.0` →
> `git ls-remote urmix`.

## B. GitHub Secrets (7) — wajib ada di repo `Winatra/urmix`

| # | Secret | Dipakai di | Status |
|---|---|---|---|
| 1 | `KEYSTORE_BASE64` | `release.yml` → restore `.jks` | ☐ |
| 2 | `KEYSTORE_PASSWORD` | `release.yml` → `storePassword` | ☐ |
| 3 | `KEY_ALIAS` | `release.yml` → `keyAlias` | ☐ |
| 4 | `KEY_PASSWORD` | `release.yml` → `keyPassword` | ☐ |
| 5 | `SIGNER_SHA256_HEX` | `release.yml` → pin signer §7.1 (gagal publish bila mismatch) | ☐ |
| 6 | `TELEGRAM_BOT_TOKEN` | `release.yml` → kirim APK ke Telegram | ☐ |
| 7 | `TELEGRAM_CHAT_ID` | `release.yml` → `chat_id` tujuan | ☐ |

Cek: repo **Settings → Secrets and variables → Actions**. Catatan §7.1:
APK release harus ditandatangani keystore yang SAMA agar update menimpa
instalasi lama tanpa uninstall; fingerprint diverifikasi otomatis oleh step
`Verify signature pins the §7.1 keystore`.

## C. Supabase (`urmix_config`) — payload siap insert

- File: `supabase/urmix_config_seed.sql` (tabel `announcement` + `donation`
  dalam bentuk `jsonb`; kolom list podcast `text[]`).
- Nilai ethics/pinned endpoint di `RemoteConfigRepository`:
  - `REMOTE_CONFIG_URL = https://winatra.supabase.co/rest/v1/urmix_config?select=*`
  - `DEFAULT_UPDATE_URL = https://winatra.com/urmix/download`
- App membaca **baris pertama** sebagai objek JSON flat; validasi URL hanya
  menerima `http(s)://`; fetch gagal → cache lokal dipakai, app tetap jalan
  (kecuali `shouldBlockStartup()` true → dialog force-update).
- Setelah seed: buka `SplashActivity` → cek banner announcement muncul &
  prompt donasi 1x/hari muncul.

## D. QA fisik di device (blueprint §10) — tanda tangan rilis

- [ ] Playback jalan di background (screen off; swipe dari recents, service hidup)
- [ ] Audio focus benar (duck/pause saat telepon masuk)
- [ ] Extraction fallback teruji (stream tanpa varian audio-only)
- [ ] Force update benar-benar blocking; soft update tidak mengganggu
- [ ] Remote config gagal → app tetap jalan (cache)
- [ ] Export → import backup di device lain → data identik (FASE 4)
- [ ] APK baru install menimpa versi lama tanpa uninstall (signing §7.1)
- [ ] Notifikasi donasi max 1x/hari, bisa di-dismiss
- [ ] GitHub Release `v1.0.0` ada + asset `URMIX_v1.0.0.apk` + changelog
- [ ] File APK yang SAMA terkirim ke Telegram group (caption = changelog)
