# URMIX Remote Config — Supabase (P0 §1.2)

Satu baris konfigurasi di Supabase mengendalikan perilaku URMIX yang bisa
diubah tanpa rilis APK baru: banner pengumuman, prompt dukungan 1×/hari,
kurasi carousel *Podcasting*, dan gerbang update (`force_update`,
`min_extractor_version`).

| Item | Nilai |
|---|---|
| Endpoint | `GET <SUPABASE_URL>/rest/v1/urmix_config?select=*&order=config_schema_version.desc&limit=1` |
| Konsumen di app | `app/src/main/java/com/winatra/urmix/remoteconfig/RemoteConfigRepository.kt` |
| Skema + seed | [`urmix_config_seed.sql`](urmix_config_seed.sql) |
| Cache offline | `app/src/main/java/com/winatra/urmix/network/HostCachePolicy.kt` |

> Config ini **bukan rahasia**: isinya hanya tautan publik + flag update, dan
> memang terkirim di lalu lintas jaringan APK. `anon`/publishable key Supabase
> juga memang kunci publik. **Jangan pernah** memakai `service_role` key di app.

---

## 1. Buat proyek Supabase

1. Masuk ke <https://supabase.com/dashboard> → **New project**.
2. Catat **Project URL** Anda, mis. `https://<ref>.supabase.co`
   (proyek produksi URMIX: `https://vbcfzjwhzfppmtqajgrj.supabase.co`).

   ⚠️ Host ini penting: `HostCachePolicy.CACHEABLE_HOSTS` meng-allowlist host
   Supabase aktif agar config tetap terbaca saat perangkat offline.
   Kalau Project URL berubah (ref baru), tambahkan host itu ke
   `CACHEABLE_HOSTS` **dan** `HostCachePolicyTest` dulu.
3. Region: pilih terdekat dengan mayoritas pengguna (mis. Singapore).
4. Simpan password database di password manager (tidak dipakai aplikasi).

## 2. Ambil kredensial

**Project Settings → API**:

- **Project URL** → `https://vbcfzjwhzfppmtqajgrj.supabase.co` (nilai `SUPABASE_URL`).
- **Project API keys → Publishable** (`sb_publishable_...`) → salin
  (nilai `SUPABASE_ANON_KEY`). **Jangan** memakai `service_role` /
  `sb_secret_...` di app.

## 3. Jalankan skema + seed

1. Buka **SQL Editor → New query**.
2. Tempel seluruh isi [`urmix_config_seed.sql`](urmix_config_seed.sql).
3. Klik **Run**. Skrip ini idempoten (`create table if not exists` +
   `insert ... on conflict do update`), jadi aman dijalankan ulang.

Yang dihasilkan: tabel `public.urmix_config` (PK `config_schema_version`),
RLS aktif dengan policy baca untuk `anon`/`authenticated`, dan satu baris seed.

## 4. Verifikasi di dashboard

1. **Table Editor → `public.urmix_config`** → harus ada **tepat satu baris**,
   `force_update = false`.
2. **Authentication → Policies** → `urmix_config` menampilkan policy
   `urmix_config anon read` (SELECT untuk `anon`, `authenticated`).
3. **API Docs** (atau **Table Editor → view → API**) → pastikan hanya SELECT
   yang diekspos.

## 5. Uji endpoint (opsional tapi disarankan)

```bash
curl -sS \
  "https://vbcfzjwhzfppmtqajgrj.supabase.co/rest/v1/urmix_config?select=*&order=config_schema_version.desc&limit=1" \
  -H "apikey: <PUBLISHABLE_KEY>" \
  -H "Authorization: Bearer <PUBLISHABLE_KEY>"
```

Respons yang benar: **array JSON berisi satu objek**

```json
[{"config_schema_version":1,"app_version":"1.0.0","min_extractor_version":"0.29.1",
  "force_update":false,"update_url":"https://github.com/.../releases/latest",
  "announcement":{...},"donation":{...},"podcast_channels":[...],"podcast_playlists":[]}]
```

## 6. Masukkan kredensial ke build

Nilai resolusi: **gradle `-P` → environment variable → `local.properties` →
default**. Pilih salah satu cara:

**a. `local.properties`** (gitignored, untuk build lokal):

```properties
urmix.supabase.url=https://vbcfzjwhzfppmtqajgrj.supabase.co
urmix.supabase.anonKey=<PUBLISHABLE_KEY>
```

**b. Environment variable** (dipakai CI):

```bash
export URMIX_SUPABASE_URL="https://vbcfzjwhzfppmtqajgrj.supabase.co"
export URMIX_SUPABASE_ANON_KEY="<PUBLISHABLE_KEY>"
```

**c. Property langsung** (sekali pakai):

```bash
./gradlew assembleRelease -PurmixSupabaseUrl=https://vbcfzjwhzfppmtqajgrj.supabase.co \
  -PurmixSupabaseAnonKey=<PUBLISHABLE_KEY>
```

**d. GitHub Actions** (agar APK rilis memuat config): tambahkan repository
secret **`URMIX_SUPABASE_URL`** dan **`URMIX_SUPABASE_ANON_KEY`**; keduanya
sudah dibaca di step *Build signed release APK* pada `release.yml`.
Tanpa keduanya build tetap sukses (URL default + key kosong → memakai cache).

## 7. Troubleshooting

| Gejala | Penyebab | Solusi |
|---|---|---|
| HTTP `401` / `No API key found in request` | Header `apikey` kosong | Set `urmix.supabase.anonKey` / `URMIX_SUPABASE_ANON_KEY` |
| Response `[]` | RLS aktif tanpa policy SELECT | Jalankan bagian policy di seed |
| `permission denied for table urmix_config` | `grant select` hilang | Jalankan baris `grant select ... to anon, authenticated` |
| `404` / `Could not find the table` | Tabel belum dibuat / skema bukan `public` | Ulangi langkah 3 |
| Config tidak terbaca saat offline | Host tak ada di `CACHEABLE_HOSTS` | Tambahkan host + update `HostCachePolicyTest` |
| Banner/prompt tidak muncul | `announcement.active = false` atau `donation` kosong | Perbarui baris di SQL Editor |

## 8. Operasional harian

- **Hanya boleh satu baris** di `urmix_config`, karena aplikasi membaca baris
  pertama. Semua perubahan dilakukan lewat `update ... where
  config_schema_version = 1`.
- **Pengumuman saja** → ubah kolom `announcement` (resep ada di akhir seed).
- **Soft update** → naikkan `app_version`, `force_update = false`.
- **Force update** → set `force_update = true` **hanya setelah** APK versi baru
  benar-benar tersedia di `update_url`, karena startup akan diblokir.
- Naikkan `min_extractor_version` hanya bila build terbaru memang membawa
  extractor yang lebih baru (lihat `BuildConfig.EXTRACTOR_VERSION`).
