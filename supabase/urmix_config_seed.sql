-- ============================================================
-- URMIX Remote Config - Supabase schema + seed for `urmix_config`
--
-- Endpoint consumed by the app (see RemoteConfigRepository.kt):
--   GET <SUPABASE_URL>/rest/v1/urmix_config?select=*&order=config_schema_version.desc&limit=1
-- The app reads the FIRST row and parses it as a flat JSON object.
--
-- Run once in the Supabase SQL Editor ("Run"); it is idempotent, so
-- re-running it simply refreshes the same row.
-- ============================================================

-- ---- 1. Table -----------------------------------------------------------------
create table if not exists public.urmix_config (
  config_schema_version integer     primary key,
  app_version           text        not null default '1.0.0',
  min_extractor_version text,
  force_update          boolean     not null default false,
  update_url            text,
  announcement          jsonb,
  donation              jsonb,
  podcast_channels      text[]      not null default '{}',
  podcast_playlists     text[]      not null default '{}',
  updated_at            timestamptz not null default now()
);

-- ---- 2. Access control --------------------------------------------------------
-- The app only ever READS with the public anon key, so a single SELECT policy
-- is enough; writes stay with the service role (SQL Editor / server side).
alter table public.urmix_config enable row level security;

drop policy if exists "urmix_config anon read" on public.urmix_config;
create policy "urmix_config anon read"
  on public.urmix_config
  for select
  to anon, authenticated
  using (true);

grant select on public.urmix_config to anon, authenticated;

-- ---- 3. Seed row --------------------------------------------------------------
insert into public.urmix_config (
  config_schema_version,
  app_version,
  min_extractor_version,
  force_update,
  update_url,
  announcement,
  donation,
  podcast_channels,
  podcast_playlists
) values (
  1,                                                       -- config_schema_version
  '1.0.0',                                                 -- app_version (required when force_update = true)
  '0.29.1',                                                -- must match BuildConfig.EXTRACTOR_VERSION of the newest build
  false,                                                   -- force_update: true = block startup until the user updates
  'https://github.com/ahmaddzulkarnainyoo-dev/urmix/releases/latest',
  -- announcement (jsonb): shown as an in-app banner while announcement.active = true
  '{
     "active": true,
     "title": "Selamat datang di URMIX v1.0",
     "message": "Pemutar audio latar, mini-player ala Spotify, dan backup library sudah aktif. Selamat menikmati!"
   }'::jsonb,
  -- donation (jsonb): drives the once-per-day support prompt (blueprint v2 §6)
  '{
     "title": "Dukung URMIX",
     "saweria_url": "https://saweria.co/winatra",
     "qris_url": null,
     "daily_message": "URMIX gratis selamanya. Kalau kamu terbantu, traktir tim WINATRA secangkir kopi ya."
   }'::jsonb,
  -- podcast_channels (text[]): curated YouTube channels used by the Podcasting carousel (§3.3)
  array[
    'https://www.youtube.com/@TED',
    'https://www.youtube.com/@lexfridman',
    'https://www.youtube.com/@TEDTalks',
    'https://www.youtube.com/@hubermanlab'
  ],
  -- podcast_playlists (text[]): curated public YouTube playlists (§3.3).
  --   Add real, publicly reachable playlist URLs (must work while signed out);
  --   leave empty to rely on podcast_channels only.
  array[]::text[]
)
on conflict (config_schema_version) do update set
  app_version           = excluded.app_version,
  min_extractor_version = excluded.min_extractor_version,
  force_update          = excluded.force_update,
  update_url            = excluded.update_url,
  announcement          = excluded.announcement,
  donation              = excluded.donation,
  podcast_channels      = excluded.podcast_channels,
  podcast_playlists     = excluded.podcast_playlists,
  updated_at            = now();

select * from public.urmix_config;

-- ---- 4. Recipes (run manually as needed) --------------------------------------

-- Announcement-only refresh (no update pressure):
--   update public.urmix_config
--   set announcement = '{"active": true,
--                        "title": "Info WINATRA",
--                        "message": "Tulis pengumuman baru di sini."}'::jsonb
--   where config_schema_version = 1;

-- Soft update notice (non-blocking; blueprint v2 §7.3):
--   update public.urmix_config
--   set app_version = '1.1.0',
--       force_update = false,
--       update_url = 'https://github.com/ahmaddzulkarnainyoo-dev/urmix/releases/latest'
--   where config_schema_version = 1;

-- Forced update (blocks startup; only AFTER the new APK is published):
--   update public.urmix_config
--   set app_version = '1.1.0',
--       force_update = true,
--       update_url = 'https://github.com/ahmaddzulkarnainyoo-dev/urmix/releases/tag/v1.1.0'
--   where config_schema_version = 1;