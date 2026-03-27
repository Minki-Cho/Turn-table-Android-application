# PicklyDeck Privacy Policy

Last updated: 2026-03-27

PicklyDeck is an on-device Android remote for compatible media sessions. It does not stream music and does not include online lyrics lookup in the shipping build.

## Data PicklyDeck Accesses

- Media-session metadata exposed by the currently active player on your device, such as track title, artist, album art, playback position, playback state, and available transport actions
- Notification listener access, so Android can expose compatible media sessions to PicklyDeck
- Notification permission on Android 13 and newer, so PicklyDeck can show its ongoing controls notification
- Local app preferences and widget snapshot data, such as the selected deck theme, selected visual mode, and the last track information shown in the widget

## How PicklyDeck Uses This Data

- To render the turntable UI and now-playing information
- To power widget, lock-screen, and notification controls
- To remember your local UI preferences between launches

## What PicklyDeck Does Not Do

- It does not create a PicklyDeck account
- It does not upload playback metadata to external servers
- It does not sell or share personal data
- It does not perform online lyrics lookups in the shipping app build
- It does not use analytics or advertising SDKs in the shipping app build

## Data Storage

PicklyDeck stores its preferences and widget snapshot locally on the device. The shipped app build disables Android cloud backup for this app data.

If you uninstall PicklyDeck, its locally stored preferences and widget snapshot are removed as part of app removal.

## Third-Party Services

The shipped app build does not rely on third-party online services for its core playback-remote functionality.

## Your Choices

- You can stop PicklyDeck from reading media-session data at any time by revoking notification access in Android settings.
- On Android 13 and newer, you can revoke notification permission in Android settings to stop PicklyDeck's ongoing notification from appearing.
- You can uninstall the app to remove its locally stored preferences and widget snapshot.

## Contact

For privacy questions, use the support email listed on the Play Store page or the release support channel you publish with the app.

## Publishing Note

The GitHub Pages version of this policy should live at `/privacy-policy/` on the configured custom domain.

Preferred public URL:

`https://picklydeck.minki-portfolio.info/privacy-policy/`
