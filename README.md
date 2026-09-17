# vasmarfas

A personal site and a free multitool in one app. One Kotlin Multiplatform / Compose Multiplatform
codebase builds the site on WebAssembly ([vasmarfas.com](https://vasmarfas.com),
[vasmarfas.ru](https://vasmarfas.ru)) and native apps for Android, iOS, Windows, macOS and Linux.

**Русский:** [`README.ru.md`](README.ru.md) · **Support:** [`SUPPORT.md`](SUPPORT.md) ·
**Privacy:** [`privacy-policy.md`](privacy-policy.md) · **Terms:** [`terms.md`](terms.md)

## What is inside

A card page with contacts, projects, articles and a CV, and a catalogue of offline tools in 13
categories with search, favourites and recents. Anything that can be computed locally runs
everywhere; sockets and sensors run where the OS provides them, and the catalogue marks each tool
with the platforms it supports.

| Category | Examples |
|---|---|
| Network | ping (ICMP/TCP/HTTP), traceroute, port and LAN scanners, DNS over HTTPS and UDP, whois/RDAP, subnet calculator, HTTP requests, TLS certificates, speed test, Wake-on-LAN, interfaces and Wi-Fi |
| Converters | units, number bases, data sizes, currencies, cooking measures, clothing sizes, numbers to words, Roman numerals |
| Calculators | scientific and programmer, percentages, tips, discounts and VAT, loans, compound interest, BMI, fuel, Ohm's law, resistors, LEDs, wire gauge, aspect ratios |
| Text | case, counters, lines, find and replace, transliteration, Morse, ciphers, diff, cleanup, Unicode, ASCII |
| Developer | JSON, Base64, URL and punycode, JWT, UUID, hashes and HMAC, regex, cron, chmod, HTTP codes, MIME, CSV, escaping, semver, Markdown |
| Security | password generator and strength, breach check (HIBP k-anonymity), TOTP, random data, checksum comparison |
| Colour and design | colour converter, palettes, contrast, Material 3 schemes, gradients, QR and barcodes, typography, CSS units |
| Time | stopwatch, timers and Pomodoro, date maths, Unix time, world clock, sunrise and sunset, calendar, working hours |
| Measure and sensors | compass, level, accelerometer, magnetometer, lux meter, barometer, pedometer, GPS, ruler, protractor, sound meter, tone generator, spectrum analyzer, metronome |
| Device | device info, battery, display, screen test, brightness and torch, touch tester, vibration, clipboard, benchmarks, stress test |
| Everyday | unit price, expense splitting, water, sleep, decision wheel, dice, counters, notepad, countdown |
| 3D printing | print and resin cost, filament length and weight, layer and extrusion width, E-steps and flow, shrinkage, temperature tower, time estimate, G-code cheatsheet |
| Sport and health | workout plans, interval timer, one-rep max, pace, heart rate zones, calorie burn, body metrics, macros, blood pressure, HbA1c, VO2 max, caffeine, blood alcohol |

Texts come in Russian and English and switch instantly. The card content is data rather than code:
`profile.json` and `resume.json` ship inside the app and are refreshed from the network at startup,
so the site and installed apps pick up changes without a release.

## Stack

Kotlin 2.4, Compose Multiplatform 1.12, Material 3 Expressive (`MaterialExpressiveTheme`,
`MotionScheme`, `WideNavigationRail`, `ShortNavigationBar`, wavy indicators), navigation-compose
bound to browser history, Ktor 3, kotlinx-serialization, kotlinx-datetime, material-kolor (scheme
from a seed colour, dynamic colours on Android 12+), qrose (QR and barcodes). The web target is
`wasmJs` only.

## License

The source is available under [PolyForm Strict License 1.0.0](LICENSE) with the additions in
[NOTICE.md](NOTICE.md). This is a personal site and a personal app: the code is published so it can
be read and audited, not reused. Reading it, building it for yourself and using the official builds
are fine; modifying and distributing it are not. Pull requests are still welcome, and forking to
prepare one is the normal way to do that. Third-party components are listed in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
