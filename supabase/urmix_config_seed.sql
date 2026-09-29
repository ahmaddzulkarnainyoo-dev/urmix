-- ============================================================
-- URMIX Remote Config — Supabase seed for table `urmix_config`
-- Endpoint consumed by the app: GET .../rest/v1/urmix_config?select=*
-- (RemoteConfigRepository parses the FIRST row as a flat JSON object)
-- ============================================================
-- NOTE: replace the placeholder values with the real WINATRA endpoints
-- before the first public release.

INSERT INTO public.urmix_config (
  config_schema_version,
  app_version,
  min_extractor_version,
  force_update,
  update_url,
  announcement,
  donation,
  podcast_channels,
  podcast_playlists
) VALUES (
  1,                                     -- config_schema_version
  '1.0.0',                               -- app_version (required when force_update = true)
  '0.29.1',                              -- min_extractor_version (matches FASE 3 bundle; see BuildConfig.EXTRACTOR_VERSION)
  false,                                 -- force_update (true = block app start, user must update)
  'https://winatra.com/urmix/download',  -- update_url (APK / release page)
  -- announcement (jsonb) — shown as an in-app banner when announcement.active = true
  '{
     "active": true,
     "title": "Selamat datang di URMIX v1.0 🎶",
     "message": "Pemutar audio latar, mini-player Spotify-style, dan backup library kini aktif. Nikmati musik tanpa batas!"
   }'::jsonb,
  -- donation (jsonb) — drives the once-per-day support prompt (§6)
  '{
     "title": "Dukung URMIX ☕",
     "saweria_url": "https://saweria.co/WINATRA",
     "qris_url": "https://winatra.com/urmix/donasi/qris.png",
     "daily_message": "URMIX gratis selamanya. Kalau kamu terbantu, traktir tim WINATRA secangkir kopi yuk 🙏"
   }'::jsonb,
  -- podcast_channels (text[]) — curated YouTube channel/podcast sources (§3.3)
  ARRAY[
    'https://www.youtube.com/@.Ted',
    'https://www.youtube.com/@lexfridman'
  ],
  -- podcast_playlists (text[]) — curated YouTube playlist sources (§3.3)
  ARRAY[
    'https://www.youtube.com/playlist?list=WL'
  ]
);

-- ---- Row 2: example of a FORCED update row (apply only when shipping v1.1.0+) ----
-- UPDATE public.urmix_config
-- SET app_version = '1.1.0',
--     force_update = true,
--     update_url = 'https://github.com/Winatra/urmix/releases/tag/v1.1.0',
--     announcement = '{"active": true,
--                       "title": "URMIX v1.1.0 tersedia",
--                       "message": "Versi ini wajib diinstal untuk lanjut memakai URMIX."}'::jsonb
-- WHERE config_schema_version = 1;

-- ---- Row 3: announcement-only refresh (no version change) ----
-- UPDATE public.urmix_config
-- SET announcement = '{"active": true,
--                       "title": "Info WINATRA",
--                       "message": "Tulis pengumuman baru di sini tanpa memaksa update."}'::jsonb
-- WHERE config_schema_version = 1;
