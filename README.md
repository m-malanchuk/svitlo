# Svitlo

Svitlo is a native Android application for viewing electricity outage schedules published by Zaporizhzhiaoblenergo (ZOE).

The application downloads the public ZOE outage page, extracts the most recent schedule for each date, and presents the result as a 24-hour timeline for the selected queue. No account or authentication is required.

## Features

- Schedules for today and tomorrow.
- Queue selection and a visual 24-hour outage timeline.
- Local caching of the last successful schedule for offline viewing.
- Manual and periodic background refresh from 15 to 1440 minutes.
- Notifications when the selected queue's schedule changes.
- Russian and Ukrainian interfaces.
- Optional 30-minute schedule correction.

## Requirements

- Android 10 (API 29) or newer.
- Internet access to retrieve updates from the public ZOE website.
- Notification permission on Android 13 and newer if schedule-change notifications are desired.

Background refresh is managed by Android WorkManager. Android or device-specific battery restrictions can delay periodic work.

## Installation

No public APK or store package is available yet. A release APK can be attached to a GitHub release after it has been built and signed by the project owner.

## Usage

1. Open Svitlo and select the queue assigned to your address.
2. Switch between today and tomorrow to view the published intervals.
3. Use the refresh button to request the latest schedule immediately.
4. Open settings to change the interface language, refresh interval, or 30-minute correction.

The app keeps the most recently downloaded schedule on the device. When a refresh fails, that saved schedule remains available.

## Data source and privacy

Svitlo reads the public outage page at [zoe.com.ua/outage](https://www.zoe.com.ua/outage/). Schedule parsing happens on the device. The application has no account system and does not send personal information to an application backend.

## License

Svitlo is available under the [MIT License](LICENSE).
