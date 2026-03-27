# PicklyDeck Release Readiness Checklist

Last verified: 2026-03-27 (America/Los_Angeles)

## Build & Lint
- [x] `:app:lintDebug` passes with 0 errors, 0 warnings
- [x] `:app:lintRelease` passes with 0 errors, 0 warnings
- [x] `:app:assembleRelease` succeeds
- [x] `:app:bundleRelease` succeeds
- [x] `:app:testDebugUnitTest` succeeds

## Signing
- [x] Release signing works with `keystore.properties`
- [x] Release signing also supports `PICKLYDECK_*` environment variables
- [x] `release` signingConfig connected in Gradle

## Widget / Manifest integrity
- [x] Turntable widget provider declared in `AndroidManifest.xml`
- [x] `vinyl_turntable_widget_info.xml` referenced by manifest
- [x] API 31-only widget attributes split into `xml-v31`
- [x] Notification listener service is non-exported
- [x] Android cloud backup disabled for app data

## Product / Policy integrity
- [x] User-facing branding is PicklyDeck in the app and project docs
- [x] Lyrics references removed from the shipping app docs
- [x] In-app disclosure explains notification access and on-device metadata usage
- [x] Privacy policy draft added at `docs/PRIVACY_POLICY.md`
- [x] Notification permission flow exists for Android 13+

## Remaining release operations (outside source control)
- [ ] Enable GitHub Pages for repository `main` -> `/docs` and add `https://minki-cho.github.io/Turn-table-Android-application/privacy-policy/` to Play Console
- [ ] Move the real signing secrets out of the working tree or rotate the upload key if this workspace was ever shared
- [ ] Play Console upload + staged rollout checks
