# Privacy Policy

**Effective Date:** 2026-09-18

**vasmarfas** ("we," "our," or "us") is a personal website and a free multitool published as a
web app at vasmarfas.com and vasmarfas.ru and as native applications for Android, iOS, Windows,
macOS and Linux (together, the "App"). This Privacy Policy explains what the App does with your
information.

## 1. What we collect

The App has no accounts, no sign-in, no advertising and no server of ours that receives data from
you. It does collect usage statistics, crash reports and startup timings through third-party
services. The collection starts with the App and there is no switch inside it that stops it.

The App sends these events and nothing else:

- `app_open` — the App was started;
- `screen_view` — a screen was opened (`screen`: `home`, `projects`, `resume`, `tools`, `settings`);
- `tool_open` — a tool was opened (`tool`: the tool identifier from the catalogue, e.g. `ping`);
- `tool_action` — an action inside a tool (`tool` and `action`, both fixed identifiers);
- `language_change`, `theme_change` — the interface language or the theme was switched.

Event parameters may contain only identifiers matching `[a-z0-9_-]` — tool and screen names from the
App's own catalogue. Whatever you type into a tool — addresses, domains, passwords, text, numbers —
is never part of an event.

The statistics are collected and processed by their operators, not by us:

- **Google, Firebase Analytics** (Android) and **Firebase Analytics for the web** (website).
  Collects an app instance identifier, device model, OS and application version, the country derived
  from the IP address, and session data. https://policies.google.com/privacy
- **Google, Firebase Crashlytics** (Android). On a crash it sends the stack trace, the device model,
  the OS and application version and an installation identifier.
- **Google, Firebase Performance Monitoring** (Android). Measures startup time, screen rendering and
  the duration of the App's own network requests.
- **Yandex, Yandex.Metrica** (website). Sets its own cookie and collects the IP address, user agent,
  visited pages and the referrer. Session replay (Webvisor) and the click map are enabled in the
  counter's configuration, so your interaction with the page is recorded.
  https://yandex.com/legal/metrica_termsofuse/

The desktop and iOS builds contain no analytics code: there `logEvent` does nothing.

## 2. Data stored on your device

The App keeps a small amount of data locally so that it works the way you left it:

- settings — language, theme and accent colour;
- the list of favourite and recently opened tools;
- data you enter into tools that offer to remember it — saved Wake-on-LAN devices, world clock
  zones, counters, the scratchpad, countdown events, calibration values;
- a cached copy of the profile file (`profile.json`) that describes the site's content;
- a cached copy of currency exchange rates.

On Android this lives in the App's private storage, on iOS in the App's user defaults, on desktop in
the user's preferences store, and in the browser in `localStorage` of the site's origin. None of it
is synced or sent anywhere. Uninstalling the App, or clearing the site's data in the browser, removes
it.

## 3. Network requests

Apart from the analytics traffic described in section 1, the App makes network requests only when you
open a tool that needs them or explicitly start an action. Each request goes directly from your
device to the third party named below; we operate none of these services and receive nothing from
them.

- **Profile refresh** — on start the App downloads the latest `profile.json` from
  `raw.githubusercontent.com` (the project's public repository) so that the site's text can be
  updated without a release. The request contains no personal data.
- **DNS lookup, public DNS benchmark** — queries are sent to the resolver you choose
  (Cloudflare, Google, Quad9, AdGuard over DNS-over-HTTPS, or any server you enter over UDP).
  The resolver sees the domain name you typed.
- **Whois / RDAP** — the domain, IP address or AS number you typed is sent to `rdap.org` and the
  registry it redirects to, and, where sockets are available, to whois servers on port 43.
- **IP info** — `ipwho.is` (with `ipapi.co` as a fallback) receives the IP address you typed, or,
  when you ask for your own address, the request itself reveals your public IP to that service.
- **MAC address lookup** — the address prefix you typed is sent to `api.maclookup.app`.
- **Speed test** — random data is downloaded from and uploaded to `speed.cloudflare.com`; the
  uploaded bytes carry no information.
- **HTTP request** — the request you compose is sent to the URL you entered, exactly as written.
- **Ping (HTTP mode), TLS certificate** — connect to the host you entered.
- **Currency converter** — exchange rates are fetched from `open.er-api.com`; the request contains
  no personal data.
- **Have I Been Pwned check** — only the first five characters of the SHA-1 hash of the password
  you typed are sent to `api.pwnedpasswords.com`; the password itself never leaves your device.

Tools that work with your local network (ping, traceroute, port scanner, LAN scanner,
Wake-on-LAN, device discovery, network interfaces) talk only to the addresses you specify or to
devices on your own network.

## 4. Device permissions and sensors

Some tools use device capabilities, always after you open the tool and, where the platform
requires it, after you grant the permission:

- **Location** (Android, iOS, browser) — the GPS tool shows your coordinates and speed on screen;
  on Android the same permission is required by the system to read the Wi-Fi network name. The
  location is not stored or transmitted.
- **Motion and environment sensors** — compass, level, accelerometer, magnetometer, light,
  barometer and pedometer read the sensors and display the values. Nothing is recorded beyond the
  short history shown in the chart.
- **Microphone** — the sound meter computes a loudness level from the audio input on the fly;
  no audio is recorded or saved.
- **Camera flash** — the torch tool toggles the LED; the camera itself is not used and no image is
  captured.
- **Vibration** — used by the vibration test and by a few tools as haptic feedback.
- **Network state** — used to show the connection details in the network interfaces tool.

## 5. Third-party services

The App uses the analytics services listed in section 1, plus the services listed in section 3,
which you trigger yourself. There are no others. The source code is
public on GitHub under a source-available license, so you can verify this yourself.

## 6. Children's privacy

The App is not directed at children and asks nobody, including children under 13, for personal
information. Google and Yandex receive the device and session data listed in section 1 regardless of
the user's age.

## 7. Your rights

We hold no personal data about you ourselves: we have no server, no accounts and no access to the
analytics raw data beyond the aggregated reports in the Firebase and Yandex.Metrica
consoles, which carry no name, e-mail or any other detail that identifies you.

The App has no switch that stops the collection. In the browser you can block the counters with an
extension, or clear the site's cookies and `localStorage`, which removes the Yandex.Metrica
identifier; on Android you can reset the advertising identifier in the system settings. Events that
were already sent are stored by Google and Yandex under their own terms — request access or deletion
from them directly through the links in section 1.

## 8. Changes to this policy

We may update this Privacy Policy from time to time. Material changes will be reflected by a new
Effective Date at the top of this document and published together with the release that introduces
them.

## 9. Contact

**Developer:** vasmarfas
**Contact:** https://github.com/vasmarfas/toolbox/issues
