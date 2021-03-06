# UniversalMusicPlayer
A music player designed to integrate local songs on the computer with songs linked on the internet, all under one interface.

# Requirements
Users will need both FFMPEG and VLC media player installed in order to run this program

https://ffmpeg.org/download.html

https://www.videolan.org/vlc/

This program has been tested on Windows and Linux. While MacOS may be supported, it will not as of yet run out of the box (primarily due to extra work needed to get Firefox properly installed there).

![Interface Preview](https://raw.githubusercontent.com/Markil3/UniversalMusicPlayer/master/UniversalMusicPlayer1.png)
![Artists View](https://raw.githubusercontent.com/Markil3/UniversalMusicPlayer/master/UniversalMusicPlayer2.png)
![Viewing Single Artist](https://raw.githubusercontent.com/Markil3/UniversalMusicPlayer/master/UniversalMusicPlayer3.png)
![Adding a Song](https://raw.githubusercontent.com/Markil3/UniversalMusicPlayer/master/UniversalMusicPlayer4.png)

## Using
This program has both a GUI and a CLI. If you launch multiple instances, any commands entered in the secondary instances will be forwarded to the main application.

You can view command-line options on [the wiki](https://github.com/Markil3/UniversalMusicPlayer/wiki/Command-Line-Interface).

## How it Works
This program integrates two background programs for audio. Local files are played through VLC media player, courtesy of the [VLCJ](https://github.com/caprica/vlcj) library. This is done with the [edu.regis.universeplayer.player.LocalPlayer](https://github.com/Markil3/UniversalMusicPlayer/blob/master/interface/src/main/java/edu/regis/universeplayer/player/LocalPlayer.java) class. Playback commands are forwarded to VLC in the background.

Internet-based songs (YouTube, etc.) work a little differently. Most providers require users to view their content directly through their website. As such, a browser is needed. To accomplish this, the build script will download and install a local copy of Firefox Developer Edition, with a custom addon to facilitate communications between the music player and the browser. Since Firefox addons cannot directly communicate with a running application, the installer will also install a small intermediary application that will automatically connect to a running instance of the music player and forward messages back and forth. All of this is handled by [edu.regis.universeplayer.player.BrowserPlayer](https://github.com/Markil3/UniversalMusicPlayer/blob/master/interface/src/main/java/edu/regis/universeplayer/player/BrowserPlayer.java).

The Firefox program uses a separate profile with its own settings to isolate it from any existing Firefox installations. It can be found under the profile name "Universal."

Both of these solutions are seamlessly integrated under [edu.regis.universeplayer.player.PlayerManager](https://github.com/Markil3/UniversalMusicPlayer/blob/master/interface/src/main/java/edu/regis/universeplayer/player/PlayerManager.java).

## Building
To start, just download the repository. Then, cd into the project directory and run "./gradlew :interface:run" (without the quotes).

## Installing
To create an installer, run "./gradlew buildscriptNix" for Linux. Windows build scripts are pending.

To run, launch "universalplayer."

## Todo list
This project is still in early development, and there are still important features that are needed before it can replace anything else I'm using.

* Stabilize the YouTube interface
* Add support for playLists
* Fix the scrolling bug for Artists/Album views
* Add a song search utility in the GUI
* Add better error checking for the song add dialogue.
* Add support for Spotify and Amazon Music
* Expand the CLI, with the goal of parity between the two interfaces.
* A Windows installer (as soon as I learn how to write batch scripts...)

## License
Copyright (c) William Hubbard. All rights reserved.
