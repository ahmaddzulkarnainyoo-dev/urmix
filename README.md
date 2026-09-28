<p align="center"><a href="https://winatra.com/urmix"><img src="assets/new_pipe_icon_5.png" width="150"></a></p>
<h2 align="center"><b>URMIX</b></h2>
<h4 align="center">A libre lightweight streaming front-end for Android. Developed by WINATRA, built on NewPipe Core.</h4>

<p align="center">
<a href="https://www.gnu.org/licenses/gpl-3.0" alt="License: GPLv3"><img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg"></a>
<a href="https://github.com/Winatra/URMIX/actions" alt="Build Status"><img src="https://github.com/Winatra/URMIX/actions/workflows/build.yml/badge.svg?branch=main&event=push"></a>
</p>

<hr>
<p align="center"><a href="#screenshots">Screenshots</a> &bull; <a href="#supported-services">Supported Services</a> &bull; <a href="#description">Description</a> &bull; <a href="#features">Features</a> &bull; <a href="#installation-and-updates">Installation and updates</a> &bull; <a href="#contribution">Contribution</a> &bull; <a href="#donate">Donate</a> &bull; <a href="#license">License</a></p>
<p align="center"><a href="https://winatra.com/urmix">Website</a> &bull; <a href="https://github.com/Winatra/URMIX">Source code</a> &bull; <a href="https://saweria.co/winatra">Donate</a></p>
<hr>

> [!warning]
> <b>THIS APP IS IN BETA, SO YOU MAY ENCOUNTER BUGS. IF YOU DO, OPEN AN ISSUE IN OUR GITHUB REPOSITORY.</b>
> 
> <b>URMIX IS DISTRIBUTED OUTSIDE THE GOOGLE PLAY STORE (GITHUB RELEASES + TELEGRAM). ANY PLAY STORE COPY IS UNOFFICIAL.</b>

## Screenshots

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/00.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/00.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/01.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/02.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/03.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/04.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/05.png)
<br/><br/>

### Supported Services

URMIX (via the [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor)) supports these services:

* YouTube ([website](https://www.youtube.com/)) and YouTube Music ([website](https://music.youtube.com/))
* PeerTube ([website](https://joinpeertube.org/)) and all its instances
* Bandcamp ([website](https://bandcamp.com/))
* SoundCloud ([website](https://soundcloud.com/))
* media.ccc.de ([website](https://media.ccc.de/))

## Description

URMIX fetches the required data from the official API of the service you're using. If the official API is restricted (e.g. YouTube) or proprietary, the app parses the website or uses an internal API instead. This means you don't need an account on any service to use URMIX.

Neither the app nor the extractor use any proprietary libraries or frameworks, such as Google Play Services. This means URMIX works on devices or custom ROMs that do not have Google apps installed.

URMIX is a transparent fork of [NewPipe](https://github.com/TeamNewPipe/NewPipe) (GPLv3): the streaming core, extraction layer and much of the architecture are NewPipe's work, rebranded and extended by WINATRA.

### Features

* Watch videos at resolutions up to 4K
* Listen to audio in the background, only loading the audio stream to save data
* Popup mode (floating player, aka Picture-in-Picture)
* Watch live streams
* Show/hide subtitles/closed captions
* Search videos and audios (on YouTube, you can specify the content language as well)
* Enqueue videos (and optionally save them as local playlists)
* Show/hide general information about videos (such as description and tags)
* Show/hide next/related videos
* Show/hide comments
* Search videos, audios, channels, playlists and albums

<span id="updates"></span>

## Installation and updates

1. Download the APK from [GitHub Releases](https://github.com/Winatra/URMIX/releases) or from the **official Telegram group**, and install it.
2. Updates arrive in-app (soft update banner) or as a Telegram announcement — download the new APK from the same source and install it **over** the old version.

> [!IMPORTANT]
> All official releases are signed with **one consistent key**. Installing the new APK over the old one keeps your local data (history, subscriptions, playlists, downloads). If you ever install an APK signed with a *different* key, Android will refuse the upgrade and you would have to uninstall first — **losing local data** — so always verify you are installing an official build. The expected signer fingerprint is published with each release (see [doc/RELEASE.md](doc/RELEASE.md)).

Building a debug APK yourself (`./gradlew assembleDebug`) installs side-by-side the official app and cannot update it.

### Backing up your data

1. Back up your data via Settings → Backup and Restore → Export Database so you keep your history, subscriptions, and playlists.
2. Import the data on the other device via Settings → Backup and Restore → Import Database.

## Contribution

Whether you have ideas, translations, design changes, code cleaning, or even major code changes, help is always welcome. If you'd like to get involved, check our [contribution notes](.github/CONTRIBUTING.md).

## Donate

If you like URMIX, you're welcome to send a donation via [Saweria](https://saweria.co/winatra). You can also donate from inside the app (drawer → Donation), which shows the QRIS code.

## Privacy Policy

URMIX aims to provide a private, anonymous experience for using web-based media services. The app does not collect any data without your consent. Crash reports are only sent when you explicitly choose to send them. Like NewPipe: no account, no tracking, no Google Play Services.

## License

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)

URMIX is Free Software: You can use, study, share, and improve it at will. Specifically you can redistribute and/or modify it under the terms of the [GNU General Public License](https://www.gnu.org/licenses/gpl.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Portions originate from NewPipe (org.schabi.newpipe), © the NewPipe contributors, also GPLv3.

* Browse videos and audios within a channel
* Subscribe to channels (yes, without logging into any account!)
* Get notifications about new videos from channels you're subscribed to
* Create and edit channel groups (for easier browsing and management)
* Browse video feeds generated from your channel groups
* View and search your watch history
* Search and watch playlists (remote playlists fetched from the service you're browsing)
* Create and edit local playlists (saved within the app)
* Download videos/audios/subtitles (closed captions)
* Watch/Block age-restricted material
* Local JSON backup & restore of your library (settings → import/export)
