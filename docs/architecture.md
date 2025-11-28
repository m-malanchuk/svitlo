# Svitlo architecture

## Scope

The first release is a single native Android application. It has no account system and no application backend. The app fetches the public ZOE outage page directly, parses it locally, stores the normalized result on the device, and exposes the result in the main screen and a home-screen widget.

## Components

- **Compose UI** — queue selection, today/tomorrow tabs, status card, 24-hour timeline, theme controls, language controls, and refresh settings.
- **ZOE parser** — an isolated Jsoup-based parser for `article[id^=post-]` entries, Ukrainian dates, queue identifiers, update times, and exact outage intervals.
- **Local store** — JSON in app-private `SharedPreferences`. The data set is small enough for the parsed archive page, and the store avoids a database migration surface in the first release.
- **Refresh worker** — WorkManager with a connected-network constraint. The interval is user-configurable from 15 to 1440 minutes.
- **Widget** — Jetpack Glance reads the same local store and renders a compact 24-hour bar.
- **Notifications** — a notification is emitted when the selected queue's stored schedule changes and Android notification permission is available.

## Data model

Outage times are exact integer minutes from local midnight. Valid intervals have a start in `0..1439`, an end in `1..1440`, and `start < end`; `24:00` is represented as minute `1440`. UI geometry may use floating-point coordinates only for drawing proportions derived from these exact values.

For a date with multiple source articles, the parser chooses the article with the latest explicit `оновлено HH:mm` time. If no update time is present, the first article in the source order wins.

## Operational limitations

Android WorkManager defines a 15-minute minimum for periodic work, and device manufacturers may delay or stop background work under aggressive battery-saving policies. The app should therefore provide a battery-optimization hint if background refresh is delayed on MIUI devices.
