# AIHealth

AIHealth is an Android health management app built with Java and Gradle. It includes modules for diagnosis record management, medicine reminders, diet analysis, sport guidance, and health data visualization.

## Project Structure

- `app/` - Android application source code and resources
- `gradle/` - Gradle version catalog and wrapper files
- `build.gradle.kts` - root Gradle build file
- `settings.gradle.kts` - Gradle project settings
- `local.properties.example` - local configuration template

## Local Setup

1. Install Android Studio with the Android SDK.
2. Copy `local.properties.example` to `local.properties`.
3. Set `sdk.dir` and, if needed, `BAIDU_API_KEY` / `BAIDU_SECRET_KEY`.
4. Open the project root in Android Studio or run:

```powershell
.\gradlew.bat assembleDebug
```

## GitHub Notes

Generated build outputs, IDE state, local SDK paths, logs, heap dumps, APKs, and app bundles are ignored by `.gitignore`.
