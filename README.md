# VELUM: Optical Studio // 01

A high-precision, industrial-grade AR Camera Tracing and Digital Drawing Studio built for Android. Designed with a bespoke hardware aesthetic inspired by Leica camera firmware and Teenage Engineering interfaces, VELUM bridges physical paper tracing and digital creation.

---

## Key Features

- **AR Trace & Hybrid Modes:** Overlay reference images or live camera feeds directly onto your workspace for physical paper tracing or digital painting.
- **Real-Time Optical Convolution Engine:** Non-destructive image processing including a true 3x3 Sobel edge-detection filter for instant line-art extraction, comic stencil thresholds, luma inversion, and 4-band posterization.
- **Multi-Layer Raster Engine:** 4 independent offscreen bitmap layers featuring velocity-smoothed Catmull-Rom spline inking, procedural bristle grain brushes, angled chisel markers, vector shapes, alpha erasing, and 8-way radial symmetry.
- **Physical Touch-Shield Lock:** Retracts all UI chromes for unobstructed physical paper tracing, secured by a 1-second press-and-hold animated radial unlock badge to prevent accidental palm inputs.
- **Strobe & Flicker Assist:** Smooth 0.5Hz to 2.0Hz sine-wave oscillation of reference opacity to maintain optical persistence, allowing artists to clearly see both the physical pencil tip and digital reference lines simultaneously.
- **High-Luminance Lightbox Ring:** Pure white illumination border mode to cast light downward onto dark-room sketchbooks.
- **Built-In Procedural Study Presets:** Instant vector study presets (Anatomy Skull, Perspective Architecture, Botanical Specimen, Portrait Proportions) and optical calibration viewfinders.
- **Studio Export Suite:** Export composite renders, transparent artwork layers, or high-contrast trace stencil sheets with side-by-side previews.

---

## Architecture & Tech Stack

- **Platform:** Android (Native Jetpack Compose)
- **Camera Pipeline:** CameraX with real-time frame analysis and RGBA bitmap conversion
- **Graphics & Rendering:** Custom Canvas/Bitmap rendering pipeline with 25-step undo/redo snapshot buffers
- **Design System:** Precision dark industrial palette (#090A0C void backdrop, #FF4F00 aerospace orange accents, #00E5FF optical guides, and monospace tabular telemetry readouts)

---

## Project Structure

```text
com.example/
├── camera/          # CameraX stream and frame management
├── graphics/        # Optical filter convolution engine and multi-layer drawing engine
├── model/           # App state models, brushes, layers, and presets
└── ui/
    ├── components/  # Modular hardware-inspired UI widgets (Tool rail, inspector, scrub console, etc.)
    └── theme/       # Industrial color tokens and typography
