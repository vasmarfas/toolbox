Required Notice: Copyright 2026 vasmarfas

This file supplements [LICENSE](LICENSE). The license text is an unmodified PolyForm Strict License 1.0.0 and controls whenever the two disagree; this file adds the required copyright notice, explains the license in plain language, and adds a branding restriction that a copyright license doesn't cover on its own.

## What the license means in practice

Not legal advice, just a summary — read [LICENSE](LICENSE) for the actual terms.

This is a personal site and a personal app. The source is published so it can be read, audited, and learned from, not so it can be reused. PolyForm Strict is the tightest of the PolyForm licenses: it grants use for noncommercial purposes and nothing else.

- Reading the code, running the official builds, and running your own local build for personal study or experiment are permitted.
- Changing the software or building anything on top of it is **not** permitted. That covers patches you keep to yourself, translated forks, re-themed versions, and copying pieces of this code into another project.
- Distributing the software is **not** permitted, modified or not. Only the maintainer publishes builds.
- Any commercial use is **not** permitted.

Anything outside that needs a separate written agreement with the copyright holder.

## Contributing

Pull requests are still welcome. Forking [github.com/vasmarfas/toolbox](https://github.com/vasmarfas/toolbox) on GitHub to prepare one is the normal contribution flow, and this notice doesn't restrict it: keep the fork for as long as you're working toward a pull request and sync it with upstream. What isn't permitted is keeping a fork as an independent product — publishing it, promoting it, or shipping it to anyone.

By submitting a contribution you agree that it becomes part of this project on the same basis as the rest of the codebase, and that the maintainer may use it in published builds without the license restricting the maintainer's own use of the combined work.

## Name, icon and package ID

The name **vasmarfas**, the name **vasmarfas Toolbox**, the app icon, and the application ID `com.vasmarfas.toolbox` identify official builds distributed by the maintainer. They are not covered by the code license and may not be used to label, describe, or identify any other build or distribution, nor to imply endorsement by or affiliation with this project.

## Third-party components

The app bundles third-party components — Kotlin, Compose Multiplatform, Skiko, Ktor, OkHttp, kotlinx, AndroidX, material-kolor, qrose and the Firebase SDK — each under its own license, in most cases Apache-2.0 or MIT. Two are not open-source licenses: Firebase Analytics is covered by the Android Software Development Kit License, and desktop installers bundle a Temurin 21 runtime under GPLv2 with the Classpath Exception. The website additionally loads Yandex Metrica from mc.yandex.ru, which is a hosted service rather than a bundled library. Those licenses apply to those components and are unaffected by this notice. The full list, with the license text each one requires, is in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
