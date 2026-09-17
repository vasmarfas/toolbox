# Third-party notices

Components redistributed in official builds of this project, with the license each one is
covered by. This file supplements [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md): the terms below
apply to those components, not to this project's own code.

Versions are the ones pinned in [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Apache License 2.0

Full text: [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)

| Component | Copyright |
|---|---|
| Kotlin standard library, `kotlin-test` | JetBrains s.r.o. and Kotlin Programming Language contributors |
| kotlinx.coroutines, kotlinx.serialization, kotlinx-datetime, kotlinx-browser | JetBrains s.r.o. |
| Compose Multiplatform — runtime, foundation, material3, ui, ui-backhandler, components-resources, material-icons-extended | The Android Open Source Project, JetBrains s.r.o. |
| Skiko | JetBrains s.r.o. |
| navigation-compose, lifecycle-viewmodel-compose, lifecycle-runtime-compose (JetBrains multiplatform builds) | The Android Open Source Project, JetBrains s.r.o. |
| androidx.core:core-ktx, androidx.activity:activity-compose | The Android Open Source Project |
| Ktor (client core, OkHttp, Darwin, JS engines) | JetBrains s.r.o. |
| OkHttp | Square, Inc. |
| Firebase Crashlytics, Firebase Performance Monitoring | Google LLC |

## MIT License

**material-kolor** — Copyright (c) 2025 Jordon de Hoog

**qrose** — Copyright (c) 2023 Alexander Zhirkevich

> Permission is hereby granted, free of charge, to any person obtaining a copy of this software
> and associated documentation files (the "Software"), to deal in the Software without
> restriction, including without limitation the rights to use, copy, modify, merge, publish,
> distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
> Software is furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all copies or
> substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
> BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
> NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
> DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## BSD 3-Clause

**Skia** — Copyright (c) 2011 Google Inc. Ships as a native binary inside Skiko; see
<https://github.com/google/skia/blob/main/LICENSE>.

## Not open-source licenses

**Firebase Analytics** (`com.google.firebase:firebase-analytics`, which pulls in
`play-services-measurement`) is distributed under the Android Software Development Kit License
Agreement, <https://developer.android.com/studio/terms>. It permits building and distributing
applications; it is not covered by this project's own license, and anyone building from source
obtains it directly from Google on those terms.

**Bundled Java runtime.** Desktop installers produced by `jpackage` embed an Eclipse Temurin 21
runtime image, licensed under GPLv2 with the Classpath Exception,
<https://openjdk.org/legal/gplv2+ce.html>. The exception is what permits bundling it with an
application under different terms.

## Ported benchmark code

The benchmark tool contains Kotlin ports of three reference implementations, kept faithful so the
scores stay comparable with published ones:

- **Whetstone** — from the netlib C source, <https://netlib.org/benchmark/whetstone.c>, itself a
  translation of the Curnow and Wichmann ALGOL original (1976). Public domain.
- **Linpack** — from the netlib Java version by Jack Dongarra and co-authors,
  <https://netlib.org/benchmark/linpackjava/>. Public domain.
- **SciMark 2.0** — from the NIST ANSI C sources, <https://math.nist.gov/scimark>. Work of the
  U.S. National Institute of Standards and Technology and not subject to copyright in the
  United States.

The SHA-256, memory-copy and Mandelbrot kernels beside them are not ports of anything and are not
standard benchmarks.

## Development-only

**JUnit 4** (`junit:junit`) is used for tests under the Eclipse Public License 1.0,
<https://www.eclipse.org/legal/epl-v10.html>. It is a test dependency and is not present in any
published build.

## Website

vasmarfas.com and vasmarfas.ru load Yandex Metrica from `mc.yandex.ru`. It is a hosted service
called over the network, not a library bundled into the site.
