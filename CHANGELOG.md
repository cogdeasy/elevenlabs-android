# Changelog

All notable changes to the ElevenLabs Android SDK are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
(pre-1.0: minor versions may contain breaking changes).

## [Unreleased]

### Fixed
- `ConversationEventParser` now reads `conversation_initiation_metadata` payloads
  from the `_event`-suffixed key the server actually sends
  (`conversation_initiation_metadata_event`), with fallbacks to the unsuffixed
  key and flat root for compatibility.

### Changed
- Burned down the detekt/ktlint baselines: ktlint auto-format applied across
  the codebase, wildcard imports replaced with explicit imports, and
  Composable-aware rule configuration added (baselines shrank from ~850 to
  under 40 entries).

### Added
- Compose UI tests for the example app (`StartScreen`, `TextChatScreen`),
  run in the CI emulator job alongside the SDK instrumented tests.
- API reference published to GitHub Pages on every push to `main` (Dokka).
- Pull request template and `CONTRIBUTING.md`.
- Static analysis (detekt + ktlint) wired into CI with baselines.
- Expanded JVM unit test coverage for event parsing, transcript state,
  callbacks/deprecations, token fetching, and configuration.
- Instrumented test scaffold (`androidTest`) exercising SDK initialization and
  the text-only WebSocket mode against a mock server, run on a CI emulator.
- Dokka API reference generation (`./gradlew :elevenlabs-sdk:dokkaHtml`),
  published as a CI artifact.
- Issue templates for bug reports and feature requests.

## [0.11] - 2025

### Added
- Text-only mode over a raw WebSocket transport (`textOnly = true`), including
  support for private agents via `signedUrl`.
- Event-id-aware callbacks: `onUserTranscriptEvent`, `onTentativeUserTranscriptEvent`,
  `onAgentResponseEvent`, `onAgentResponsePartEvent`, `onAgentResponseCorrectionEvent`.
- Reconciled transcript exposed as `ConversationSession.messages: StateFlow<List<Message>>`.
- Software mute pipeline with muted-speech detection (`AudioPipelineConfiguration`).
- VAD score, audio alignment, and agent response metadata callbacks.

### Deprecated
- `onUserTranscript`, `onAgentResponse`, and `onAgentResponseCorrection` in
  favor of the event-id-aware equivalents.

### Changed
- Voice sessions use LiveKit/WebRTC; text-only sessions skip LiveKit entirely.

[Unreleased]: https://github.com/elevenlabs/elevenlabs-android/compare/0.11...HEAD
[0.11]: https://github.com/elevenlabs/elevenlabs-android/releases/tag/0.11
