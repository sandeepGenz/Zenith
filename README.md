# Zenith - Peak Performance Interview Coach ⛰️

> **"Practice like you play."**

Zenith is a real-time, voice-activated AI Agent designed to simulate high-stakes executive interviews. Built with the **Google Agent Development Kit (ADK)** and the **Gemini 2.0 Multimodal Live API**, it listens to your answers and provides immediate, actionable coaching feedback on both content and delivery.

## 🏗️ Architecture

Zenith operates as a bi-directional streaming application. It captures raw audio from your microphone, streams it to the Gemini model via the ADK Runner, and plays back the AI's generated audio response instantly.

```mermaid
graph TD
    User([User 👤]) <-->|Audio Waves| Hardware[Microphone & Speaker 🎤/mw]
    Hardware <-->|PCM Data| JavaApp[ZenithRun.java]
    
    subgraph "Application Layer (Local)"
        JavaApp -->|1. Capture Audio| Queue[LiveRequestQueue]
        Queue -->|2. Stream Input| ADK[ADK Runner]
        ADK -->|3. Receive Events| JavaApp
    end
    
    subgraph "Cloud Layer (Google Cloud)"
        ADK <-->|WebSocket (Bi-Di)| Gemini[Gemini 2.0 Flash ⚡\n(Multimodal Live API)]
    end
    
    style Gemini fill:#4285F4,stroke:#333,stroke-width:2px,color:white
    style User fill:#34A853,stroke:#333,stroke-width:2px,color:white
    style ADK fill:#FBBC05,stroke:#333,stroke-width:2px,color:white
```

## ✨ Standout Features

-   **Real-time Bi-Directional Audio**: No "push to talk". Just speak naturally. Zenith handles interruptions and turn-taking.
-   **"Peak Performance" Persona**: Unlike generic assistants, Zenith is instructed to be an authoritative executive coach. It uses the **STAR method** (Situation, Task, Action, Result) to rigorously evaluate your answers.
-   **Low Latency**: Powered by `gemini-2.0-flash-exp` for near-instant voice responses.

## 🚀 Getting Started

### Prerequisites

-   **Java 17+**
-   **Maven**
-   **Google Cloud API Key** (or Service Account)

### Installation

1.  **Clone the repository** (or download source).
2.  **Export your API Key**:
    ```bash
    export GOOGLE_API_KEY="your_actual_api_key_here"
    ```
    *(Get a free key from [Google AI Studio](https://aistudio.google.com/app/apikey))*

3.  **Build the project**:
    ```bash
    mvn clean install
    ```

### Running the Coach

```bash
mvn exec:java -Dexec.mainClass="samples.liveaudio.ZenithRun"
```

1.  Wait for the prompt: `>>> Zenith is listening...`
2.  **Speak**: "Hi Zenith, I'm ready for a mock interview question."
3.  **Listen**: Zenith will respond via your speakers.
4.  **End**: Press `Enter` to stop.

## 🛠️ Troubleshooting

-   **Quota Exceeded?**: The Gemini 2.0 experimental model has rate limits. If you see a quota error, wait a minute and try again, or ensure your Google Cloud project has billing enabled (even for the free tier).
-   **Microphone Error?**: Ensure your terminal has permission to access the Microphone (System Settings > Privacy & Security > Microphone).

---
*Built with ❤️ using Google ADK.*
