

# IRSmartHub
A project with the goal of creating a solution to the many IR-controlled devices in our home. This project makes those devices "smart", allowing them to be controlled from any android phone (soon to be any device with a browser). This is achieved by creating a device containing an IR receiver/transmitter and ESP8266 wifi board that speaks to a Firebase realtime database backend. In addition, a Firestore database is used to contain all user/device data. This allows for a "serverless" implementation with all commands running through Firebase.
## Data Structures
The following describe how data is stored and handled in this project:
### User
Contains all information with respect to a specific user using this service. 
 - **Unique id**: The uid of the associated user account 
 - **Username**: *used for allowing other users to invite/connect without sharing email*
 - **Groups**: *a list of groups the user is associated with*
### Remote Profile
Contains a collection of pre-programmed actions
- **Name**: A custom name given by the creator of the remote profile
- **Hub**: The uid of the hub associated with this remote
- **Buttons**: a list of remote functions (either *actions* or *commands*). Remote functions can be static, pre-defined (i.e. VOL UP) or custom
### Command
Contains raw data for a single IR command
- **Raw Data**: A string of raw data for the given command
- **Raw Data Length**: The length of the raw data string. Used by the IR Hub when parsing
### Action
Contains data for a specific action. An action consists of one or more *Commands* in succession. An action can also specify a specific IR Hub to target for each command.
- **Commands**: An ordered list of <Command, IR Hub> pairs. If a command is not associated with an IR Hub, the command is sent to the user's currently selected hub. 
### Group
Contains a collection of users, remote profiles, and associated hubs. This is used for sharing hubs between multiple users. Each group has a group owner and 0 or more users. Other users can be given specific permissions. 
- **Remote Profiles**: *a collection of profiles shared among the group*
- **Connected Hubs**: *a collection of hubs associated with the group*
- **Owner**: *uid of the original creator of the group*
- **Users**: *a collection of users with set permissions. Permission types are as follows:*
	- ***Add Devices***: Allows user to add hubs to the group. Users can only add hubs that they set up.
	- ***Remove Devices***: Allows user to remove hubs from the group
	- ***Add Users***: Allows user to add other users to the group. Added users can only have the same (or less) permissions as the user who added them.
	- ***Remove Users***: Allows user to remove other users from the group.
	- ***Add Remote Profiles***: Allows user to add remote profiles to the group. User can remove profiles they have added at any time.
	- ***Remove Remote Profiles***: Allows user to remove any remote profile shared with the group.
## Firebase
## Android Application
This is the primary front-end user interface. From it, users can create an account that allows them to: 
- Link/Enable interaction with IRSmartHubs
- Create custom actions to hubs as well as remote profiles (aka, custom remote controllers)
- Create groups, allowing multiple users to share hubs/remote profiles
### Current App Development Progress
The splash screen/sign-in screen is fully operational *(highlighted in **green** in the following flow diagram)*. Additional passes on it will be done at a later time to refine the experience. Fetching user data and showing the appropriate walk-through screens is currently under development *(highlighted in **yellow** in the following flow diagram)*. After cursory funtionality is built out for that, development on creating usable remote profiles is next. This will allow physical testing of the IRSmartHub and App in conjunction!

### App Flow Diagrams
#### Splash Screen / Initialization Process
This initial phase is responsible for authenticating the user and fetching initial user data. A sign in screen is displayed when there are no user credentials on device. Once the user has signed in, the firebase firestore is queried for user data. An appropriate screen is then displayed to the user based on what kind of data (or lack thereof) is returned.
<img src="https://i.imgur.com/aQLYwju.png"
     style="float: center; margin-right: 10px;"
/>

#### Create Remote Profile
Creating a remote profile involves three main steps: Creating a layout, assigning it to a group, and uploading the created remote profile to firebase.
<img src="https://i.imgur.com/qIBXngl.png"
     style="float: center; margin-right: 10px;"
/>
The following is a more in-depth UI flowchart showing the user's experience when creating a remote profile.
<img src="https://i.imgur.com/0PYhHxj.png"
     style="float: center; margin-right: 10px;"
/>
## Physical Device
The physical device is essentially a Wifi connected IR receiver / blaster. The hardware consists of a simple IR LED setup, ESP8266 board, and IR receiver. Utilizes a Firebase backend for a "serverless" implementation. 

### Feature List
- Dynamically learn SSID/Password via SoftAP setup from any wifi-connected device 
- Send programmable IR signals via Android App **(WIP)**
- Learn IR signals and send to Android App **(WIP)**
- Handle sending repeat signals (simulating "holding down" a remote button) **(WIP)**

### IRFunctions
The currently planned functions are:
- record IR signal
- send single IR signal
- send repeat signal

#### Record IR Signal
Listens for complete IR signals recorded from `IRrecv` ([see IRremoteESP8266](https://github.com/markszabo/IRremoteESP8266/blob/master/src/IRrecv.h)). This function ignores any recognized "repeat" commands. If no signal is recorded after `IR_READ_TIMEOUT` seconds ([see ArduinoIRFunctions](https://github.com/MattS8/IRSmartHub/blob/master/arduino/IRSmartHub-arduino/ArduinoIRFunctions.h)), a timeout error is sent. If the signal is too large, an overflow error is sent. Assuming none of the previous conditions are met, the recorded raw data is sent ([see ArduinoFirebaseFunctions](https://github.com/MattS8/IRSmartHub/blob/master/arduino/IRSmartHub-arduino/ArduinoFirebaseFunctions.h))

#### Send IR Signal
Current implementation sends straight raw data at a fixed frequency. Most likely, future implementations will have protocol-specific behavior. That all depends with how well sending raw data suites our need. Certain devices require sending a signal multiple times. This might require the `sendSignal` function to behave differently based on the type of IR signal sent. In addition, repeat functions may vary on a device-by-device basis. Idk, might need to check that.

### Firebase Functions
Currently planned functions are:
- set hub name
- send recorded signal
- send error message

#### Set Hub Name
This function is meant to be called automatically when first being set up by a user. There's a strong possibility this feature will be removed from the hub's functionality and just be a user feature.

#### Send Recorded Signal
Sets the "result" object in FirebaseDB, sending the raw data for the IR signal. If there is a problem sending the signal, an error message is sent with an unknown error code. The sent JSON object structure is as follows:

    {
     "code": <RES_SEND_SIG>, 
     "timestamp": "<timestamp value>", 
     "rawData": "<{4999, 9993, 4999, 4333, ... }>",
     "rawLen": "<size of rawData array>"
     }

#### Send Error
Several error messages can be set based on issues sending responses, failing to get IR input, etc. This is done by setting the "result" object in FirebaseDB. The "code" value will be one of several error codes. The sent JSON object structure is as follows:

    {
     "code": <error_code>,
     "timestamp": <timestamp value> 
    }


The following is a WIP flow diagram of the app experience:
<img src="https://i.imgur.com/gbUhyBY.png"
     style="float: center; margin-right: 10px;"
/>

## Physical Device
The physical device is essentially a Wifi connected IR receiver / blaster. The hardware consists of a simple IR LED setup, ESP8266 board, and IR receiver. Utilizes a Firebase backend for a "serverless" implementation. 

### Feature List
- Dynamically learn SSID/Password via SoftAP setup from any wifi-connected device 
- Send programmable IR signals via Android App **(WIP)**
- Learn IR signals and send to Android App **(WIP)**
- Handle sending repeat signals (simulating "holding down" a remote button) **(WIP)**

### IRFunctions
The currently planned functions are:
- record IR signal
- send single IR signal
- send repeat signal

#### Record IR Signal
Listens for complete IR signals recorded from `IRrecv` ([see IRremoteESP8266](https://github.com/markszabo/IRremoteESP8266/blob/master/src/IRrecv.h)). This function ignores any recognized "repeat" commands. If no signal is recorded after `IR_READ_TIMEOUT` seconds ([see ArduinoIRFunctions](https://github.com/MattS8/IRSmartHub/blob/master/arduino/IRSmartHub-arduino/ArduinoIRFunctions.h)), a timeout error is sent. If the signal is too large, an overflow error is sent. Assuming none of the previous conditions are met, the recorded raw data is sent ([see ArduinoFirebaseFunctions](https://github.com/MattS8/IRSmartHub/blob/master/arduino/IRSmartHub-arduino/ArduinoFirebaseFunctions.h))

#### Send IR Signal
Current implementation sends straight raw data at a fixed frequency. Most likely, future implementations will have protocol-specific behavior. That all depends with how well sending raw data suites our need. Certain devices require sending a signal multiple times. This might require the `sendSignal` function to behave differently based on the type of IR signal sent. In addition, repeat functions may vary on a device-by-device basis. Idk, might need to check that.

### Firebase Functions
Currently planned functions are:
- set hub name
- send recorded signal
- send error message

#### Set Hub Name
This function is meant to be called automatically when first being set up by a user. There's a strong possibility this feature will be removed from the hub's functionality and just be a user feature.

#### Send Recorded Signal
Sets the "result" object in FirebaseDB, sending the raw data for the IR signal. If there is a problem sending the signal, an error message is sent with an unknown error code. The sent JSON object structure is as follows:

    {
     "code": <RES_SEND_SIG>, 
     "timestamp": "<timestamp value>", 
     "rawData": "<{4999, 9993, 4999, 4333, ... }>",
     "rawLen": "<size of rawData array>"
     }

#### Send Error
Several error messages can be set based on issues sending responses, failing to get IR input, etc. This is done by setting the "result" object in FirebaseDB. The "code" value will be one of several error codes. The sent JSON object structure is as follows:

    {
     "code": <error_code>,
     "timestamp": <timestamp value> 
    }
