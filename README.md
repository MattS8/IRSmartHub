

# IRSmartHub
A project with the goal of creating a solution to the many IR-controlled devices in our home. This project makes those devices "smart", allowing them to be controlled from any android phone (soon to be any device with a browser). This is achieved by creating a device containing an IR receiver/transmitter and ESP8266 wifi board that speaks to a Firebase realtime database backend. In addition, a Firestore database is used to contain all user/device data. This allows for a "serverless" implementation with all commands running through Firebase.

## Firestore Data Structures (Android/User-Facing Interfaces):
The following are data structures used in Firebase Firestore. This is the part of the back-end holds all user content:


### User
Contains All information with respect to a specific user using this service. 
 - **uid**: The uid associated with this user. ***NOTE***: Users are keyed by *username*. This field is necessary to link user accounts to their respective username.
 - **groups**: A list of groups the user is associated with. ***NOTE***: Upon creating a new user, a *personal group* is automatically added. This group is the default location for any created Commands.
 - **Group Invitations**: A list of invitations to groups the user can join. Any user can add their invitation to this list.
 
 ### Hub
 Information about a hub that has been set up and is ready to use. Hubs are keyed by their MAC address (which is conisderd their uid for this project).
 - **name**: Name given by user who initially set up device. This is used to help users distinguish between different hubs
 - **owner**: The uid of the user who initially set up this device.
 - **ownerUsername**: The username of the user who initially set up this device.
 
### Remote Profile
Contains a collection of pre-programmed actions.
- **name**: A custom name given by the creator of the remote profile.
- **hub**: The uid of the hub associated with this remote.
- **buttons**: A list of remote functions (either *actions* or *commands*). Remote functions can be static, pre-defined (i.e. VOL UP) or custom.
- **groups**: A list of groups this remote profile is associated with.

### IR Signal
Contains information for a custom-learned IR signal. This is the data object used in the Firestore database to keep track of user-learned IR signals.
- **rawData**: An array of numbers representing an IR signal.
- **rawLength**: The length of the raw data array.
- **name**: User-given name describing what the IR signal does.
- **encodingType**: The type of encoding for the learned signal (i.e. SAMSUNG, etc.).
- **code**: The hex code for the IR signal.
- **repeat**: Determines whether the signal is repeatable. (IMPLEMENTATION MAY CHANGE)

### Command
Contains one or more actions for an IRSmartHub to perform. A command can specify a specific IR Hub to target for each command.
- **Actions**: An ordered list of <IR Signal, Hub> pairs. 
- **Name**: User-given name describing what the command does.

### Group
Contains a collection of users, remote profiles, and associated hubs. This is used for sharing hubs between multiple users. Each group has a group owner and 0 or more users. Other users can be given specific permissions. 
- **remoteProfile**: A collection of profiles shared among the group.
- **connectedDevices**: A collection of hubs associated with the group.
- **owner**: The uid of the original creator of the group.
- **ownerUsername**: The username of the original creator of the group.
- **personalGroup**: A boolean value that determines whether other users can be added to group.
- **users**: A collection of users with set permissions. Permission types are as follows:
	- ***addDevices***: Allows user to add hubs to the group. Users can only add hubs that they set up.
	- ***removeDevices***: Allows user to remove hubs from the group.
	- ***addUsers***: Allows user to add other users to the group. Added users can only have the same (or less) permissions as the user who added them.
	- ***removeUsers***: Allows user to remove other users from the group.
	- ***addRemoteProfiles***: Allows user to add remote profiles to the group. User can remove profiles they have added at any time.
	- ***removeRemoteProfiles***: Allows user to remove any remote profile shared with the group.


## Realtime Database Data Structures (Arduino/IRSmartHub):
The following are data structures used in the Realtime Database. This part of the back-end instructs IRSmartHubs and transfers information from user to devices:


### Hub Action
Contains data for a specific action for the hub. This is what the arduino interprets to know what to do.
- **rawData**: An array of numbers representing an IR signal. This is only used for actions that require it (i.e. "send IR signal").
- **rawLen**: The length of the raw data array.
- **sender**: The uid of the user who initiated the action.
- **timestamp**: Date and time the action was sent.
- **repeat**: Whether or not the action is meant to be repeated. (IMPLEMENTATION MAY CHANGE)

### Hub Result
Contains data for a result object from an IRSmartHub. This can be either a learned signal or an error message.
- **resultCode**: The status of the result. Either an error code or result type indicator.
- **code**: The hex code for the learned IR signal.
- **timestamp**: Time (in milliseconds) the IRSmartHub has been on.
- **encoding**: The type of encoding for the learned signal (i.e. SAMSUNG, etc.).
- **rawData**: An array of uint16_t numbers representing the recorded signal.
- **rawLen**: The length of rawData.
- **repeat**: Determines whether the learned signal is a repeat. (IMPLEMENTATION MAY CHANGE)

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
     "resultCode": <RES_SEND_SIG>, 
     "timestamp": "<timestamp value>", 
     "rawData": "<{4999, 9993, 4999, 4333, ... }>",
     "rawLen": "<size of rawData array>",
     "encoding": "<encoding type>",
     "code": <Hex code for IR signal>,
     "repeat": <whether or not the signal can repeat>
     }

#### Send Error
Several error messages can be set based on issues sending responses, failing to get IR input, etc. This is done by setting the "result" object in FirebaseDB. The "code" value will be one of several error codes. The sent JSON object structure is as follows:

    {
     "resultCode": <error_code>,
     "timestamp": <timestamp value> 
    }
