
# ProtonMail for Android

Copyright (c) 2020 Proton Technologies AG

## License

The code and data files in this distribution are licensed under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See <https://www.gnu.org/licenses/> for a copy of this license.

See [LICENSE](../../../../../../../../LICENSE) file

### UI tests structure:

UI tests code is divided into three main packages: 
1. **robots** package - contains ***Robot***s classes where each Robot represents a single application screen, i.e. ***Compose new email*** functionality. All pop-up modals that are triggered from this functionality belong to the same Robot and represented by inner classes within one Robot class, i.e. ***Set email password modal***. If an action triggers new Activity or Fragment start then it is considered as new Robot. 
2. **tests** package - contains test classes split by application functionality, i.e. ***login*** or ***settings***.
3. **testsHelper** package - contains test data and helper classes.

### Running tests

In order to run UI tests locally the ***private.properies*** file should contain a list of four test users and four recipients credentials in the following format

`TEST_USER1="email,password,mail_password,2fa"`

`TEST_RECIPIENT1="email,password,mail_password,2fa"`

or the same parameters must be added to the gradle command when running from terminal

`./gradlew cDAT -PUSER1="email,password,mail_password,2fa" -PRECIPIENT1="email,password,mail_password,2fa"` 

Similarly, in case when test results should be reported to TestRail or tests are going to be run on Browserstack, the 3rd party platforms credentials must be provided:   

`TESTRAIL_PROJECT_ID="number"`

`TESTRAIL_USERNAME="username"`

`TESTRAIL_PASSWORD="password"`

`BROWSERSTACK_USER="user"`

`BROWSERSTACK_KEY="key"`

or the same parameters must be added to the gradle command

`./gradlew cDAT -PBROWSERSTACK_USER="user" -PBROWSERSTACK_KEY="key"`
