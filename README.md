
# ProtonMail for Android

Copyright (c) 2020 Proton Technologies AG

## License

The code and data files in this distribution are licensed under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See <https://www.gnu.org/licenses/> for a copy of this license.

See [LICENSE](LICENSE) file

## Setup

The most straightforward way to build and run this application is to:

- Install Android Studio: https://developer.android.com/studio/install
- Clone the repository. You have two options:
	- Use the `Project from version control` in Android Studio, or
	- Use the `git clone` command and import it into Android Studio
- Build and run the app directly in Android Studio

Alternatively, if you want to build the app directly from the command line (or using a different IDE, etc.), you will first need to install the command line tools from: https://developer.android.com/studio#cmdline-tools. Then you will need to install the SDK using the `sdkmanager` tool. After cloning the repository with `git clone` you will need to edit the `local.properties` file so that it points to the location of the SDK. Depending on which operating systems you use, the location of the SDK is usually:

- Windows: `C:\Users\<username>\AppData\Local\Android\sdk`
- MacOS: `/Users/<username>/Library/Android/Sdk/`
- Linux: `/home/<username>/Android/Sdk/`

Then, go to the app’s root directory in the command line tool and run:

- `./gradlew assembleBetaDebug`
- `adb install ./app/build/outputs/apk/beta/debug/ProtonMail-Android-1.XX.X-beta-debug.apk`

## Contributions

Contributions are appreciated, but must conform to Proton Guidelines.

#### Branch naming

Branch names must respect the pattern `type/description-of-the-change`.

*Type* must be one of the following:

* `chore` for changes not related to the Kotlin source code, for example a change in the build config
* `doc` for changes related to source code documentation, or external document, like the README
* `feat` for a new feature for the app
* `fix` for bug fixes
* `refactor` for improving one or more unit of code, without impacting the behaviour of the app
* `test` for everything related to test ( add a new test suite, add a new test into an already existing test suite or improve/modify the performance or the behaviour of an already existing test )

_description of change_ must be a concise and meaningful description of what is expected by the change apported; words must be separated by a dash `-`

The whole name of the branch must be lower case.

#### Commit message

The template for a commit message is the following

```
<Title of the commit>

#comment <Description of the changes>
Affected: <List of affected classes or behaviours>
```

_Title_ is required and must start with a capital letter.

`#comment` field is optional if the Title can exhaustively explain the changes, otherwise is required. Body of the comment must start with a capital letter.

`Affected` is optional and must be a comma separated list of the elements affected by the changes, it could be the name of a class or a behaviour like `Encryption, Login flow, LoginActivity.kt`

#### Code style and pattern

The code must conform the standards defined in the files `config/CodeStyle.xml` and `config/detekt.xml`.

`CodeStyle.xml` can be imported in the IDE ( _Preferences -> Editor -> Code Style -> Import scheme_ for IntelliJ and Android Studio ).

Detekt reports can be generated with the command `./gradlew detekt`. Check _Detekt GitHub_ documentation for know how to download and configure the **optional** IDE plugin.



Copyright (c) 2020 Proton Technologies AG

