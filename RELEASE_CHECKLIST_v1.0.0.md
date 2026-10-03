# URMIX Release Checklist — `v1.0.0` (FASE 6 §10 + blueprint §10 + Phase C)

> Komit rilis: `70d06cc4a` (komit rilis awal; tag `v1.0.0` dipindahkan ke tip `main` —
> lihat tabel §A — agar tag == main == CI hijau, sebelum ada GitHub Release/asset).
> (`feat(library): Like button + Liked Songs manager`, manifest cleanup).
> Riwayat: `d932f439b` (retag v1.0.0) → `f98bbbfbc` (harden Like toggle)
> → `70d06cc4a` (helper lokal gitignored) → `3fddb1b48` (fix gaya ktlint/checkstyle)
> → docs (dokumen checklist ini) → `10743c21f` (fix schema Room §A1 + gitignore
> crash dump; **CI hijau total**) → `1d99eb2b0` → `27ed7026a` → `87d2b91f7`
> (docs §A/§B1: bukti run tip + re-validasi probe + paritas nama secret).
> Tag `v1.0.0` = `10743c21f`; tip `main` saat checklist ini ditulis =
> `87d2b91f7` (komit dokumen ini menambah satu komit docs lagi di atasnya).
> Komit dokumentasi setelah tag memindahkan tip `main`, tetapi tag rilis
> `v1.0.0` **tetap** `10743c21f`: `release.yml` selalu checkout **tag**,
> bukan tip `main`, jadi isi rilis tidak berubah.
> Pipeline rilis: `.github/workflows/release.yml` (trigger: push tag `v*`,
> fallback manual `workflow_dispatch` + input `tag`).
> Catatan filter: `ci.yml` meng-`paths-ignore` `doc/**` dan `README.md`,
> `build.yml` **tanpa** `paths-ignore`, dan `RELEASE_CHECKLIST_v1.0.0.md`
> tidak termasuk daftar abaikan — jadi komit dokumen ini tetap memicu
> Build + CI penuh (dipakai sebagai re-validasi, bukan regresi).

## A0. Akar masalah push lama (RESOLVED) — shallow clone

- Gejala: `remote: fatal: did not receive expected object 14401c532ff…` +
  `error: remote unpack failed: index-pack failed` pada **semua** percobaan push
  (HTTPS, `protocol.version=0`, `--no-thin`, clone bersih, repack, pump-pack
  terisolasi). Objek `14401c…` **tidak ada** di lokal (`git cat-file -t 14401c…`
  → *could not get object info*), jadi bukan korupsi pack lokal.
- Penyebab: `e:\urmix` adalah **shallow clone** — `.git/shallow` =
  `00acf2f318b…` (NewPipe master, "Bump NewPipe version to 0.29.1"); commit itu
  punya `parent 14401c532ff…` yang belum pernah diunduh. Saat push riwayat
  shallow, receive-pack GitHub merekonstruksi ancestry, meminta objek parent
  boundary tsb, lalu gagal. Bukti pembanding: push repo **non-shallow**
  1-komit (`tmp-mini-probe`) **BERHASIL** di repo yang sama.
- Perbaikan (terbukti 2026-10-01): `git fetch --unshallow origin` — instan,
  karena objek NewPipe sudah ada lokal dan hanya penanda shallow yang salah —
  lalu push bertahap: base `00acf2f` → `main` → tag `v1.0.0`.
- **Preflight wajib**: `git rev-parse --is-shallow-repository` harus `false`
  sebelum push. `post_auth_push.bat` (lokal, gitignored) sudah otomatis
  menjalankan `git fetch --unshallow origin` bila mendeteksi repo shallow.

## A. Push state (`ahmaddzulkarnainyoo-dev/urmix`)

| Item | Expected remote state | Verified |
|---|---|---|
| `main` | tip `87d2b91f7` (`70d06cc4a` → `3fddb1b48` → `10743c21f` fix schema Room §A1 → docs `1d99eb2b0` → `27ed7026a` → docs `87d2b91f7`) | ✅ `git ls-remote urmix`, 2026-10-03 |
| tag `v1.0.0` | = tip `main` = `10743c21f` (dipindah via `git tag -f` + `push --force`, sebelum GitHub Release/asset ada) | ✅ `git ls-remote urmix` |
| default branch | `main` (bukan `tmp-mini-probe`) | ✅ `gh repo edit --default-branch main` |
| branch probe `tmp-mini-probe` | dihapus dari remote | ✅ `git push urmix --delete …` |
| `build.yml` (push→main) | run green | ✅ run `36897778394` (komit `3fddb1b48`) · ✅ run `36907319052` (komit `10743c21f`; job *Build URMIX Android app (debug)* ✅ 3m27s, artifact `urmix-apk`) · ✅ run `36917248458` (komit `1d99eb2b0`, ✅ 4m16s) · ✅ run `36917619584` (komit `27ed7026a`, ✅ 3m39s) · ✅ run `37116714843` (komit `87d2b91f7`, ✅ 3m49s) |
| `ci.yml` (push→main) | run green | ✅ run `36907318999` (komit `10743c21f`) — **FULLY GREEN**: `build-and-test-jvm` ✅ 8m39s (ktlint + checkstyle + unit) · `test-android (35, x86_64)` ✅ 6m2s · `test-android (23, x86)` ✅ 7m25s (emulator `Boot completed in 32436 ms`, `BUILD SUCCESSFUL in 4m 30s`, 25/25 test instrumented). Akar masalah = schema Room (lihat §A1), bukan hanya flake emulator · ✅ run `36917248171` (komit `1d99eb2b0`, ✅ 7m2s) · ✅ run `36917619614` (komit `27ed7026a`, ✅ 7m8s: `build-and-test-jvm` ✅ 7m2s · `test-android (23)` ✅ 6m33s · `test-android (35)` ✅ 6m35s · `sonar` skipped tanpa `SONAR_TOKEN`) — jadi seluruh komit docs ikut tervalidasi penuh · ✅ run `37116714857` (komit `87d2b91f7`, ✅ 8m22s: `build-and-test-jvm` ✅ 8m19s · `test-android (23)` ✅ 5m49s · `test-android (35)` ✅ 5m8s · `sonar` skipped) |
| `release.yml` (push tag `v1*` / dispatch) | run green, APK signed + published | ❌ run `36907345654` (tag `v1.0.0` @ `10743c21f`, 2026-10-02) — gagal di step *Restore release keystore from secrets*: `missing KEYSTORE_BASE64`, keempat env keystore kosong (`KEYSTORE_BASE64`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`), `Process completed with exit code 1`. Blocker tunggal = §B (0/7 secret), bukan kode |

> Fix gaya `3fddb1b48` (unblock Build/CI): ktlint `PlaylistStreamDAO.kt` (`@Query` wrapping) +
> checkstyle `LikedSongsManager` (`@param context` / `streamUrl` / `info`).
> Divalidasi lokal: `gradlew :app:runCheckstyle :app:runKtlint` → BUILD SUCCESSFUL.
>
> Bukti push sukses: `* [new branch] 00acf2f31 -> main` (base: 146.772 objek /
> 12.242 komit NewPipe master, ~83 MiB) → `00acf2f31..70d06cc4a main -> main`
> (15 komit URMIX, 598 delta) → `* [new tag] v1.0.0 -> v1.0.0`.
> Auth sudah tersedia via `gh`, jadi `gh auth login` interaktif tidak perlu.

> Preflight non-interaktif gagal (`repository not found` / 401): perlu sign-in
> GitHub interaktif (`gh auth login -h github.com` atau Credential Manager).
> Setelah auth OK: `post_auth_push.bat` (cek shallow → push base bila perlu →
> push `main` → push `v1.0.0` → `git ls-remote`). Komit dokumentasi setelah
> `70d06cc4a` tidak mengubah tag rilis.

## A1. Akar masalah `test-android` (RESOLVED) — schema Room tidak ikut rename paket

- Gejala (run `36901004258`, emulator API 35 x86_64 **berhasil boot**): ketiga test
  `DatabaseMigrationTest.migrateDatabaseFrom{2to3,7to8,8to9}` gagal dengan
  `java.io.FileNotFoundException: Cannot find the schema file in the assets folder … Missing file: com.winatra.urmix.database.AppDatabase/{2,8}.json`.
- Penyebab: `MigrationTestHelper` membaca asset `"<canonicalName AppDatabase>/<versi>.json"`.
  Saat FASE 2 memindahkan `AppDatabase` ke paket `com.winatra.urmix.database`, ksp
  hanya mengekspor `9.json` ke direktori baru, sedangkan schema historis
  (`2.json`–`8.json`) tetap di direktori lama
  `app/schemas/org.schabi.newpipe.database.AppDatabase/` → tak terjangkau helper.
- Perbaikan: `git mv app/schemas/org.schabi.newpipe.database.AppDatabase/{2..8}.json`
  → `app/schemas/com.winatra.urmix.database.AppDatabase/`, lalu hapus `9.json`
  upstream yang jadi orphan. Kini 2–9 berada di satu direktori sesuai paket kelas DB.
- Bukti statis: `9.json` lama vs baru identik secara semantik (fork hanya
  menghilangkan field default `"notNull": false` / `"foreignKeys": []`); 12 tabel
  v9 sama; kolom v8 (`playlists`: `uid,name,is_thumbnail_permanent,thumbnail_stream_id`,
  `remote_playlists`: `service_id,name,url,…`) cocok dengan `insert` di test;
  seluruh JSON valid (punya `createSql` per entitas).
- Validasi runtime: **CI** (lihat kolom `ci.yml` §A). Build lokal
  `:app:connectedDebugAndroidTest` tidak bisa dipakai di mesin ini: RAM 3,7 GB
  (0,3 GB bebas) → daemon Gradle (`org.gradle.jvmargs=-Xmx4096M`) di-kill OS
  (`hs_err_pid*.log`; kini di-gitignore).
- **Validasi CI 2026-10-02** (run `36907318999` @ `10743c21f`): kedua leg
  `test-android` hijau — API 35 (x86_64) ✅ 6m2s, API 23 (x86) ✅ 7m25s;
  emulator `Boot completed in 32436 ms`, `BUILD SUCCESSFUL in 4m 30s`
  (25/25 test instrumented, termasuk `DatabaseMigrationTest` 2→3 / 7→8 / 8→9).
  `build-and-test-jvm` ✅ 8m39s; `sonar` *skipped* (tanpa `SONAR_TOKEN`).
  Build debug paralel: run `36907319052` ✅ (3m27s).

## B. GitHub Secrets (7) — wajib ada di repo `ahmaddzulkarnainyoo-dev/urmix`

| # | Secret | Dipakai di | Status |
|---|---|---|---|
| 1 | `KEYSTORE_BASE64` | `release.yml` → restore `.jks` | ☐ |
| 2 | `KEYSTORE_PASSWORD` | `release.yml` → `storePassword` | ☐ |
| 3 | `KEY_ALIAS` | `release.yml` → `keyAlias` | ☐ |
| 4 | `KEY_PASSWORD` | `release.yml` → `keyPassword` | ☐ |
| 5 | `SIGNER_SHA256_HEX` | `release.yml` → pin signer §7.1 (gagal publish bila mismatch) | ☐ |
| 6 | `TELEGRAM_BOT_TOKEN` | `release.yml` → kirim APK ke Telegram | ☐ |
| 7 | `TELEGRAM_CHAT_ID` | `release.yml` → `chat_id` tujuan | ☐ |

Status terverifikasi 2026-10-02: **0/7** — `gh secret list --repo
ahmaddzulkarnainyoo-dev/urmix` kosong dan
`gh api …/actions/secrets --jq .total_count` = `0`.

Cek: repo **Settings → Secrets and variables → Actions**. Catatan §7.1:
APK release harus ditandatangani keystore yang SAMA agar update menimpa
instalasi lama tanpa uninstall; fingerprint diverifikasi otomatis oleh step
`Verify signature pins the §7.1 keystore`.

### B1. Jalur pembuatan secret (sudah diuji lokal 2026-10-02)

Skrip generator (di **luar** repo, tidak ikut commit):
`C:\Users\ahmad\.urmix\release\setup_release_keystore.ps1`

Perilaku: preflight `keytool` + `JAVA_HOME` → password (input tersembunyi) →
`keytool -genkeypair` (RSA 4096 / JKS / 3650 hari / alias `urmix-release`) →
self-check entri `PrivateKeyEntry` → `-exportcert` + SHA-256 DER →
silang-cek `keytool -printcert` → tulis `e:\urmix\keystore.properties`
(git-ignored) → `.jks.b64` satu baris → `gh secret set` 7 secret →
verifikasi `gh secret list` → checklist backup.

**Bukti self-test lokal (folder `%TEMP%`, `-SkipSecrets`): `exit 0`.**
Rantai yang diverifikasi: entri `PrivateKeyEntry` ✅;
`SIGNER_SHA256_HEX` = `286570549e63e87460c07d0a07c3f489b794d6b93eaa96cca1e2302aa3dfed31`
cocok dengan `keytool -printcert`
(`28:65:70:54:…:DF:ED:31`) ✅; `keystore.properties` ✅; base64 5100 char ✅.

**Bukti pin `release.yml` cocok** (step *Verify signature pins the §7.1
keystore*, baris `grep -qi "$SIGNER_SHA256_HEX" "$RUNNER_TEMP/certs.txt"`):
APK debug CI (`urmix-apk` @ `10743c21f`) ditandatangani ulang dengan kunci uji
sekali-pakai, lalu `apksigner verify --print-certs` (build-tools 36.0.0)
mencetak:

```text
Signer #1 certificate SHA-256 digest: 5361bd49eb9f347f4af76e3126d4f0f29a9b32f8621ddf25ffda41725dbec661
```

nilai itu **identik** dengan hash DER hasil ekspor
(`(Get-FileHash cert.der -Algorithm SHA256).Hash.ToLowerInvariant()`).
Kesimpulan: apksigner mencetak hex huruf kecil **tanpa titik dua**, sama
persis dengan spesifikasi `doc/RELEASE.md` §2 → `grep -qi` pasti cocok.
(Nilai `5361bd49…`/`2865705…` di atas hanya kunci uji, bukan kunci rilis.)

**Re-validasi probe 2026-10-02 (setelah pengerasan skrip):** APK debug CI yang
sama ditandatangani ulang dengan kunci uji sekali-pakai **kedua**
(`SIGNER_SHA256_HEX` =
`716c24bea6e3827208ebb61a91c8fa5d87284bf2f02ecfa75427c0adb29f2695`) → skrip
`exit 0`, baris `Signer #1 certificate SHA-256 digest: 716c24be…b29f2695`
terbaca, self-check menyatakan `grep -qi` **WILL match**, dan tidak ada sisa
file probe. Keluaran APK re-sign kini ditulis ke `%TEMP%` (bukan di sebelah
APK masukan) supaya lokasi APK yang read-only tidak menggagalkan probe.

**Bukti paritas nama secret (2026-10-03):** skrip kini menegakkan
`$expectedSecrets` (7 nama yang dideklarasikan) terhadap daftar yang
benar-benar didaftarkan (`Compare-Object`; meleset → `throw`), dan langkah
verifikasi `gh secret list` memakai `$expectedSecrets` (bukan kunci hashtable).
Diuji programatik: nama di skrip == nama di `release.yml` (pola
`secrets.<NAMA>` yang diekstrak dari workflow) → **7 vs 7, drift = 0**; uji
negatif `Compare-Object` (satu nama salah) → 2 selisih terdeteksi; self-test
skrip ulang (`-SkipSecrets`) → `exit 0`.

Alternatif tanpa skrip: `KEYSTORE_BASE64` = isi `.jks.b64`, sisanya nilai
literal (`KEY_ALIAS` = `urmix-release`). Verifikasi nama kapan saja:
`gh secret list --repo ahmaddzulkarnainyoo-dev/urmix` (harus 7 baris).

Jika step *Verify signature pins* gagal setelah rilis pertama: salin token
persis dari `certs.txt` pada log run itu, lalu
`gh secret set SIGNER_SHA256_HEX --repo ahmaddzulkarnainyoo-dev/urmix --body='<token>'`.

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

## D2. QA Phase C — mini-player Like/Unlike (blueprint §5.1)

- [ ] Tap heart di mini-player → toast "Added to Liked Songs", icon jadi hijau
- [ ] Tap lagi → toast "Removed from Liked Songs", icon kembali putih
- [ ] Pindah track → status heart refresh sesuai state liked track baru
  (`updateOverlayData` → `refreshOverlayLikeButton`, stale-check by URL)
- [ ] Track yang di-like muncul di library "Liked Songs" (UID prefs
  `urmix_liked_songs_playlist_uid`, self-heal by name)
- [ ] "Liked Songs" ikut tereksport sebagai `liked_tracks` di
  `urmix_backup_YYYYMMDD.json` (FASE 4 §5.2)
- [ ] DB error saat toggle → toast/snackbar, player tidak crash, icon tidak
  berubah palsu (Maybe empty path + `isAdded()` guard)
