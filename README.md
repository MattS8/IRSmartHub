
# IRSmartHub
A project with the goal of creating a solution to the many IR-controlled devices in our home. This project makes those devices "smart", allowing them to be controlled from any android phone (soon to be any device with a browser).
## Data Structures
The following describe how data is stored and handled in this project:
### User
Contains all information with respect to a specific user using this service. 
 - **Unique id**: The uid of the associated user account 
 - **Username**: *used for allowing other users to invite/connect without sharing email*
 - **Groups**: *a list of groups the user is associated with*
### Remote Profile
Contains a collection of pre-programmed actions
 - **Buttons**: a map of actions to remote functions. Remote functions can be static, pre-defined (i.e. VOL UP) or custom
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
	- ***Add Remote Profiles***: Allows user to add remote profiles to the group. User can remove added profiles at any time.
	- ***Remove Remote Profiles***: Allows user to remove any remote profile shared with the group.
