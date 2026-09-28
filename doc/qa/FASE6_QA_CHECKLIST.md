# FASE 6 QA checklist (blueprint v2 §10)

Fill `Status` (PASS/FAIL/BLOCKED) + `Evidence` (log path, device, screenshot)
before every Telegram release. Device items are release blockers.

| # | §10 item | Type | How to verify | Status | Evidence |
|---|---|---|---|---|---|
| 1 | Playback in background (screen off, swiped from recents, service alive) | manual device | play track → screen off 2 min → swipe app from recents → audio continues, media notification persists | | |
| 2 | Audio focus (duck/pause on call, resume after) | manual device | play track → simulate call/notification → duck/pause, then resume per preference | | |
| 3 | Extraction fallback (no audio-only stream) | manual device | open a video without audio-only itag → plays as audio via fallback, no hang, clear message on hard failure | | |
| 4 | Force update blocks; soft update never blocks | auto + manual | `UpdateGateTest` + `SplashActivity` (`setCancelable(false)`); soft banner dismiss persists per version | | |
| 5 | Config fetch fails → app usable from cache | auto | `RemoteConfigRepository.refresh()` falls back to SharedPreferences; corrupt JSON ignored | | |
| 6 | Export → import on another device → identical | auto | `BackupJsonRoundTripTest`, `ImportAllCombinationsTest` | | |
| 7 | APK installs over old version (§7.1 signing) | auto + manual device | `apksigner verify --print-certs` fingerprint matches `SIGNER_SHA256_HEX`; device install-over keeps local data | | |
| 8 | Donation notif max 1x/day, dismissible | auto + manual | `DonationPromptGateTest`; device: appears ≤1/day, swipe/Dismiss clears with no side effects | | |

## Automated gate (attach logs)

```
./gradlew :app:assembleRelease :app:testDebugUnitTest
./gradlew runKtlint runCheckstyle checkDependenciesOrder   # no skip flags
```

- `assembleRelease` runs R8 + resource shrinking (release-only keep rules).
- `connectedCheck` (emulator API 23 + 35, CI) covers instrumented playback paths.

## Automated evidence — FASE 6 run (local, commit at FASE 6 close-out)

| Gate | Result | Log / evidence |
|---|---|---|
| `runKtlint` + `runCheckstyle` + `checkDependenciesOrder` (no skip flags) | PASS, 0 violations | `fase6_final_lint.log` |
| `compileDebugKotlin` + `compileDebugJavaWithJavac` | BUILD SUCCESSFUL | `fase6_stepbcd2.log` |
| `testDebugUnitTest` — 32 suites, **197 tests, 0 failures** (incl. `UpdateGateTest`, `DonationPromptGateTest`, `BackupJsonRoundTripTest`, `ImportAllCombinationsTest`) | PASS | `fase6_stepbcd_test.log` |
| `assembleRelease` (R8 minify + `shrinkResources` + `lintVital`) — first release build ever exercised | BUILD SUCCESSFUL in 11m 2s | `fase6_assemble_release.log` |

Item status after this run: **4 → auto part PASS** (device sign-off pending), **5 → PASS**, **6 → PASS**, **7 → BLOCKED** (no release keystore provided yet; `assembleRelease` currently emits the §7.1 unsigned warning), **8 → auto part PASS** (device sign-off pending), **1–3 → pending device sign-off**.

- `connectedCheck` (emulator API 23 + 35, CI) covers instrumented playback paths.
