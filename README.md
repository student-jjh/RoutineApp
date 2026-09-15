<div align="center">
  <img src="app/src/main/res/drawable-nodpi/routive_logo.png" alt="Routive logo" width="96" />
  <h1>Routive</h1>
  <p><strong>Build your rhythm. Record your progress.</strong></p>
  <p>A quieter way to manage daily routines, log your sets,<br />and turn workout data into everyday progress.</p>
  <p>
    <img src="https://img.shields.io/badge/Android-8.0%2B-347A52?style=for-the-badge&amp;logo=android&amp;logoColor=white" alt="Android 8.0 and above" />
    <img src="https://img.shields.io/badge/Kotlin-2.2.10-173F2C?style=for-the-badge&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin 2.2.10" />
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-347A52?style=for-the-badge" alt="Jetpack Compose UI" />
    <img src="https://img.shields.io/badge/Status-Testing-72A982?style=for-the-badge" alt="Currently in testing" />
  </p>
  <p>
    <a href="#the-experience">Explore features</a> ·
    <a href="#getting-started">Build the app</a> ·
    <a href="#health-connect">Connect workouts</a> ·
    <a href="https://github.com/student-jjh/RoutineApp/issues">Share feedback</a>
  </p>
  <sub>Personal project · Local-first storage · Currently available with a Korean interface</sub>
</div>

<br />

## Small routines. Meaningful progress.

Routive brings your daily checklist and workout notebook together. Plan what matters, check in without friction, and look back at the days you showed up.

<table>
  <tr>
    <td width="33%" valign="top">
      <h3>🌿 Your own rhythm</h3>
      <p>Choose your weekdays, arrange your routines, and see only what is scheduled for today.</p>
    </td>
    <td width="33%" valign="top">
      <h3>🏋️ Every set counts</h3>
      <p>Track each set individually, take timed rests, and follow your exercise progress.</p>
    </td>
    <td width="33%" valign="top">
      <h3>📅 Progress, not perfection</h3>
      <p>A 75% completion rate is enough for an achievement day. Missed a check-in? Update it later.</p>
    </td>
  </tr>
</table>

## The experience

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>01 / Today</h3>
      <p><strong>A focused checklist for the day ahead.</strong></p>
      <ul>
        <li>Only today's scheduled routines.</li>
        <li>Tap to complete; tap again to undo.</li>
        <li>Long-press to reveal the edit action.</li>
        <li>Add a routine directly from the home screen.</li>
        <li>Matching workouts can complete exercise routines automatically.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>02 / My Routines</h3>
      <p><strong>Make the plan fit your life.</strong></p>
      <ul>
        <li>Create, edit, and delete routines.</li>
        <li>General and exercise categories.</li>
        <li>Individual days, weekdays, weekends, or every day.</li>
        <li>Exercise type and minimum-duration rules.</li>
        <li>Long-press and drag cards to reorder. Your order is saved.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>03 / Exercise</h3>
      <p><strong>Your training notebook, connected.</strong></p>
      <ul>
        <li>Strength logs with per-set weight and repetitions.</li>
        <li>Bodyweight exercises without a required weight.</li>
        <li>Muscle-group and exercise filters, plus custom exercises.</li>
        <li>Full-screen record editor with a rest timer.</li>
        <li>Health Connect cardio sessions and running trend charts.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>04 / History</h3>
      <p><strong>See how far you have come.</strong></p>
      <ul>
        <li>Calendar and date-by-date navigation.</li>
        <li>Editable completion status for past dates.</li>
        <li>Daily completion tiers: 25%, 50%, 75%, and 100%.</li>
        <li>Expandable per-routine completion rates.</li>
        <li>New routines excluded from dates before their creation.</li>
      </ul>
    </td>
  </tr>
</table>

<p align="center">
  <strong>New here?</strong> Swipe through the first-launch guide.<br />
  <sub>Reopen it anytime from My Routines → User Guide (<code>사용 가이드</code>).</sub>
</p>

## A closer look at your training

### Strength — log, rest, repeat

Choose an exercise from the muscle-group list or add your own. Record each set with its own weight and repetitions, keep weight blank for bodyweight movements, and use the rest timer while adding or editing a workout.

Today's records are the default view. Switch to muscle-group or exercise views to find previous sessions and review progress. Today's strength records can also complete matching strength routines.

### Cardio — more than a completed checkbox

<table>
  <tr>
    <th align="left">Metric</th>
    <th align="left">In Routive</th>
  </tr>
  <tr><td>Distance &amp; duration</td><td>Session details and cardio summaries</td></tr>
  <tr><td>Pace</td><td>Running session pace and trend chart</td></tr>
  <tr><td>Heart rate</td><td>Session metrics and average heart-rate trends</td></tr>
  <tr><td>Cadence</td><td>Session metrics and running cadence trends</td></tr>
  <tr><td>Elevation &amp; calories</td><td>Additional session details when available</td></tr>
</table>

> Metrics depend on the data shared by your source app and the permissions granted. Missing data is not a zero-value workout. Pull down to refresh after your source app shares a session.

## Health Connect

<p>
  <strong>Workout source app</strong> &nbsp;→&nbsp;
  <strong>Health Connect</strong> &nbsp;→&nbsp;
  <strong>Routive</strong>
</p>

Routive reads workouts shared with Health Connect; it does not connect directly to Samsung Health.

1. Make sure Health Connect is available on your device.
2. Enable Health Connect data sharing in your source app, such as Samsung Health.
3. Grant Routive read access through its workout connection flow.
4. After the workout is shared, pull down in Routive to refresh.

<details>
  <summary><strong>Permissions and missing workout data</strong></summary>

<br />

The basic connection uses exercise session and distance permissions. Heart rate, steps/cadence, elevation gain, and calories burned are optional permissions for additional metrics.

An access entry for Routive in Health Connect does not guarantee that workout records are available. Check which data types the source app actually shared and when the workout was recorded.

Health Connect integration requires a supported device and an available Health Connect service, separately from the app's minimum Android version.

</details>

## Under the hood

<p>
  <img src="https://img.shields.io/badge/Material-3-173F2C?style=flat-square" alt="Material 3" />
  <img src="https://img.shields.io/badge/Room-Local%20Database-347A52?style=flat-square" alt="Room local database" />
  <img src="https://img.shields.io/badge/Coroutines-%26%20Flow-173F2C?style=flat-square" alt="Coroutines and Flow" />
  <img src="https://img.shields.io/badge/Health%20Connect-Workout%20Data-347A52?style=flat-square" alt="Health Connect workout data" />
  <img src="https://img.shields.io/badge/Build-Gradle%20%2B%20KSP-173F2C?style=flat-square" alt="Gradle and KSP" />
</p>

Built with **Kotlin, Jetpack Compose, and Material 3**, using **Room** for local records and **Coroutines / Flow** for data updates. No dedicated backend or account registration is required.

## Getting started

<details>
  <summary><strong>Development requirements</strong></summary>

<br />

| Setting | Current configuration |
| --- | --- |
| Minimum Android SDK | 26 (Android 8.0) |
| Compile / Target SDK | 37 |
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.2.10 |
| Gradle Daemon JVM | 25 |
| Application ID | `com.example.routineapp` |

Use an Android Studio version that supports the SDK and Gradle plugin configured in the repository. Configure your Android SDK location locally and do not commit `local.properties`.

</details>

<br />

```bash
git clone https://github.com/student-jjh/RoutineApp.git
cd RoutineApp

# Build the debug APK
./gradlew assembleDebug
```

**APK output:** `app/build/outputs/apk/debug/app-debug.apk`

<details>
  <summary><strong>Run tests and install on a device</strong></summary>

<br />

```bash
# Run local unit tests
./gradlew testDebugUnitTest

# Find a device with USB debugging enabled
adb devices

# Update-install the debug APK
adb -s <device-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

</details>

<details>
  <summary><strong>Explore the project structure</strong></summary>

<br />

```text
app/src/main/java/com/example/routineapp/
├── MainActivity.kt             # Navigation, routine UI, Health Connect
├── ReorderableRoutineList.kt   # Long-press drag-and-drop ordering
├── OnboardingScreen.kt         # First-launch guide
├── CalendarDashboard.kt        # Calendar and routine history
├── ExerciseDashboard.kt        # Cardio metrics and charts
├── StrengthRecordScreen.kt     # Strength logs, set entry, rest timer
├── RoutineSchedule.kt          # Scheduling and achievement rules
├── data/                      # Room entities, DAOs, migrations
└── ui/theme/                  # Colors, typography, theme
```

</details>

## Your data & updates

Routines, completion history, strength records, custom exercises, and routine order are stored locally in Room. Onboarding completion is stored in SharedPreferences.

<details>
  <summary><strong>Read before distributing or updating an APK</strong></summary>

<br />

- Update with the **same Application ID and signing key** to generally preserve existing app data.
- Database changes use migrations; the current database version is `10`.
- Uninstalling the app or clearing its storage deletes local data.
- Debug APKs built in different development environments may use different keys and fail to install as updates.
- Keep the signing key consistent and increment `versionCode` for ongoing tester releases.

**Cloud synchronization and user-facing data export are not currently implemented.** Be careful before uninstalling an app containing important records.

</details>

<br />

---

<div align="center">
  <h3>Help shape Routive</h3>
  <p>Found a rough edge? Have an idea for a better daily flow?</p>
  <p><a href="https://github.com/student-jjh/RoutineApp/issues"><strong>Report a bug or suggest an improvement →</strong></a></p>
  <sub>Include your device, Android version, APK version, and reproduction steps.<br />Please redact personal health information from screenshots and logs.</sub>
  <br /><br />
  <p><strong>Build your rhythm. One day at a time.</strong></p>
</div>
