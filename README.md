# Teleport

<img src="assets/icon/teleport-icon-512.png" width="96" align="right" alt="The icon of Teleport: a portal ring with an arrow flying out of it">

An Android app whose launcher icon is a button you configure. Tap it to open the app you choose, or a link that deep-links into a specific screen. Long-press the icon for a menu with your actions and the settings. Teleport itself stays out of the way: the tap runs the action and finishes without showing a window.

## Features

- Tap the icon to open your chosen app or link.
- Long-press the icon for the shortcut menu: settings plus your actions, with the target app icons. Most launchers show up to four entries; the order in the settings decides which appear.
- Add any launchable app from a searchable picker, or a URL / deep link with its own label.
- Reorder actions, pick the one the icon tap opens, remove what you no longer need.
- Drag a menu entry to the home screen as a separate pinned icon.
- If the target is gone, Teleport opens its settings with a message instead of failing silently.
- No permissions, no network, no ads or analytics. The configuration stays on the device.

## Install

Teleport needs Android 8.0 (API 26) or later. Download the APK of the latest version from [Releases](https://github.com/juev/teleport/releases) and open it on the phone.

## Build

Requirements: JDK 21 and the Android SDK with platform 37.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # or any JDK 21
./gradlew assembleDebug testDebugUnitTest lintDebug
```

`local.properties` points Gradle at the SDK (`sdk.dir=...`); Android Studio writes it on its own.

The release is shrunk by R8. It is signed when `TELEPORT_KEYSTORE`, `TELEPORT_KEYSTORE_PASSWORD`, `TELEPORT_KEY_ALIAS` and `TELEPORT_KEY_PASSWORD` are set, and unsigned otherwise.

## Release

The version and its code live in `gradle.properties`, as `teleport.version` and `teleport.versionCode`; F-Droid reads them there to find new releases. A release goes in two steps:

1. A PR raises both lines and adds `fastlane/metadata/android/*/changelogs/<version code>.txt`.
2. After it is merged, a tag `v<version>` on its commit runs the Release workflow: the tests, then the signed APK, published as a GitHub release with notes from the commits since the previous tag. A tag that differs from `teleport.version` fails the workflow.

A version `1.2.3-rc.4` with the tag `v1.2.3-rc.4` is a release candidate, published as a pre-release. Every release raises the version code: the candidate 1.2.3-rc.4 gets 1020304, the final 1.2.3 gets 1020399, above all of its candidates. The build fails when `teleport.versionCode` is not the code of `teleport.version`.

The workflow takes the key from the repository secrets `TELEPORT_KEYSTORE_BASE64` (the keystore in Base64), `TELEPORT_KEYSTORE_PASSWORD`, `TELEPORT_KEY_ALIAS` and `TELEPORT_KEY_PASSWORD`.

The same workflow builds an Android App Bundle for Google Play, signed with the upload key from `TELEPORT_UPLOAD_KEYSTORE_BASE64`, `TELEPORT_UPLOAD_KEYSTORE_PASSWORD`, `TELEPORT_UPLOAD_KEY_ALIAS` and `TELEPORT_UPLOAD_KEY_PASSWORD`, and keeps it as a workflow artifact. Play signs the APKs it serves with the key of the GitHub APK, so an install from either place updates the other.

## Store listing

The texts and pictures of F-Droid and Google Play live in `fastlane/metadata/android/` (English and Russian). A release adds `changelogs/<version code>.txt` in both languages, at most 500 characters. The icon and feature graphics are rendered from `assets/icon/` (see `assets/README.md`); the phone screenshots are taken on an emulator.

## Privacy

What Teleport stores is in the [privacy policy](docs/privacy.md). It stores only its configuration, sends nothing over the network and requests no permissions.

## License

[MIT](LICENSE).
