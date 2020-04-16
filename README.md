
# ProtonMail for Android

Copyright (c) 2020 Proton Technologies AG

## License

The code and data files in this distribution are licensed under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See <https://www.gnu.org/licenses/> for a copy of this license.

See [LICENSE](LICENSE) file

## Setup

The most straightforward way to build and run the project is by following these steps: 

- Install Android Studio: https://developer.android.com/studio/install
- Clone the repository:
	- either by using the `Project from version control` in Android Studio or 
	- using the `git clone` command and importing it into Android Studio
- Build and run the project directly from Android Studio

Alternatively, if you want to build directly from the command line (or using a different IDE, etc.), you will first need to install the command line tools from: https://developer.android.com/studio#cmdline-tools and install the SDK by using the `sdkmanager` tool. After cloning the repository with `git clone` you will need to edit the `local.properties` file so that it points to the location of the SDK, which for the various operating systems most usually is:

- Windows: `C:\Users\<username>\AppData\Local\Android\sdk`
- MacOS: `/Users/<username>/Library/Android/Sdk/`
- Linux: `/Users/<username>/Android/Sdk/`

Then, after moving into the project's root directory, you can simply run:

- `./gradlew assembleBetaDebug`
- `adb install ./app/build/outputs/apk/beta/debug/ProtonMail-Android-1.XX.X-beta-debug.apk`

Copyright (c) 2020 Proton Technologies AG

