// Repository-root aggregator.
//
// Android Studio sometimes opens the repository root instead of the `android`
// directory. This composite build ensures that doing so still resolves the
// project wrapper (Gradle 8.11.1) rather than falling back to the IDE's bundled
// Gradle distribution. Day-to-day builds should still be run from `android/`
// or `backend/` directly.
rootProject.name = "momento"

includeBuild("android")
includeBuild("backend")
