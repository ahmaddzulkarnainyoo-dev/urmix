# URMIX release process (FASE 6 §7)

## 1. One consistent keystore (§7.1)

- Generate once, back it up **outside the repo** (password manager + offline
  copy). Losing it forces users to uninstall → **local data loss** (§5).
- Never commit `keystore.properties`, `*.jks`, or `*.keystore`
  (git-ignored; see `keystore.properties.template`).
- Fingerprint the certificate once and store it as the `SIGNER_SHA256_HEX`
  secret — `release.yml` refuses to publish on mismatch:
  `apksigner verify --print-certs app-*.apk | grep -i sha-256`.

## 2. GitHub secrets (all required by release.yml)

| Secret | Content |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 urmix-release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias (e.g. `urmix-release`) |
| `KEY_PASSWORD` | key password |
| `SIGNER_SHA256_HEX` | certificate SHA-256 hex (no colons) |
| `TELEGRAM_BOT_TOKEN` | BotFather token for the release bot |
| `TELEGRAM_CHAT_ID` | target group/channel id |

## 3. Telegram bot setup

1. Create the bot via BotFather, add it to the release group/channel.
2. `GET https://api.telegram.org/bot<TOKEN>/getUpdates` to resolve the chat id.
3. Store token + chat id as secrets above.

## 4. Pre-dispatch QA gate (§10)

- Complete `doc/qa/FASE6_QA_CHECKLIST.md` — the three device items
  (background playback, audio focus, extraction fallback) plus
  install-over-install are **release blockers** needing human sign-off.
- Automated evidence: `./gradlew :app:assembleRelease :app:testDebugUnitTest`
  plus `runKtlint runCheckstyle checkDependenciesOrder` without skip flags.

## 5. Dispatch + post-release

1. Update `fastlane/metadata/android/en-US/changelogs/<CODE>.txt`
   (CODE = `URMIX_VERSION_CODE` in `ProjectConfig.kt`); it becomes the GitHub
   Release body **and** the Telegram caption.
2. Paste the matching `announcement` (+ `app_version`/`force_update` when the
   release is mandatory) into the Supabase `urmix_config` row.
3. Run the `URMIX release` workflow (`workflow_dispatch`, tag `vX.Y.Z`) or
   push the tag; verify GitHub Release + Telegram post + signature pin.
