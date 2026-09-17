# vasmarfas

The personal site of vasmarfas and a free multitool in one application. A single Kotlin Multiplatform /
Compose Multiplatform codebase builds into a WebAssembly site (vasmarfas.com, vasmarfas.ru) and into
native apps for Android, iOS, Windows, macOS and Linux.

**Русская версия:** [`README.md`](README.md) · **Support:** [`SUPPORT.md`](SUPPORT.md) ·
**Privacy:** [`privacy-policy.md`](privacy-policy.md) · **Terms:** [`terms.md`](terms.md)

## What is inside

**Business card.** Home with contacts and an about section, projects and articles, résumé (experience,
education, skills), settings. Russian and English with an instant switch. All card content lives in one
file, `shared/src/commonMain/composeResources/files/profile.json`: the app ships a bundled copy and
refreshes it from the repository on start, so both the site and installed apps pick up changes without
a release.

**Tools.** A catalog of 143 tools in 13 categories with search, favorites and recents. Every tool is tagged with the
platforms it works on; anything that can be computed locally works everywhere, sockets and sensors work
where the OS provides them.

| Category | Examples |
|---|---|
| Network | ping (ICMP/TCP/HTTP), traceroute, port scanner, LAN scanner, DNS lookup (DoH and UDP), whois/RDAP, my IP, subnet calculator and splitter, HTTP requests, TLS certificate, speed test, Wake-on-LAN, UPnP/Bonjour, interfaces and Wi-Fi, MAC vendor, port and DNS references |
| Converters | units, number bases, data sizes, currencies, cooking measures, clothing sizes, numbers to words, Roman numerals |
| Calculators | scientific and programmer, percentages, tips, discounts and VAT, loans, compound interest, BMI, fuel, Ohm's law, resistors, LEDs, dividers, batteries, wire gauges, aspect ratios, age |
| Text | case, counters, lines, find and replace, transliteration, Morse, ciphers, diff, cleaner, Unicode, ASCII |
| Developer | JSON, Base64, URL and punycode, JWT, UUID, hashes and HMAC, regex, cron, chmod, HTTP status codes, MIME, CSV, escaping, semver, key events, Markdown |
| Security | password generator and strength, breach check (HIBP, k-anonymity), TOTP, random data, checksum compare |
| Color and design | color converter, palettes, contrast, Material 3 scheme, gradients, QR and barcodes, typography, CSS units |
| Time | stopwatch, timer and Pomodoro, date calculator, Unix time, world clock, sunrise and sunset, calendar, working hours |
| Measure and sensors | compass, level, accelerometer, magnetometer, light meter, barometer, pedometer, GPS, ruler, protractor, sound meter, tone generator, metronome |
| Device | device info, battery, display, screen test, screen light and torch, touch tester, vibration, clipboard |
| Everyday | unit price, bill split, water, sleep, decision wheel, dice, counters, scratchpad, countdowns |
| 3D printing | print and resin cost, filament length and weight, layer and extrusion width, E-steps and flow, shrinkage, temperature tower, time estimate, G-code cheat sheet |
| Sport and health | your own workout plans, interval timer, one-rep max, pace and race predictions, heart-rate zones, calorie burn, body metrics, macros |

The catalog itself shows per-platform status: tools unavailable on the current platform are
marked in the list.

## Stack

Kotlin 2.4, Compose Multiplatform 1.12, Material 3 Expressive (`MaterialExpressiveTheme`,
`MotionScheme`, `WideNavigationRail`, `ShortNavigationBar`, wavy indicators), navigation-compose bound
to browser history, Ktor 3, kotlinx-serialization, kotlinx-datetime, material-kolor (scheme from a seed
color, dynamic color on Android 12+), qrose (QR and barcodes). Web is `wasmJs` only; the JS target was
dropped.

## Build

```bash
./gradlew :androidApp:assembleDebug
```

```bash
./gradlew :desktopApp:run
```

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

```bash
./gradlew :webApp:wasmJsBrowserDistribution
```

The site ends up in `webApp/build/dist/wasmJs/productionExecutable`. iOS: open `iosApp` in Xcode.
Tests: `./gradlew :shared:jvmTest`. JDK 21; everything else comes from the Gradle wrapper.

Version and signing come from the environment: `APP_VERSION_NAME`, `APP_VERSION_CODE`,
`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Deployment

- `deploy_web.yml` — on push to `main` builds the Wasm distribution and publishes it to GitHub Pages
  (vasmarfas.com, `CNAME` is added on the fly) and to Yandex Object Storage (vasmarfas.ru) with
  `aws s3 sync`. Secrets: `YC_S3_ACCESS_KEY_ID`, `YC_S3_SECRET_ACCESS_KEY`, optionally `YC_S3_BUCKET`.
  Pages must be set to the "GitHub Actions" source and the bucket to static-website mode with
  `index.html` as both index and error document.
- `deploy_android.yml`, `deploy_desktop.yml`, `deploy_ios.yml` — manual per-platform builds with
  optional publishing to Google Play / RuStore / App Store Connect.
- `release.yml` — builds APK, MSI/EXE, DEB/RPM and DMG and creates a GitHub Release.
- `ci.yml` — tests and compilation of every target on each PR.

## Layout

```
shared/     shared code: core/ (expect/actual layer), data/ (profile, settings), ui/, tools/<category>/
androidApp/ desktopApp/ webApp/ iosApp/   entry points
.github/workflows/                        build and publishing
```

## License

The source is available under the [PolyForm Strict License 1.0.0](LICENSE) with additions in
[NOTICE.md](NOTICE.md). This is a personal site and a personal app: the code is published so it can be
read and audited, not reused. Reading it, building it for yourself and using the official builds are
fine; modifying or redistributing it is not. Pull requests are still welcome, and forking to prepare
one is the normal flow.
