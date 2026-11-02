# DEV AI — Personal AI Android Assistant

DEV AI is a native Android AI assistant developed by **DEV L KAKKOTH** with the tagline *"Your Personal AI. Your Phone. Your Control."* It pairs an OpenRouter-powered **NVIDIA Nemotron 3 Ultra** reasoning model with an autonomous, native Android tool execution engine for real device control (flashlight, app launching, Storage Access Framework file management, system settings, and volume/telemetry) along with speech-to-text and text-to-speech voice interactions.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were established during Phase 1 clarification:
> - **OpenRouter API Key Configuration**: User configures their key directly in the in-app Settings screen, complete with a live "Test Connection" button, model selection (`nvidia/nemotron-3-ultra-550b-a55b`), and secure local preference storage.
> - **Voice Assistant Interaction**: Auto-speak responses aloud whenever voice input is initiated via the microphone or dedicated voice interface, with an instant "Stop Speaking" floating control.
> - **File Search & Storage Access**: Powered by Android Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) with persistent URI permissions to respect modern Android security policies while supporting recursive folder search, file inspection, renaming, and launching.
> - **GitHub Actions**: A `.github/workflows/build-apk.yml` workflow will be created to build signed/unsigned APK artifacts on push.

---

## 1. Overview & Core Concept

DEV AI turns the Android smartphone into an AI-orchestrated command center. Rather than a passive text chatbot, DEV AI is an active agent that reasons through user requests, invokes structured Android platform tools, inspects real device state, and acts on behalf of the user within explicit permission boundaries.

- **Primary Persona**: Power users, students, and professionals wanting quick, natural hands-free voice and text control over their apps, local documents, device hardware, and daily questions.
- **Key Value**: Real hardware and OS integration (launching apps, torch, deep SAF folder searching, system toggles, audio control) powered by OpenRouter Nemotron 3 Ultra with zero simulated mock data.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Theme Posture**: Dark-first luxury futuristic aesthetic.
- **Color Palette**:
  - Background: Deep Charcoal (`#090D16`) with midnight navy canvas gradient (`#0F172A`).
  - Surface & Cards: Elevated dark slate glass cards (`#1E293B` at 80% alpha, `#111827`) with subtle border strokes (`#334155`).
  - Primary Accent: Neon Electric Cyan (`#00E5FF`) and Cyber Purple (`#8B5CF6`).
  - Status Indicators: Emerald Green (`#10B981`) for online/success, Amber (`#F59E0B`) for warnings, Crimson (`#EF4444`) for errors.
- **Typography & Layout**:
  - Clean M3 typography scale with bold, futuristic headers.
  - Generous spacing (16.dp padding, 12.dp card corners), full-bleed edge-to-edge support with `WindowInsets.safeDrawing`.
  - Minimum 48.dp interactive touch targets on all buttons and micro-controls.

### Core Screens & Flows
1. **Splash Screen**:
   - Animated DEV AI cyber-ring logo entrance.
   - Tagline: *"Your Personal AI. Your Phone. Your Control."*
   - Subtitle: *"Developed by DEV L KAKKOTH"*.
2. **Main Assistant Chat & Agent Screen**:
   - Top Bar: DEV AI glowing badge, active model status, network connection indicator, settings gear, and new chat action.
   - Quick Action Chips: Fast triggers for "Turn on Torch", "Open WhatsApp", "Search Documents", "Battery Info", "Wi-Fi Settings".
   - Dynamic Message List:
     - User message bubbles (cyber-purple accent).
     - DEV AI response cards (deep glass card with glowing cyan avatar).
     - **Live Tool Execution Cards**: Shows running tool name (e.g. `turn_flashlight_on`, `search_files`), arguments, execution spinner, and verified success/failure status badges.
     - Destructive Action Confirmation Modals (for file deletion, overwrite, or phone call intent).
   - Bottom Bar: Fluid text field, voice wave recording button, and send action.
3. **Dedicated Voice Assistant Modal**:
   - Immersive full-screen overlay with pulsing reactive audio visualizer.
   - Live speech transcription display.
   - AI thinking pulse animation.
   - Active TTS speech playback indicator with one-tap "Stop" button.
4. **Settings & Preferences Screen**:
   - OpenRouter API configuration (hidden key input with visibility toggle, "Test Connection" ping).
   - Model ID customization (defaults to `nvidia/nemotron-3-ultra-550b-a55b`).
   - Voice parameters: Auto-speak toggle, speech rate slider, pitch slider, language picker.
   - Storage Access Framework: "Manage Authorized Folders" with folder picker launcher.
   - Chat session manager: Clear history, export transcript, Room database statistics.
   - **About Section**: Full branding, version 1.0, tagline, and developer credit (*"Developed by DEV L KAKKOTH"*).

---

## 3. Key Product Decisions & Trade-Offs

| Area | Chosen Strategy | Rationale | Alternatives Considered |
|---|---|---|---|
| **AI Backend** | OpenRouter Chat Completions (`POST /chat/completions`) using `nvidia/nemotron-3-ultra-550b-a55b` with OpenAI-compatible tool definitions. | Strict user prompt requirement; provides high-tier reasoning and native function calling. | Gemini API (explicitly forbidden by user prompt). |
| **Agent Loop** | Multi-round tool orchestration loop (up to 6 sequential rounds) validated on Android. | Allows complex chained actions (e.g., search folder -> find file -> open file). | Single-shot prompt; fails when intermediate step data is required. |
| **File Access** | Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE` + persistable URIs + `DocumentFile`). | Privacy-safe, compliant with Android 11+ Scoped Storage; requires zero dangerous runtime storage permissions. | Legacy `READ_EXTERNAL_STORAGE` (fails Play Store policies on Android 13+). |
| **Chat Persistence** | Room Database (`SessionEntity`, `MessageEntity`, `ToolCallEntity`). | Robust, structured offline relational store with Coroutines/Flow integration. | SharedPreferences or raw JSON file (prone to corruption and hard to query). |
| **Voice Engine** | Native Android `SpeechRecognizer` + `TextToSpeech`. | 100% on-device voice processing with zero latency or third-party audio API costs. | Cloud audio streaming (high latency, costly, requires extra API keys). |

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DEV AI Android Application                      │
├────────────────────────────────────────────────────────────────────────┤
│  Presentation Layer (Jetpack Compose + Material 3)                     │
│  ┌───────────────────────┐  ┌─────────────────┐  ┌──────────────────┐  │
│  │ MainChatScreen        │  │ VoiceModalSheet │  │ SettingsScreen   │  │
│  │ - QuickActionChips    │  │ - AudioVisual   │  │ - OpenRouter Key │  │
│  │ - ToolExecutionCard   │  │ - LiveTranscript│  │ - Model & Voice  │  │
│  └───────────▲───────────┘  └────────▲────────┘  └────────▲─────────┘  │
│              │                       │                    │            │
├──────────────┼───────────────────────┼────────────────────┼────────────┤
│  ViewModel & State Management (StateFlow / Coroutines)                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ AssistantViewModel (Orchestrator, Chat State, Voice State)       │  │
│  └───────────────────────────────────▲──────────────────────────────┘  │
│                                      │                                 │
├──────────────────────────────────────┼─────────────────────────────────┤
│  AI Agent & Tool Execution Engine    │                                 │
│  ┌─────────────────────────────────┐ │ ┌─────────────────────────────┐ │
│  │ OpenRouterApiClient             │ │ │ ToolRegistry                │ │
│  │ - Nemotron-3-Ultra Chat Endpoint│ │ │ - Tool definition schemas   │ │
│  │ - Tool calls & response parsing ├─┴─┤ - Argument validation       │ │
│  └─────────────────────────────────┘   │ - Safety & confirmation gate│ │
│                                        └──────────────┬──────────────┘ │
│                                                       │                │
├───────────────────────────────────────────────────────┼────────────────┤
│  Native Android Platform Integrations                 │                │
│  ┌─────────────────┐ ┌────────────────┐ ┌─────────────┴──────────────┐ │
│  │ FlashlightManager│ │ AppLaunchManager│ │ SafFileManager             │ │
│  │ - CameraManager │ │ - PackageManager│ │ - DocumentFile recursive   │ │
│  │ - Torch callback│ │ - ResolveIntent │ │ - MIME type open intent    │ │
│  └─────────────────┘ └────────────────┘ └────────────────────────────┘ │
│  ┌─────────────────┐ ┌────────────────┐ ┌────────────────────────────┐ │
│  │ SystemSettings  │ │ AudioManagerCtl │ │ DeviceTelemetryManager     │ │
│  │ - Official Ints │ │ - Volume streams│ │ - Battery, storage, network│ │
│  └─────────────────┘ └────────────────┘ └────────────────────────────┘ │
│  ┌─────────────────┐ ┌────────────────┐                                │
│  │ VoiceSpeechRecog│ │ AndroidTTS     │                                │
│  └─────────────────┘ └────────────────┘                                │
├────────────────────────────────────────────────────────────────────────┤
│  Local Persistence Layer (Room + DataStore)                            │
│  ┌─────────────────────────┐  ┌──────────────────────────────────────┐ │
│  │ DevAiDatabase (Room)    │  │ DevAiPreferences (SharedPreferences) │ │
│  │ - ChatSessionEntity     │  │ - OpenRouter API Key                 │ │
│  │ - ChatMessageEntity     │  │ - Model ID & Voice Settings          │ │
│  │ - ToolHistoryEntity     │  │ - Authorized SAF Folder URIs         │ │
│  └─────────────────────────┘  └──────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Complete Tool Suite in ToolRegistry
1. **Application Management**:
   - `list_installed_apps(filter: string?)`: Returns launchable user applications.
   - `open_app(app_name: string)`: Finds application by name or package and launches intent.
   - `get_app_info(app_name: string)`: Returns version, package name, and install date.
   - `open_app_settings(app_name: string)`: Opens system application details settings page.
2. **Flashlight Control**:
   - `turn_flashlight_on()`: Activates camera flash torch with safety fallback.
   - `turn_flashlight_off()`: Disables torch.
   - `get_flashlight_status()`: Queries current torch status.
3. **File Manager & Search (SAF)**:
   - `list_authorized_folders()`: Shows currently accessible folder roots.
   - `request_folder_access()`: Signals UI to launch Android folder picker.
   - `search_files(query: string, extension: string?, folder_name: string?)`: Recursively searches files in authorized document trees.
   - `list_folder_contents(folder_uri: string?)`: Inspects children of a directory.
   - `get_file_info(file_name: string)`: Size, last modified date, MIME type.
   - `open_file(file_name: string)`: Fires `Intent.ACTION_VIEW` with appropriate MIME type.
   - `create_folder(folder_name: string, parent_uri: string?)`: Creates a new child directory.
   - `delete_file(file_name: string, confirmation: boolean)`: Deletes file after user confirmation.
4. **System Settings**:
   - `open_wifi_settings()`, `open_bluetooth_settings()`, `open_display_settings()`, `open_sound_settings()`, `open_battery_settings()`, `open_location_settings()`, `open_accessibility_settings()`.
5. **Audio Management**:
   - `get_volume_levels()`: Media, ring, notification, and alarm volume percentages.
   - `set_media_volume(level_percent: int)`: Sets media stream volume.
6. **Device Telemetry**:
   - `get_battery_status()`: Percentage, charging status, power save mode.
   - `get_device_storage_information()`: Internal storage total and free space.
   - `get_network_status()`: Wi-Fi, Cellular, or Offline state.
   - `get_current_time()` / `get_current_date()`.
7. **Action Utilities**:
   - `open_url(url: string)`: Opens browser intent.
   - `share_text(text: string)`: Triggers system share sheet.

---

## 5. Verification Plan

1. **Local Compilation (`compile_applet`)**:
   - Compile code with Android Gradle Plugin to ensure zero Kotlin/KSP syntax or dependency errors.
2. **Tool Execution Verification**:
   - Verify tool schemas and JSON serialization for OpenRouter compatibility.
   - Validate fallback handling when OpenRouter API key is not yet configured (clean user banner directing to Settings).
3. **Robolectric Unit Testing (`gradle :app:testDebugUnitTest`)**:
   - Test `ToolRegistry` parameter parsing and schema validity.
   - Test `AgentOrchestrator` multi-step tool call execution.
   - Test Room database persistence for chat messages and sessions.
4. **GitHub Actions Workflow**:
   - Create `.github/workflows/build-apk.yml` configured to build the APK cleanly using Gradle.
