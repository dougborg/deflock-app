# Deflock Load Tests

Gatling load tests for validating [`overpass.deflock.org`](https://overpass.deflock.org) performance before rolling it out as the primary Overpass API endpoint for all Deflock app users.

## What is this?

The Deflock app fetches surveillance camera data from the [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API) every time a user pans or zooms the map. We've deployed our own Overpass instance at `overpass.deflock.org` to reduce dependence on the public endpoint. These load tests validate that our instance can handle realistic traffic patterns before we switch users over to it.

The tests use [Gatling](https://gatling.io), an open-source load testing framework. Gatling simulates virtual users sending HTTP requests and produces detailed HTML reports with latency percentiles, error rates, and throughput metrics.

## Quick start

### Prerequisites

- **JDK 21+** — install via [SDKMAN](https://sdkman.io) (`sdk install java 21-tem`) or your package manager
- **No other tools needed** — Gradle and Scala are handled automatically by the included wrapper and build config

Or use the included [dev container](#dev-container) to skip local setup entirely.

### Run the tests

```bash
cd load-tests
./gradlew gatlingRun
```

This takes about 10-15 seconds (6 sequential requests with 500ms pauses between them). When finished, Gatling prints a `file://` URL to the HTML report — open it in your browser.

The report lands in `build/reports/gatling/<simulation-name-timestamp>/index.html`.

### Run via GitHub Actions

1. Go to the **Actions** tab in GitHub
2. Select the **"Load Test"** workflow
3. Click **"Run workflow"**
4. When complete, download the **gatling-report** artifact (retained for 30 days)

## How the test works

### The simulation

A single virtual user walks through map zoom levels from **z15** (a few city blocks) down to **z10** (a metro region). At each zoom level, it picks a random US city and sends the same Overpass API query that the Deflock app sends when a user views that area on the map.

This zoom progression reveals how response time scales with viewport size — larger viewports contain more surveillance nodes, producing bigger API responses.

### Test data

Six US cities were chosen for high surveillance camera density in their downtown areas:

| City | Center coordinates | Landmark |
|---|---|---|
| Denver | 39.75, -105.00 | 16th St Mall / Union Station |
| Los Angeles | 34.05, -118.25 | Pershing Square, DTLA |
| San Francisco | 37.79, -122.40 | Financial District |
| New York | 40.75, -73.98 | Midtown / 42nd & 6th Ave |
| Boston | 42.36, -71.06 | Downtown Crossing |
| Chicago | 41.88, -87.63 | State & Madison, The Loop |

### Zoom levels and viewport sizes

Each zoom level corresponds to a different viewport size on a typical mobile phone screen (~400x800px, portrait):

| Zoom | Area covered | Lat x Lng span |
|---|---|---|
| 15 | A few city blocks (~1.5 x 3 km) | 0.026 x 0.017 deg |
| 14 | A neighborhood (~3 x 6 km) | 0.053 x 0.034 deg |
| 13 | A district (~6 x 12 km) | 0.105 x 0.069 deg |
| 12 | A mid-size city (~12 x 23 km) | 0.210 x 0.140 deg |
| 11 | A large city (~23 x 47 km) | 0.420 x 0.270 deg |
| 10 | A metro region (~47 x 93 km) | 0.840 x 0.550 deg |

## Interpreting the report

The Gatling HTML report includes several views. Here's what to look for:

### Key metrics

- **p50 (median) latency** — what a typical user experiences
- **p95 latency** — should be under 10s for a good user experience
- **p99 latency** — should be under 30s (the assertion threshold)
- **Error rate** — should be 0% under single-user load

### Report sections

- **Response time distribution** — histogram showing how many requests fell into each latency bucket
- **Response time percentiles over time** — trend lines for p50/p75/p95/p99 throughout the test
- **Requests per second** — throughput over the test duration
- **Individual request details** — click any request name (e.g., "Overpass z15 - Denver") to see its specific metrics

### What "good" looks like

From our baseline runs, typical single-user performance is:

| Zoom | Expected latency |
|---|---|
| z15 (blocks) | ~400-600ms |
| z13-z14 (neighborhood) | ~600-1000ms |
| z10-z11 (city/metro) | ~1000-1600ms |

## Planned scenarios

| Scenario | Description | Status |
|---|---|---|
| Single-user zoom progression | Baseline latency at each zoom level | Current |
| Concurrent users | Ramp up multiple users to find capacity limits | Planned |
| Stress test | Push beyond expected capacity to find breaking points | Planned |

## Project structure

```
load-tests/
├── .devcontainer/           # VS Code dev container (JDK 21 + Scala)
│   ├── devcontainer.json
│   └── Dockerfile
├── build.gradle.kts         # Build config (Gatling + Scala plugins)
├── settings.gradle.kts      # Gradle project name
├── gradlew / gradlew.bat    # Gradle wrapper (no global install needed)
├── gradle/wrapper/          # Gradle wrapper jar + config
├── src/gatling/
│   ├── scala/deflock/
│   │   ├── OverpassSimulation.scala  # The simulation (test scenario)
│   │   ├── OverpassRequests.scala    # HTTP request definitions
│   │   └── TestData.scala            # City coordinates + zoom feeders
│   └── resources/
│       ├── gatling.conf              # Gatling charting config
│       └── logback-test.xml          # Log level config (WARN by default)
└── build/reports/gatling/   # Generated HTML reports (.gitignored)
```

## Dev container

If you don't want to install JDK locally, the included dev container provides a ready-to-go environment:

1. Open the `load-tests/` folder in VS Code
2. Install the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
3. Press `Ctrl+Shift+P` → "Dev Containers: Reopen in Container"
4. Wait for the container to build (first time takes a few minutes)
5. Open a terminal and run `./gradlew gatlingRun`

The container includes JDK 21, Scala (via [Coursier](https://get-coursier.io)), and VS Code extensions for Scala (Metals) and Gradle.
