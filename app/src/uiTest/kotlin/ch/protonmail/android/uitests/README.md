
# ProtonMail for Android

Copyright (c) 2020 Proton Technologies AG

## License

The code and data files in this distribution are licensed under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See <https://www.gnu.org/licenses/> for a copy of this license.

See [LICENSE](../../../../../../../../LICENSE) file

In order to run Espresso UI tests the below data should be provided as instrumentation test arguments:

#### via the command line using **Android gradle wrapper**
- Set of 4 test users: <br>`-Pandroid.testInstrumentationRunnerArguments.USER1="email,password,mailbox_password,auth_code"`
- Set of 4 recipients: <br>`-Pandroid.testInstrumentationRunnerArguments.RECIPIENT1="email,password,mailbox_password,auth_code"`
- TestRail enable/disable boolean flag: <br>`-Pandroid.testInstrumentationRunnerArguments.REPORT_TO_TESTRAIL=false`
- When above is `true` the following arguments should also be present:

    `-Pandroid.testInstrumentationRunnerArguments.TESTRAIL_PROJECT_ID=id`<br>
     `-Pandroid.testInstrumentationRunnerArguments.TESTRAIL_USERNAME=username`<br>
     `-Pandroid.testInstrumentationRunnerArguments.TESTRAIL_PASSWORD=password`<br>
     
The final command may look like the following:

`./gradlew app:connectedBetaDebugAndroidTest \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.USER1="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.USER2="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.USER3="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.USER4="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.RECIPIENT1="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.RECIPIENT2="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.RECIPIENT3="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.RECIPIENT4="email,password,mailbox_password,auth_code" \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.REPORT_TO_TESTRAIL=true \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.TESTRAIL_PROJECT_ID=id \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.TESTRAIL_USERNAME=username \`<br>
 `-Pandroid.testInstrumentationRunnerArguments.TESTRAIL_PASSWORD=password`<br>
     
#### via the Android Studio **Run/Debug Configurations**
1. In Android studio menu select **Run->Edit Configurations**.
2. Select or create **Android Instrumented Tests** by clicking **+** button.
3. Select **Instrumentation arguments** on the right side of **Run/Debug Configurations** window by clicking **...** button.
3. Add **Value - Pair** arguments.
4. Apply changes and save.

For the future convenience you can update **Android Instrumented Tests** template with needed arguments, so it can be used by default for any newly created run configuration.