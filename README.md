# Svitlo

Svitlo is a native Android application for viewing hourly electricity outage schedules from the Zaporizhzhiaoblenergo public outage page.

The project is currently in active development. The first release is planned for Android 10 and newer and includes queue selection, today/tomorrow schedules, a 24-hour outage timeline, local caching, background refresh, notifications, themes, language selection, and a home-screen widget.

## Current source

The application reads `https://www.zoe.com.ua/outage/`. A captured HTML page used for parser development is stored in [`fixtures/zoe-outage.html`](fixtures/zoe-outage.html).

The parser selects the latest article revision for each date, extracts queue identifiers and outage intervals, and ignores archive text that does not contain queue-specific intervals.

## Platform and behavior

- Android 10 (API 29) and newer.
- No account or authentication is required.
- The last successful schedule is stored locally on the device.
- Background refresh is configurable, with a minimum of 15 minutes because Android does not guarantee shorter periodic WorkManager intervals.
- The UI and widget support Russian and Ukrainian, plus light, dark, and system themes.

There is no release APK or store distribution yet.
