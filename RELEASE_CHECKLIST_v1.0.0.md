# URMIX Release Checklist — `v1.0.0` (FASE 6 §10 + blueprint §10 + Phase C)

> Komit rilis: `7f7f6ffbe` (`feat: P0 remote config`, versi 1.0.1, code 101;
> tag `v1.0.1` menunjuk commit ini; GitHub Release `v1.0.1` TERBIT 2026-10-05,
> run `37275407682`, 13/13 hijau; APK `URMIX_v1.0.1.apk` 11.522.797 byte,
> sha256 `c8bde216…eee1add6`, Telegram msg 2138).
> Riwayat rilis lama: `6fa4fc3e5` (v1.0.0, run `37133111052`, Telegram msg 2131,
> sha256 `4bed064d…5cde`; lihat §D). Komit P0 remote config: `d7ac215df`
> (dilipat ke `7f7f6ffbe`).

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
| `main` | tip `7f7f6ffbe` (P0 remote config + versi 1.0.1/code 101) | ✅ `git log`, 2026-10-05; komit docs menambah komit di atasnya (hash tip tidak dikejar lagi) |
| tag `v1.0.1` | = `7f7f6ffbe` (annotation 2026-10-05; rilis TERBIT, run `37275407682`, 13/13 hijau; APK `URMIX_v1.0.1.apk` 11.522.797 byte sha256 `c8bde216…eee1add6`, Telegram msg 2138) | ✅ `git ls-remote urmix` + log run |
| tag `v1.0.0` lama | = `6fa4fc3e5` (run `37133111052`, APK 11.522.829 byte sha256 `4bed064d…5cde`, Telegram msg 2131) | ✅ arsip, lihat §D |
| default branch | `main` (bukan `tmp-mini-probe`) | ✅ `gh repo edit --default-branch main` |
| branch probe `tmp-mini-probe` | dihapus dari remote | ✅ `git push urmix --delete …` |
| `build.yml` (push→main) | run green | ✅ run `36897778394` (komit `3fddb1b48`) · ✅ run `36907319052` (komit `10743c21f`; job *Build URMIX Android app (debug)* ✅ 3m27s, artifact `urmix-apk`) · ✅ run `36917248458` (komit `1d99eb2b0`, ✅ 4m16s) · ✅ run `36917619584` (komit `27ed7026a`, ✅ 3m39s) · ✅ run `37116714843` (komit `87d2b91f7`, ✅ 3m49s) · ✅ run `37131855015` (komit `67972f038`, ✅ 7m31s) · ✅ run `37133108153` (komit `6fa4fc3e5`, ✅ 2m57s) |
| `ci.yml` (push→main) | run green | ✅ run `36907318999` (komit `10743c21f`) — **FULLY GREEN**: `build-and-test-jvm` ✅ 8m39s (ktlint + checkstyle + unit) · `test-android (35, x86_64)` ✅ 6m2s · `test-android (23, x86)` ✅ 7m25s (emulator `Boot completed in 32436 ms`, `BUILD SUCCESSFUL in 4m 30s`, 25/25 test instrumented). Akar masalah = schema Room (lihat §A1), bukan hanya flake emulator · ✅ run `36917248171` (komit `1d99eb2b0`, ✅ 7m2s) · ✅ run `36917619614` (komit `27ed7026a`, ✅ 7m8s: `build-and-test-jvm` ✅ 7m2s · `test-android (23)` ✅ 6m33s · `test-android (35)` ✅ 6m35s · `sonar` skipped tanpa `SONAR_TOKEN`) — jadi seluruh komit docs ikut tervalidasi penuh · ✅ run `37116714857` (komit `87d2b91f7`, ✅ 8m22s: `build-and-test-jvm` ✅ 8m19s · `test-android (23)` ✅ 5m49s · `test-android (35)` ✅ 5m8s · `sonar` skipped) · ✅ run `37131855010` (komit `67972f038`, ✅ 8m40s) · ✅ run `37133108146` (komit `6fa4fc3e5`, ✅ 8m40s) |
| `release.yml` (push tag `v1*` / dispatch) | run green, APK signed + published | ✅ **run `37133111052`** (tag `v1.0.0` @ `6fa4fc3e5`, 2026-10-03, 5m6s) — **SEMUA STEP HIJAU**: *Restore release keystore from secrets* ✅ · *Build signed release APK* ✅ · *Verify signature pins the §7.1 keystore* ✅ · *Rename APK + resolve changelog* ✅ · *Publish GitHub Release* ✅ · *Publish to Telegram* ✅ · *Upload APK artifact (backup)* ✅. Hasil: GitHub Release `v1.0.0` (`isDraft=false`) + asset `URMIX_v1.0.0.apk` (11.522.829 byte; sha256 `4bed064d…5cde` = unduhan lokal), Telegram `ok:true` `message_id` 2131 di supergroup *WINATRA.OFFICIAL* (`file_name=URMIX_v1.0.0.apk`). Bukti pin: APK rilis diunduh & diverifikasi lokal dengan `apksigner` (build-tools 36.0.0) → `Signer #1 certificate SHA-256 digest: c767c5aa…c3bdb` = `SIGNER_SHA256_HEX` §B persis ✓. Riwayat gagal (sebelum fix, lihat §A2): run `36907345654` (0/7 secret) dan `37131880889` (apksigner tak ada di PATH) |

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

## A2. Akar masalah rilis pertama (RESOLVED) — 2 bug nyata di luar kode fitur

Keduanya **hanya** muncul di jalur rilis (`clean assembleRelease` di runner CI),
tidak pernah di Build/CI debug, jadi lolos dari semua gate sebelumnya.

### A2.1 `Unresolved reference 'BuildConfig'` (release variant) — run `37131880889`

- Gejala: *Build signed release APK* gagal;
  `e: …/shared/src/commonMain/…/TopAppBar.kt:19:37 Unresolved reference 'BuildConfig'`
  (+5 error serupa di `AboutPage.kt`), `CompilationErrorException`.
- Akar masalah: `shared/build.gradle.kts` membuat `BuildConfig.kt` lewat `Sync`
  yang sumbernya `resources.text.fromString(...)`. Sumber TextResource
  dimaterialisasi di dalam direktori build, sedangkan `release.yml` menjalankan
  **`clean assembleRelease`** dalam satu invokasi → berkas sumber itu terhapus
  sebelum eksekusi → task berstatus **`NO-SOURCE`** (tetap "sukses" tetapi tidak
  menghasilkan apa pun) → `commonMain` kehilangan `BuildConfig`. Build/CI debug
  tidak pernah memakai `clean`, jadi selalu hijau.
- Perbaikan (`67972f038`): task menulis berkasnya sendiri di `doLast` dengan
  `outputs.dir(build/generated/kotlin)`, dan `commonMain` memakai
  `buildConfigGenerator.map { it.outputs.files.singleFile }`.
- Bukti reproduksi + verifikasi **lokal** (mesin ini; task-nya ringan sehingga
  aman di RAM 3,7 GB): `gradlew :shared:clean :shared:buildConfigGenerator`
  → **sebelum**: `NO-SOURCE`, berkas tidak ada (`Test-Path` = `False`);
  **sesudah**: task dieksekusi, `BUILD SUCCESSFUL`, berkas ada berisi `package
  com.winatra.urmix.shared.app` / `VERSION_NAME = "1.0.0"` / `APP_NAME =
  "URMIX"`, configuration cache tersimpan tanpa masalah.

### A2.2 `apksigner: command not found` di runner — run `37131880889`

- Gejala: *Build signed release APK* ✅ (APK sudah ditandatangani!), tetapi
  *Verify signature pins the §7.1 keystore* ❌:
  `…/…sh: line 2: apksigner: command not found` →
  `signer fingerprint mismatch — refusing to publish`. Jadi **bukan** mismatch
  sidik jari, murni masalah tooling.
- Akar masalah: `apksigner` ada di `$ANDROID_HOME/build-tools/<versi>/` dan
  **tidak** diekspor ke `PATH` pada image runner GitHub.
- Perbaikan (`6fa4fc3e5`): step pin mencari `apksigner` secara eksplisit
  (`$APKSIGNER` override → `PATH` → `$ANDROID_HOME`/`$ANDROID_SDK_ROOT`/
  `/usr/local/lib/android/sdk`/`$HOME/Android/Sdk`, ambil build-tools terbaru
  via `sort -V`), punya fallback **tanpa SDK** (`keytool -printcert -jarfile`,
  hanya butuh JDK), menerima bentuk bertitik-dua (`tr -d ':'`), dan pemilihan
  berkas APK mengabaikan `*-unsigned`.
- Validasi: `python -c "yaml.safe_load"` → YAML sah (9 step); `bash -n` → 0;
  8/8 tes logika `test_pin_logic.sh` (resolusi SDK ×4 termasuk override dan
  fallback kosong; pencocokan sidik jari ×4 termasuk bentuk keytool bertitik-dua
  serta penolakan kunci yang salah).
- Verifikasi akhir pada rilis nyata (run `37133111052`): step pin ✅, lalu APK
  hasil rilis **diunduh lokal** dan diperiksa `apksigner verify --print-certs`
  (build-tools 36.0.0) → `Signer #1 certificate SHA-256 digest: c767c5aa…c3bdb`
  = `SIGNER_SHA256_HEX` §B → **APK rilis benar ditandatangani keystore §7.1**
  (update akan menimpa instalasi lama tanpa uninstall).

## B. GitHub Secrets (9) — wajib ada di repo `ahmaddzulkarnainyoo-dev/urmix`

| # | Secret | Dipakai di | Status |
|---|---|---|---|
| 1 | `KEYSTORE_BASE64` | `release.yml` → restore `.jks` | ✅ 2026-10-03 |
| 2 | `KEYSTORE_PASSWORD` | `release.yml` → `storePassword` | ✅ 2026-10-03 |
| 3 | `KEY_ALIAS` | `release.yml` → `keyAlias` (`urmix-release`) | ✅ 2026-10-03 |
| 4 | `KEY_PASSWORD` | `release.yml` → `keyPassword` | ✅ 2026-10-03 |
| 5 | `SIGNER_SHA256_HEX` | `release.yml` → pin signer §7.1 (gagal publish bila mismatch) | ✅ 2026-10-03 |
| 6 | `TELEGRAM_BOT_TOKEN` | `release.yml` → kirim APK ke Telegram | ✅ 2026-10-03 |
| 7 | `TELEGRAM_CHAT_ID` | `release.yml` → `chat_id` tujuan | ✅ 2026-10-03 |
| 8 | `URMIX_SUPABASE_URL` | `release.yml` → URL proyek Supabase produksi (`https://vbcfzjwhzfppmtqajgrj.supabase.co`) | ✅ 2026-10-05 |
| 9 | `URMIX_SUPABASE_ANON_KEY` | `release.yml` → Publishable key (`sb_publishable_…`) | ✅ 2026-10-05 |

Status terverifikasi 2026-10-05: **9/9** — `gh secret list --repo
ahmaddzulkarnainyoo-dev/urmix` menampilkan ketujuh nama FASE 6 (terdaftar
2026-10-03 14:53:21–14:53:28 UTC) plus `URMIX_SUPABASE_URL` +
`URMIX_SUPABASE_ANON_KEY` (diperbarui 2026-10-05 08:24:39 UTC) dan
`gh api …/actions/secrets --jq .total_count` = `9`.
(`URMIX_SUPABASE_*` di-set ulang lagi 2026-10-05 ~08:24 UTC untuk
menyelaraskan nilai — URL `...grj` + Publishable key.)

Pendaftaran dijalankan oleh `setup_release_keystore.ps1` (via runner lokal
`run_setup_now.ps1`), yang sekaligus membuat material rilis di **luar** repo:

- keystore `C:\Users\ahmad\.urmix\release\urmix-release.jks` (RSA 4096 / JKS /
  3650 hari / alias `urmix-release`), salinan satu-baris `.jks.b64` (5100 char),
  sertifikat `.der`, dan `e:\urmix\keystore.properties` (git-ignored).
- `SIGNER_SHA256_HEX` =
  `c767c5aab7934e28cd04dbbcfef915c788b8c26f860b552d21875ad6c44c3bdb`, silang-cek
  `keytool -printcert` (awalan `C7:67:C5:AA:…`, 64 hexa = 32 byte).
- Password keystore + sidik jari dicatat di
  `C:\Users\ahmad\.urmix\release\SECRETS_BACKUP.txt` — **pindahkan ke password
  manager lalu hapus berkas itu**, dan simpan `.jks` permanen (kehilangan key =
  user wajib uninstall → data lokal hilang).
- Catatan keamanan: `TELEGRAM_BOT_TOKEN` sempat dikirim lewat chat; bila chat itu
  tersimpan/dibagikan, rotasi token di BotFather lalu
  `gh secret set TELEGRAM_BOT_TOKEN …` ulang (rilis tidak perlu diulang).

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

**Bukti paritas nama secret (2026-10-03):** skrip menegakkan
`$expectedSecrets` (7 nama FASE 6 yang dideklarasikan; **2 secret Supabase
P0 `URMIX_SUPABASE_*` ditambahkan langsung via CLI, bukan via skrip**)
terhadap daftar yang
benar-benar didaftarkan (`Compare-Object`; meleset → `throw`), dan langkah
verifikasi `gh secret list` memakai `$expectedSecrets` (7 nama skrip + 2 Supabase;
bukan kunci hashtable).
Diuji programatik: 7 nama di skrip == 7 pola `secrets.<NAMA>` FASE 6 yang diekstrak
dari workflow → **7 vs 7, drift = 0**; uji
negatif `Compare-Object` (satu nama salah) → 2 selisih terdeteksi; self-test
skrip ulang (`-SkipSecrets`) → `exit 0`.

Alternatif tanpa skrip: `KEYSTORE_BASE64` = isi `.jks.b64`, sisanya nilai
literal (`KEY_ALIAS` = `urmix-release`). Verifikasi nama kapan saja:
`gh secret list --repo ahmaddzulkarnainyoo-dev/urmix` (harus 9 baris:
7 FASE 6 + 2 Supabase).

Jika step *Verify signature pins* gagal setelah rilis pertama: salin token
persis dari `certs.txt` pada log run itu, lalu
`gh secret set SIGNER_SHA256_HEX --repo ahmaddzulkarnainyoo-dev/urmix --body='<token>'`.

## C. Supabase (`urmix_config`) — payload siap insert

> Status 2026-10-05: **LIVE**. URL produksi `https://vbcfzjwhzfppmtqajgrj.supabase.co`
> (ref benar `...grj`; ref `...gri` sebelumnya terbukti **NXDOMAIN**, 2026-10-05).
> Endpoint `GET /rest/v1/urmix_config?select=*&order=config_schema_version.desc&limit=1`
> dengan header `apikey` + `Authorization: Bearer <Publishable>` → **200 OK**,
> array 1 objek `config_schema_version=1`, `app_version='1.0.0'`,
> `force_update=false`, `updated_at='2026-10-05T07:19:44Z'`.
> Secrets `URMIX_SUPABASE_URL` + `URMIX_SUPABASE_ANON_KEY` terpasang 2026-10-05
> (set ulang 08:24:39 UTC, nilai benar); whitelist `HostCachePolicy.CACHEABLE_HOSTS` =
> `winatra.supabase.co`, `vbcfzjwhzfppmtqajgrj.supabase.co` (plus `supabase/README.md`
> diperbarui: menyebut Publishable key + URL produksi).

- File: `supabase/urmix_config_seed.sql` (tabel `announcement` + `donation`
  dalam bentuk `jsonb`; kolom list podcast `text[]`).
- Nilai ethics/pinned endpoint di `RemoteConfigRepository`:
  - `REMOTE_CONFIG_URL = https://vbcfzjwhzfppmtqajgrj.supabase.co/rest/v1/urmix_config?select=*&order=config_schema_version.desc&limit=1`
  - `DEFAULT_UPDATE_URL = https://github.com/ahmaddzulkarnainyoo-dev/urmix/releases/latest`
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
- [x] GitHub Release `v1.0.1` ada + asset `URMIX_v1.0.1.apk` + changelog —
      ✅ 2026-10-05 (run `37275407682`): `isDraft=false`, 13/13 step hijau,
      asset 11.522.797 byte (sha256 `c8bde216…eee1add6`),
      body = `fastlane/…/changelogs/101.txt`
- [x] File APK v1.0.1 yang SAMA terkirim ke Telegram group (caption = changelog) —
      ✅ Telegram `ok:true`, `message_id` 2138, supergroup *WINATRA.OFFICIAL*,
      `file_name=URMIX_v1.0.1.apk`
- [x] GitHub Release `v1.0.0` ada + asset `URMIX_v1.0.0.apk` + changelog (arsip) —
      ✅ 2026-10-03 (run `37133111052`): `isDraft=false`, asset 11.522.829 byte
      (sha256 `4bed064d…5cde`), body = `fastlane/…/changelogs/100.txt`
- [x] File APK v1.0.0 yang SAMA terkirim ke Telegram group (arsip; caption = changelog) —
      ✅ Telegram `ok:true`, `message_id` 2131, supergroup *WINATRA.OFFICIAL*,
      `file_name=URMIX_v1.0.0.apk`; sha256 unduhan lokal = sha256 asset rilis

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
