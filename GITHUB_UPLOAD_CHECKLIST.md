# GitHub 업로드 체크리스트

## 꼭 올릴 파일/폴더
- `app/src/main/java/` (앱 로직)
- `app/src/main/res/` (UI 리소스, 아이콘, 위젯 레이아웃 등)
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/` + `gradlew` + `gradlew.bat`
- `README.md`
- `BLOG_POST_DRAFT_KO.md` (원하면 별도 블로그용)
- `.gitignore`

## 올리면 안 되는 것
- `.idea/`
- `.gradle/`, `.gradle-home/`
- `build/`, `app/build/`
- `local.properties`
- keystore 파일 (`*.jks`, `*.keystore`)
- API 키/비밀 파일 (`secrets.properties`, `google-services.json` 등)

## 포트폴리오용으로 추가하면 좋은 것
- `docs/screenshot-main.png`
- `docs/screenshot-flip.png`
- `docs/screenshot-widget.png`
- `docs/demo.gif` 또는 데모 영상 링크

## 권장 초기 커밋 순서
1. `chore: initialize project structure and gitignore`
2. `feat: implement turntable interaction and media session control`
3. `feat: add lrc sync and responsive layouts`
4. `feat: add notification/widget controls and theme presets`
5. `docs: update README and portfolio blog draft`
