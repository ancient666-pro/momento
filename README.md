# Momento

Production-grade social media platform: native Android (Kotlin + Jetpack Compose), Ktor backend (PostgreSQL/PostGIS, Redis, MinIO), real-time chat & calls via Nexconn SDKs.

The master build specification and the four HTML design references live in [docs/reference/master-build-prompt.txt](docs/reference/master-build-prompt.txt).

## Repository layout

```
android/   Multi-module Android app (Kotlin, Compose, MVVM + Clean Architecture, Hilt)
backend/   Ktor modular monolith (Kotlin, PostgreSQL, Redis, MinIO, Flyway)
docs/      Specs and reference material
```

## Android modules

- `app` — application shell, MainActivity, bottom navigation (Home, Discover, Create, Pod, Profile)
- `core:common` — shared utilities, result/error types
- `core:designsystem` — color/typography/spacing/shape tokens and reusable components
- `core:navigation` — navigation routes and destinations
- `core:networking` — Retrofit/OkHttp client, auth interceptor
- `core:database` — Room (added in the offline-caching phase)
- `core:datastore` — DataStore preferences
- `domain` — models, repository interfaces, use cases
- `data` — remote API, repository implementations, Nexconn realtime layer, mappers
  - Both debug and release bind the same remote repositories; the design-preview
    dataset is kept only for screenshot regression work.
  - `data/realtime/NexconnRealtime` implements `domain.realtime.RealtimeSession` on top of
    the Nexconn Chat + Call SDKs (messages, 1:1 voice/video, multi-party audio rooms).
- `feature:auth`, `feature:home`, `feature:profile`, `feature:discover`, `feature:pod`, `feature:create`
- Planned: `feature:chat`, `feature:calling`, `feature:notifications`, `feature:settings`

## Real-time communications (Nexconn)

The backend mints Nexconn access tokens at `POST /api/v1/communications/nexconn/token`,
signing the request with the App Secret. The Android app calls that endpoint after sign-in
and connects the SDKs; the response carries the public App Key and data-center area code,
so neither is hardcoded in the client. Credentials are optional for offline development:
without them the endpoint returns `503 nexconn_not_configured` and the app logs the
realtime connection as unavailable instead of failing.

## Build requirements

- JDK 21 (Gradle must run on JDK 21, not JBR 25) — e.g. `C:\Users\Rachit Pandey\.jdks\jbr-21.0.11`
- Android SDK (platform 35, build-tools)
- Docker for the backend services (PostgreSQL+PostGIS, Redis, MinIO)

```bash
# Android (from android/)
./gradlew assembleDebug

# Backend (from backend/)
./gradlew build

# Services
docker compose up -d
```

### Opening the project in Android Studio

Open the **`android/`** folder, not the repository root. Android Studio 2026.1
bundles **JBR 25**, which Gradle 8.11.1 / AGP 8.9.2 / Kotlin 2.1 cannot run on, so
the Gradle JVM must be JDK 21 (the same one used on the command line).

- **Settings → Build, Execution, Deployment → Build Tools → Gradle →
  Gradle JDK: `C:\Users\Rachit Pandey\.jdks\jbr-21.0.11`**
- Keep **Use Gradle from: `gradle-wrapper.properties` file**. Do not select a local
  Gradle 9.x distribution; AGP 8.9.2 is not compatible with it.

On this machine the daemon JVM is also pinned in `~/.gradle/gradle.properties`:

```properties
org.gradle.java.home=C:/Users/Rachit Pandey/.jdks/jbr-21.0.11
```

This makes builds work even if the IDE launches Gradle on JBR 25. It is a local
machine setting and is intentionally not committed to the repository.

For convenience, the repository root is also a valid Gradle **composite build**
(`settings.gradle.kts` includes `android` and `backend`) with the same 8.11.1
wrapper, so opening the root cannot fall back to the IDE's Gradle 9.3.0 default.
Running the app still uses the `android/` project.

### Gradle distribution (offline / restricted networks)

Both projects are pinned to **Gradle 8.11.1** (required by AGP 8.9.2). The wrapper
sets `validateDistributionUrl=false` and a 120-second timeout, so a distribution
already present in `~/.gradle/wrapper/dists/gradle-8.11.1-bin` is used without a
network call — this is what makes the build work when `services.gradle.org` is
unreachable.

If that cache is ever cleared, point `distributionUrl` in the root,
`android/`, and `backend/` wrapper files at a reachable mirror:

```properties
distributionUrl=https\://mirrors.aliyun.com/gradle/distributions/v8.11.1/gradle-8.11.1-bin.zip
```

In Android Studio, keep **Settings → Build, Execution, Deployment → Build Tools →
Gradle → Use Gradle from: `gradle-wrapper.properties` file**. Do not select a local
Gradle 9.x distribution; AGP 8.9.2 is not compatible with it.

## Connecting the app to the local backend

The debug build talks to **`http://127.0.0.1:8080/`**. On a USB-attached phone or
emulator, forward the device port back to this machine:

```powershell
docker compose up -d          # PostgreSQL/PostGIS, Redis, S3Mock
cd backend; .\gradlew run     # Ktor on 0.0.0.0:8080

# in another terminal, from the repository root:
powershell -ExecutionPolicy Bypass -File scripts/dev-connect.ps1
```

`scripts/dev-connect.ps1` runs `adb reverse tcp:8080 tcp:8080`, checks
`/health`, and relaunches the debug app. The tunnel works over USB even when the
phone is on mobile data and cannot reach the PC's LAN address.

Why not `10.0.2.2`? That address is the emulator's alias for the host loopback
only. A physical device cannot route to it, which is the source of
`failed to connect to /10.0.2.2 (port 8080)`.

If USB is unavailable, point the build at the PC's LAN address instead (the phone
must be on the same Wi-Fi and the firewall must allow port 8080):

```powershell
cd android
.\gradlew :app:assembleDebug -Pmomento.apiBaseUrl=http://<pc-lan-ip>:8080/
```

Debug builds permit cleartext for `127.0.0.1`, `localhost`, and `10.0.2.2`; release
builds require TLS.

## Current status

| Area | Status |
|---|---|
| Monorepo scaffold, design system | Done |
| Home / Profile / Discover / Pod screens | Implemented against the live API |
| Ktor backend, migrations, seed data | Done (PostgreSQL/PostGIS, Redis, S3-compatible storage) |
| REST API wiring (debug + release) | Done |
| Nexconn Chat / Call SDK integration | Done — activate with dashboard credentials (see `.env.example`) |
| Chat/call UI screens, push notifications, moderation console | Pending |
| Automated tests | Pending |

## Secrets

Nexconn App Secret, JWT secret, and DB credentials live only in environment variables
(`.env.example` provides the template). Never commit real values.
