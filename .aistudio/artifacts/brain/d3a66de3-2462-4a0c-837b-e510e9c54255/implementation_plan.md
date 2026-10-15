# VELUM: Optical Studio // 01

A precision AR camera tracing and digital drawing studio for Android, engineered in native Jetpack Compose with CameraX, real-time Sobel line extraction, multi-layer canvas rasterization, and a Teenage Engineering / Leica-inspired tactile industrial interface.

## User Review & Critical Decisions

> [!IMPORTANT]
> Key architectural decisions confirmed through interactive alignment:

- **Confirmed Decision 1 (Architecture)**: Native Jetpack Compose with CameraX, Android Graphics Bitmap convolution pipeline, and custom pointer canvas.
- **Confirmed Decision 2 (Physical Setup)**: Top-down paper tracing stand & Lightbox mode with Touch-Shield Lock (1000ms press-and-hold unlock ring) and High-Luminance Screen Border Light for illuminating real paper.
- **Confirmed Decision 3 (Default Optics Filter)**: Real 3×3 Sobel gradient magnitude line art filter enabled by default on launch with real-time threshold and tint adjustments.
- **Zero-Comment Compliance**: Clean, human-crafted Kotlin and resource files without comments or AI clichés.

---

### 1. Overview & Core Concept

VELUM bridges the physical and digital drawing worlds. Artists and designers place their Android device in a stand over paper (or use the screen as a transparent light table) to trace real-world subjects or curated master studies with sub-millimeter precision.

- **Target Audience / Persona**: Industrial designers, illustrators, tattoo artists, and traditional sketchers who require optical alignment, high-contrast contour extraction, and distraction-free digital inking.
- **Key Value**: Real-time camera contour filtering (Sobel 3×3 gradient magnitude, comic ink stencil threshold, blueprint invert, cel-shaded posterization), multi-layer drawing engine, flicker-assist strobe (0.5Hz–2.0Hz for pen tip visibility), and physical touch-lock for paper tracing.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Launch & Viewport**: Instant camera initialization with fallback to 4 built-in procedural study models (Anatomy Skull, Architecture, Botanical Specimen, Classic Portrait).
2. **Optics & Filters Tuning**: Switch between RAW, Sobel Edge, Stencil Cutoff, Luma Invert, Posterize, and Monochromatic. Adjust edge threshold, contrast, brightness, and tint overlay (Cyan, Vermilion, Emerald).
3. **Workspace Switching**:
   - `AR TRACE`: Live camera with filtered reference overlay and full-screen view for physical sketching.
   - `HYBRID AR`: Live camera underlay + digital drawing canvas + reference overlay.
   - `SCREEN STUDIO`: Pure digital drafting canvas over customizable backdrop.
4. **Drawing & Inking**: 8 specialized tools (Transform, Spline Ink, Charcoal Grain, Chisel Marker, Vector Line/Shape, Eraser, Eyedropper, Symmetry Axis) across 4 composable layers.
5. **Touch-Shield Lock**: One-tap lock for paper tracing. All chrome retracts; only a minimal badge remains. Pressing and holding for 1000ms triggers an aerospace-orange progress ring to unlock.
6. **Composite Export**: Modal dialog offering Composite Render, Transparent Artwork Only, and Trace Stencil Sheet exports with live preview.

#### Visual Identity & Strict Industrial Theme Tokens
- **Backdrop Void**: `#090A0C`
- **Elevated Console Surface**: `#121418` (`rgba(18, 20, 24, 0.92)`)
- **Control Wells & Insets**: `#0D0E11`
- **Hairline Borders**: `1px solid #23262E`
- **Primary Accent**: `#FF4F00` (International Aerospace Orange)
- **Secondary Optical Guides**: `#00E5FF` (Precision Cyan)
- **Warning / Strobe**: `#FFB800`
- **Typography**: Geometric sans-serif labels (`11sp`, uppercase, `0.09em` tracking), Monospace telemetry readouts (`11sp`, tabular-nums, padded `064%`, `+012.4°`, `018PX`).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real-Time Sobel Convolution via Kotlin IntArray Buffer**:
  - *Chosen Approach*: Direct pixel buffer traversal using 3×3 Sobel kernels ($G_x$, $G_y$) running on background coroutines with downsampled preview frames and full-res export caching.
  - *Why*: Ultra-fast, zero external native C++ dependency, guaranteed compatibility across all Android devices and streaming emulators.
- **Decision 2: Offscreen Bitmap Layers vs Vector DisplayList**:
  - *Chosen Approach*: 4 independent Android `Bitmap` layers backed by `Canvas` drawing operations, blended into a composite preview.
  - *Why*: Allows true raster bristle stamps, chisel marker multiply blending, hard/soft alpha eraser, and instant transparent PNG export without memory ballooning.
- **Decision 3: Physical Tracing Shield & Lightbox Ring**:
  - *Chosen Approach*: Full-bleed edge-to-edge lock mode with a 24dp high-luminance pure white border illumination ring to cast light downward onto physical paper in dimly lit studios.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        VELUM Studio Container                          │
├────────────────────────────────────────────────────────────────────────┤
│  Top Telemetry Header (FPS, Zoom, Rotation, Mode Switcher, Lock, Torch)│
├───────────────┬────────────────────────────────────────┬───────────────┤
│ Left Tool     │         Center Interactive Viewport    │ Right Console │
│ Rail (52dp)   │ ┌────────────────────────────────────┐ │ (288dp)       │
│               │ │ 1. CameraX Preview / Fallback Mesh │ │               │
│ - Transform   │ │ 2. Optical Reference Layer         │ │ Tabs:         │
│ - Spline Ink  │ │    (Sobel / Stencil / Invert / Tint│ │ 1. Optics &   │
│ - Grain Brush │ │ 3. Multi-Layer Canvas (4 Bitmaps)  │ │    Filters    │
│ - Chisel Nib  │ │ 4. Optical HUD Grids & Symmetry    │ │ 2. Brush &    │
│ - Vector Tool │ │ 5. Lightbox Illumination Ring      │ │    Color      │
│ - Eraser      │ └────────────────────────────────────┘ │ 3. Layers &   │
│ - Eyedropper  │                                        │    HUD Grid   │
│ - Symmetry    │                                        │               │
├───────────────┴────────────────────────────────────────┴───────────────┤
│  Bottom Tactile Console (Undo/Redo, Reference Ghost Opacity, Strobe)   │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Components
- `VelumStudioScreen`: Top-level coordinator managing layout, window insets, camera lifecycle, and state.
- `CameraXPreviewView`: AndroidView hosting `PreviewView` with permission check and graceful fallback to procedural study models.
- `DrawingCanvasView`: Custom Compose Canvas handling pointer input, Catmull-Rom spline smoothing, bristle stamps, and chisel marks into `BitmapLayerStore`.
- `OpticalFilterEngine`: Fast image processing calculating Sobel gradient magnitude, luminance inversion, threshold binarization, and color tinting.
- `TouchShieldOverlay`: Gesture-absorbing lock screen with 1000ms press-and-hold animated circular unlock ring.
- `ExportDialog`: Multi-mode PNG renderer producing Composite, Transparent Artwork, or Trace Stencil Sheet.
