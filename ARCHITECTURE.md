# Zenith - Architecture Deep Dive 🧠

This document details the internal architecture, data flow, and component interactions of the **Zenith Interview Coach**.

## 1. High-Level System Context

Zenith bridges local Java audio hardware with Google's Cloud AI via the Agent Development Kit (ADK).

![System Context](/Users/sandeep/.gemini/antigravity/brain/d4b1edf5-0b8c-4926-8ff9-51cbbc63913c/zenith_system_context_1767521642146.png)

---

## 2. Internal Component Flow

The application is event-driven, utilizing `RxJava` for reactive stream processing.

![Internal Component Flow](/Users/sandeep/.gemini/antigravity/brain/d4b1edf5-0b8c-4926-8ff9-51cbbc63913c/zenith_internal_flow_1767521665840.png)

---

## 3. Sequence Interaction Logic

This sequence diagram illustrates the lifecycle of a single interaction turn.

![Sequence Diagram](/Users/sandeep/.gemini/antigravity/brain/d4b1edf5-0b8c-4926-8ff9-51cbbc63913c/zenith_sequence_lifecycle_1767521685371.png)

## 4. Key Technical Decisions

### Audio Sampling
-   **Input (Mic)**: `16000Hz`, 16-bit, Mono. Optimized for speech recognition models (ASR).
-   **Output (Speaker)**: `24000Hz`, 16-bit, Mono. Higher fidelity for clearer AI voice generation.

### Concurrency Model
-   **`ExecutorService` (FixedThreadPool)**: Manages two primary background tasks:
    1.  **Input Task**: Continuously polls the microphone so the main thread isn't blocked.
    2.  **Output Task**: Subscribes to the `RxJava` event stream to play audio as it arrives (streaming correlation).

### The "Zenith" Persona
-   Implemented via `System Instructions` in `ZenithAgent.java`.
-   Configured to use **STAR Method** validation.
-   Uses `gemini-2.0-flash-exp` (or `flash-live` when available) for sub-second latency.
