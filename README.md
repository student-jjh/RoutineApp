# Routive

An Android app for daily routines and workout tracking.
Plan routines by weekday, check off your progress, and let Health Connect workout data complete matching exercise routines automatically.

Started as a personal project, Routive is currently being tested through APK distribution and user feedback. The app interface is currently in Korean.

## Features

### Routine management

- Create, edit, and delete routines with general or exercise categories.
- Choose recurring weekdays individually or select weekdays, weekends, or all days.
- Set an exercise type and minimum duration for automatic completion.
- Long-press a card in My Routines and drag it to change its position.
- Save the custom order automatically and apply it to the home list.

### Today's routines

- See only routines scheduled for today.
- Tap a card to complete or undo it; long-press to reveal its edit action.
- Add routines using the + button on the home screen.
- Complete **at least 75%** of scheduled routines to count the day toward your achievement streak.
- Automatically complete exercise routines from matching Health Connect workouts or today's strength records.

### Workout tracking

**Strength training**

- View today's records by default, with muscle-group and exercise filters.
- Pick from predefined exercises by muscle group or add custom exercises.
- Record weight and repetitions separately for each set.
- Leave weight blank for bodyweight exercises.
- Add and edit records in a full-screen editor with a rest timer.
- Review exercise history and progress trends.

**Cardio**

- Browse workout sessions imported from Health Connect.
- Inspect distance, duration, pace, heart rate, cadence, elevation gain, and calories burned.
- View running trends for pace, average heart rate, and cadence.
- Pull down to refresh workout data.

> Available metrics depend on the data shared by the source app and the permissions granted. Missing metrics may be hidden or shown without a value.

### History and onboarding

- Browse past routines using the calendar or date navigation buttons.
- Update completion status for past dates, including missed late-night check-ins.
- Visualize daily completion in 25%, 50%, 75%, and 100% tiers.
- Expand per-routine completion rates when needed.
- Exclude newly created routines from dates before their creation.
- Swipe through a first-launch guide and reopen it from My Routines → User Guide (`사용 가이드`).

## Navigation

| Tab | Purpose |
| --- | --- |
| Today | Scheduled routines and completion toggles |
| My Routines | Routine settings, ordering, and user guide |
| Exercise | Strength logs and Health Connect cardio analysis |
| History | Calendar, past check-ins, and completion rates |

## Connecting Health Connect

Routive reads **workout records shared with Health Connect**, rather than connecting directly to Samsung Health.

1. Make sure Health Connect is available on your device.
2. Enable data sharing to Health Connect in your workout source app, such as Samsung Health.
3. Grant read permissions through Routive's workout data connection flow.
4. Once the workout has been shared, pull down in Routive to refresh.

The basic connection uses exercise session and distance permissions. Heart rate, steps/cadence, elevation gain, and calories burned are optional permissions for additional metrics.

An access entry for Routive in Health Connect does not guarantee that workout data is available. If records are missing, check which data types the source app actually shared and when they were recorded.

## Tech stack

- Kotlin, Jetpack Compose, and Material 3
- Room local database and Flow
- Kotlin Coroutines
- AndroidX Health Connect
- Gradle Version Catalog and KSP

Routines and strength records are managed locally without a dedicated backend or account registration.

## Development setup

Current repository configuration:

| Setting | Value |
| --- | --- |
| Minimum Android SDK | 26 (Android 8.0) |
| Compile / Target SDK | 37 |
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.2.10 |
| Gradle Daemon JVM | 25 |
| Application ID | `com.example.routineapp` |

Health Connect integration additionally requires a supported device and an available Health Connect service.
Use an Android Studio version that supports the SDK and Gradle plugin configured in this project.

```bash
git clone https://github.com/student-jjh/RoutineApp.git
cd RoutineApp
```

Open the project in Android Studio and run Gradle Sync. Configure your local Android SDK location as needed; do not commit `local.properties`.

```bash
# Build a debug APK
./gradlew assembleDebug

# Run local unit tests
./gradlew testDebugUnitTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

On Windows, use `gradlew.bat` instead of `./gradlew`.

To install on a device with USB debugging enabled:

```bash
adb devices
adb -s <device-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Updates and data retention

Routines, completion history, strength records, custom exercises, and routine order are stored in Room. Onboarding completion is stored in SharedPreferences.

- Updating an existing installation with the **same Application ID and signing key** generally preserves app data.
- Database changes use migrations. The current database version is `10`.
- Uninstalling the app or clearing its storage deletes local data.
- Debug APKs built on different development environments may use different signing keys and fail to install as updates.
- For ongoing tester distribution, retain the same signing key and increment `versionCode` for each release.

Cloud synchronization and user-facing data export are not currently implemented. Be careful before uninstalling an app containing important records.

## Project structure

```text
app/src/main/java/com/example/routineapp/
├── MainActivity.kt             # Navigation, routine UI, Health Connect integration
├── ReorderableRoutineList.kt   # Drag-and-drop routine ordering
├── OnboardingScreen.kt         # First-launch onboarding and user guide
├── CalendarDashboard.kt        # Calendar and routine history
├── ExerciseDashboard.kt        # Cardio metrics and charts
├── StrengthRecordScreen.kt     # Strength logs, set entry, and rest timer
├── RoutineSchedule.kt          # Scheduling dates and achievement threshold
├── data/                      # Room entities, DAOs, and migrations
└── ui/theme/                  # Colors, typography, and theme
```

## Feedback

Report bugs and suggestions through [GitHub Issues](https://github.com/student-jjh/RoutineApp/issues).
Include your device model, Android version, APK version, reproduction steps, and expected versus actual behavior.
Please redact personal health information from screenshots and logs before sharing them.
