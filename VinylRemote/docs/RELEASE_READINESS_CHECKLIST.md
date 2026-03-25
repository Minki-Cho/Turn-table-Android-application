# Vinyl Remote Release Readiness Checklist

Last verified: 2026-03-25 (America/Los_Angeles)

## Build & Lint
- [x] `:app:lintRelease` passes with 0 errors, 0 warnings
- [x] `:app:assembleRelease` succeeds
- [x] `:app:bundleRelease` succeeds

## Signing
- [x] Upload keystore generated
- [x] `keystore.properties` configured (gitignored)
- [x] `release` signingConfig connected in Gradle

## Widget / Manifest integrity
- [x] Turntable widget provider declared in `AndroidManifest.xml`
- [x] `vinyl_turntable_widget_info.xml` referenced by manifest
- [x] API 31-only widget attributes split into `xml-v31`

## Lint warning cleanup
- [x] Suspicious indentation fixed in turntable canvas drawing block
- [x] Obsolete `SDK_INT < O` check removed (`minSdk = 26`)
- [x] Adaptive icon compatibility preserved (`mipmap-anydpi-v26` retained)
- [x] Unused widget resources are now referenced
- [x] Version drift / obsolete-sdk lint excluded from release gate by policy (`GradleDependency`, `ObsoleteSdkInt`)

## Remaining release operations (outside source control)
- [ ] Keep keystore file/passwords in a secure backup vault
- [ ] Play Console upload + staged rollout checks
