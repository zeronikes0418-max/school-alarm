# 📚 학교알리미 - Android 앱

요일별 준비물 + 과제 알림 앱. WebView 기반 네이티브 Android 앱.

---

## ✅ APK 만드는 방법

### 방법 1: GitHub Actions (PC 없이 APK 받기) ⭐ 추천

1. GitHub 계정 만들기 → https://github.com
2. **New repository** → 이름 입력 → Create
3. 이 폴더 전체를 업로드 (Upload files)
4. 상단 **Actions** 탭 클릭
5. **Build APK** → **Run workflow** 클릭
6. 완료되면 **Artifacts** 에서 `학교알리미-debug.apk` 다운로드
7. 폰으로 APK 파일 전송 후 설치

### 방법 2: Android Studio (PC에서 직접 빌드)

1. https://developer.android.com/studio 에서 무료 설치
2. **Open** → 이 폴더(`SchoolAlarm`) 선택
3. 첫 실행 시 SDK 자동 다운로드 (시간 걸림)
4. 상단 메뉴 **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. `app/build/outputs/apk/debug/app-debug.apk` 파일이 생성됨

### APK 설치 방법

1. 폰 **설정 → 보안 → 출처를 알 수 없는 앱 설치** 허용
2. APK 파일 탭하여 설치
3. 삼성 인터넷이나 크롬에서 다운로드한 경우 바로 설치 가능

---

## 📱 앱 기능

| 기능 | 설명 |
|---|---|
| ⏰ 알람 | 정확한 시스템 알람 (앱 꺼져도 울림) |
| 📋 준비물 | 요일별 준비물 체크리스트 |
| ✏️ 과제 | 수행평가·과제 마감 알림 |
| 🔔 알림 | 상태바 알림 + 잠금화면 알람 |
| 😴 스누즈 | 나중에 알림 (1~10분) |

## 🛠 기술 스택

- Android WebView + JavaScriptInterface
- AlarmManager (정확한 알람, 부팅 복원)
- NotificationChannel (Android 8+)
- HTML/CSS/JS 앱 내 포함 (assets/index.html)
