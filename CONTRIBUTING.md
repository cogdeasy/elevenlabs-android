# Contributing

Thanks for your interest in contributing to the ElevenLabs Android SDK!

## Project layout

- `elevenlabs-sdk/` — the SDK library (published to Maven Central as `io.elevenlabs:elevenlabs-android`)
- `example-app/` — Compose demo app exercising the SDK
- `config/detekt/detekt.yml` — shared detekt configuration
- `.github/workflows/` — CI (build, unit tests, static analysis, instrumented tests, docs)

## Prerequisites

- JDK 17
- Android SDK with API 35 (`compileSdk = 35`)
- An emulator or device (API 21+) for instrumented tests

## Building and testing

```bash
./gradlew build                                  # assemble + lint + unit tests
./gradlew test                                   # JVM unit tests only
./gradlew detekt ktlintCheck                     # static analysis
./gradlew connectedDebugAndroidTest              # instrumented tests (emulator/device required)
./gradlew :elevenlabs-sdk:dokkaHtml              # API reference (build/dokka/html)
```

CI runs all of the above on every pull request, including the instrumented
tests on an API 29 emulator. Instrumented tests must not require real
ElevenLabs API keys — mock the network (e.g. with on-device `MockWebServer`).

## Code style

- Kotlin style is enforced with ktlint (`./gradlew ktlintCheck`, auto-fix with `./gradlew ktlintFormat`).
- Static analysis is enforced with detekt (`./gradlew detekt`).
- Both tools use per-module baselines (`ktlint-baseline.xml`, `detekt-baseline.xml`) for
  pre-existing issues. Do not add new baseline entries — fix new violations instead.
  Shrinking the baselines is always welcome.

## Pull requests

1. Fork and create a feature branch.
2. Keep changes focused; add or extend tests for anything you change.
3. Update `CHANGELOG.md` under **Unreleased** for user-facing changes.
4. Make sure `./gradlew build detekt ktlintCheck` passes locally.
5. Open a PR — the template will prompt you for a summary and test evidence.

## Releases

Releases are published to Maven Central via the `publish.yml` workflow by maintainers.
