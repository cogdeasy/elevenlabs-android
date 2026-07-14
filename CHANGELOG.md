# Changelog

All notable changes to the ElevenLabs Android SDK are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
(pre-1.0: minor versions may contain breaking changes).

## [Unreleased]

### Added
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
