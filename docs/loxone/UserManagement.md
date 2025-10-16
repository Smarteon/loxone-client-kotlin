# User Management

> **Note:** This document is automatically generated from the official Loxone PDF documentation.
> Last updated: 2025-10-16 11:06:00 UTC
> Source: https://www.loxone.com/wp-content/uploads/datasheets/UserManagement.pdf

---

# **Usermanagement**

Date 2025.06.03


## Table of contents

Commands .............................................................................................................................................................................. 4


Get all available users .................................................................................................................................................. 4


Get all details for a user .............................................................................................................................................. 4


Get all configurable user-groups ............................................................................................................................. 6


Create or Edit existing user........................................................................................................................................ 7


Create User ........................................................................................................................................................................ 8


Delete User ........................................................................................................................................................................ 9


Assign user to group ..................................................................................................................................................... 9


Remove user from group ............................................................................................................................................. 9


Update user password .................................................................................................................................................. 9


Update user Visu-password........................................................................................................................................ 9


Update user keycodes ................................................................................................................................................... 9


Assign NFC tag to user ............................................................................................................................................... 10


Remove NFC tag from user ...................................................................................................................................... 10


Get all permissions for a control ........................................................................................................................... 10


Get Names of Custom User Fields......................................................................................................................... 10


Get List of existing options for user-fields ....................................................................................................... 11


Get List of existing options for fields .................................................................................................................. 11


Required Rights .................................................................................................................................................................. 13


Detailed info on attributes ............................................................................................................................................ 14


userstate ........................................................................................................................................................................... 14


type ..................................................................................................................................................................................... 14


userrights ......................................................................................................................................................................... 14


validUntil .......................................................................................................................................................................... 14


validFrom ......................................................................................................................................................................... 14


expirationAction............................................................................................................................................................ 15


changePassword ........................................................................................................................................................... 15


**16.0**

Usermanagement                                                Page 2 of 22


name ................................................................................................................................................................................... 15


userid ................................................................................................................................................................................. 15


uuid ..................................................................................................................................................................................... 15


isAdmin ............................................................................................................................................................................. 15


masterAdmin (deprecated) ....................................................................................................................................... 15


usergroups ....................................................................................................................................................................... 15


nfcTags .............................................................................................................................................................................. 16


keycodes ........................................................................................................................................................................... 16


scorePWD ......................................................................................................................................................................... 16


scoreVisuPWD ................................................................................................................................................................ 16


hashing .............................................................................................................................................................................. 16


trustMember ................................................................................................................................................................... 17


disabledBySource ......................................................................................................................................................... 17


Additional User-Fields ................................................................................................................................................ 17


Trust ........................................................................................................................................................................................ 18


Available Peers .............................................................................................................................................................. 18


Discovery .......................................................................................................................................................................... 18


Create user....................................................................................................................................................................... 20


Remove user ................................................................................................................................................................... 20


Edit users .......................................................................................................................................................................... 21


Revision History ................................................................................................................................................................. 22


V13.0 - 2022.07.20 - Improved hashing doc .................................................................................................... 22


V12.1 - Trust ................................................................................................................................................................... 22


**16.0**

Usermanagement                                                Page 3 of 22


## Commands


  - In front of each command there is written “/jdev/sps/”.

  - It describes the path to the available commands.

  - Commands need to be performed by a user with a right to modify other users.

### **Get all available users**


**getuserlist2**


  - gets a list of all configured users


**Response**


  - expirationAction


      - ActionDeactivate = 0

      - ActionDelete = 1

### **Get all details for a user**


**getuser/{uuidUser}**


This webservice returns a json with the full user-configuration.


**16.0**

Usermanagement                                                Page 4 of 22


**Response**


**16.0**

Usermanagement                                                Page 5 of 22


```
         {

             "code":"6C3A7D85A4B196E37C5DADFDE7E0586ED4D7137C"

         }

     ],

     "customFields":[

        "Custom Field 1",

        "Custom Field 2",

        "Custom Field 3",

        "Custom Field 4",

        "Custom Field 5"

     ]

 }

### **Get all configurable user-groups**

```

**getgrouplist**


Lists all available user-groups and additional information


**Response**


**16.0**

Usermanagement                                                Page 6 of 22


### **Create or Edit existing user**

**addoredituser/{json}**


  - {json}:


      - User configuration with all settings to be created / changed

      - see getuser-cmd for details on JSON structure

  - json-content:


      - uuid: [optional]


         - if null, a new user will be created.

         - if provided and uuid is found, an existing user is adapted.


         - if not found, it will return with code 500, user not found.

     - usergroups

         - When during editing a user and no groups-array is set, the group
assignment will remain unchanged.

      - All other json attributes are optional.

  - Return-Value:


     - UUID of created or adapted user

      - errortext if failed


**Example - Add a new user**


Request
```
jdev/sps/addoredituser/{"name":"A","userid":"1234","changePassword":true,"userState

":4,"validUntil":371738510,"validFrom":371736410,"expirationAction":{ACTION}}

```

Please note, that here no UUID is specified - that is why a new user is created.


Response


**16.0**

Usermanagement                                                Page 7 of 22


```
     "validUntil": 371738510,

     "validFrom": 371736410,

     "usergroups": [],

     "nfcTags": [],

     "keycodes": []

 }

```

Example - Existing User → groups and NFC-tags are adapted


Example – Existing User → custom user fields are adapted

### **Create User**


**createuser/{username}**


  - creates an user with a given username

  - result will contain the uuid of the new user


**16.0**

Usermanagement                                                Page 8 of 22


### **Delete User**

**deleteuser/{uuidUser}**


  - deletes an user with a given uuid

  - All active websockets used by this user will be closed.

  - When trying to delete the last admin, the Miniserver will respond with 403

### **Assign user to group**


**assignusertogroup/{uuidUser}/{uuidGroup}**


  - adds an existing user to a usergroup

  - more information: Get all configurable user-groups

### **Remove user from group**


**removeuserfromgroup/{uuidUser}/{uuidGroup}**


  - removes an user from a usergroup

### **Update user password**


**updateuserpwdh/{uuidUser}/{value}**


  - adapts a password for an existing user

  - {value}: hashed password-value

  - optional: append password-scoring for strength of new password


      - {value} = {hash}|{score}

  - return Value 504 if user is from a trust member and member cant be reached

### **Update user Visu-password**


**updateuservisupwdh/{uuidUser}/{value}**


  - adapts a visu-password for an existing user

  - {value}: hashed visu-password

  - optional: append password-scoring for strength of new password


      - {value} = {hash}|{score}

  - return Value 504 if user is from a trust member and member cant be reached

### **Update user keycodes**


**updateuseraccesscode/{uuidUser}/{newAccessCode}**


Sets a new keycode, updates, or removes an existing keycode for a user.


**16.0**

Usermanagement                                                Page 9 of 22


  - to remove a keycode, simply pass in an empty string or invalid code


  - {newAccessCode} → numeric code (0-9) with 2-8 digits, will be hashed once stored


on the Miniserver, do not hash on client. This is required to ensure no duplicate


codes are in use.


Return values:


  - 200 → code unique, successfully changed or deleted


  - 201 → code not unique and successfully changed


  - 400 → error, {uuidUser} not found or not a user


  - 403 → error, logon user has no User management right


  - 409 → error, code already in use of NFC Authentication block

  - 429 → error, 5 minutes lock after 5 requests from logon user without sufficient rights

### **Assign NFC tag to user**


For assigning an NFC tag to a user, the tag must be read first using an NFC Code touch (see

“Structure File” documentation, section “NFC Code Touch”). Once the NFC Tag ID is known it

can be paired with the user.


**addusernfc/{uuidUser}/{nfcTagId}/{name}**

### **Remove NFC tag from user**


The linking of an NFC tag and the user can be removed at any time.


**removeusernfc/{uuidUser}/{nfcTagId}**

### **Get all permissions for a control**


This webservice returns a List of all Users and Groups which are directly assigned to a control

with a given uuid


**getcontrolpermissions/{uuid}**

### **Get Names of Custom User Fields**


Since Version 14.3


Returns a list of customizable user-fields 1-5


**16.0**

Usermanagement                                                Page 10 of 22


Request
```
    jdev/sps/getcustomuserfields

```

Response

```
 {

     "customField1": "Building",

     "customField2": "Parking Space",

     "customField3": "ICQ Number",

     "customField4": "Custom Field 4",

     "customField5": "Custom Field 5"

 }

### **Get List of existing options for user-fields**

```

Since Version 14.3


Returns an object that contains already configured options for various user-specific fields. The

key is the same as in the **getuser** response, the value is an array of values that have already

been assigned to this properties.


Request

jdev/sps/getuserpropertyoptions


Response

```
 {

     "company": ["Loxone Smart Engineering","Loxone Electronics"],

     "department": ["Software Development","Marketing","Product Management"]

 }

### **Get List of existing options for fields**

```

Since Version 14.3


Returns the user with a given userID.


If user is not found, values are empty


Request

jdev/sps/checkuserid/[userid]


Response

```
 {

     "name": "admin",

     "uuid": "19f4f3b3-038a-4172-ffffb91f6db8271b"

 }

```

**16.0**

Usermanagement                                                Page 11 of 22


**16.0**

Usermanagement                                                Page 12 of 22


## Required Rights

There are 4 levels of rights which allow different levels of editing.

These levels are best described via User-Roles:


  - Guest


      - is just a normal user

      - is not allowed to edit anything


  - User


      - has enabled “Change own password” -checkbox

      - may edit own password or visu-password


  - Usermanager

     - is Member of a group with group-right “Usermanagement”

     - “Change own password” is automatically enabled and may not be changed

      - may fully edit own user

     - may fully edit non-Admin-users

      - is not allowed to view or edit admin-users

      - is not allowed to add **any** user to an admin-group


  - Administrator


      - is Member of group “All Access” or a group with group-right “Loxone Config”

      - may fully edit own user

      - may fully edit all other users

      - is allowed to add or remove any user to/from an admin-group











|Col1|1<br>Administrator|2<br>Usermanager|3<br>User|4<br>Guest|
|---|---|---|---|---|
|change own password|X|X|X||
|change own access-code|X|X|X||
|edit own nfc tag|X|X|||
|change Code / Tag / Pass of an administrator|X||||
|change Code / Tag / Pass of non-admin|X|X|||
|add or remove user from/to admin-groups|X||||
|add or remove user from/to common groups|X|X|||


**16.0**

Usermanagement                                                Page 13 of 22


## Detailed info on attributes

### **userstate**

Indicates whether or not a user is active and may log in or get access (depending on the rights

granted in config permission management).


  - 0 = enabled, without time limitations


  - 1 = disabled

  - 2 = enabled until, disabled after that point in time

  - 3 = enabled from, disabled before that point in time

  - 4 = timespan, only enabled in between those points in time

### **type**


  - 0 = Normal

  - 1 = Admin (deprecated)


  - 2 = All

  - 3 = None


  - 4 = AllAccess → New “Admin Group”

### **userrights**


  - 0x00000000 = None

  - 0x00000001 = Web

  - 0x00000004 = Loxone config

  - 0x00000008 = FTP

  - 0x00000010 = Telnet

  - 0x00000020 = Operatingmodes

  - 0x00000040 = Autopilot

  - 0x00000080 = Expert mode Light

  - 0x00000100 = Usermanagement

  - 0xFFFFFF = All rights (admin)

### **validUntil**


  - Only available/required if with userstate 2 and 4

  - provided as seconds since 1.1.2009 00:00:00

### **validFrom**


  - Only available/required if with userstate 3 and 4


**16.0**

Usermanagement                                                Page 14 of 22


  - provided as seconds since 1.1.2009 00:00:00

### **expirationAction**


  - Only available/required if with userstate 2 and 4

  - Since V14.2.5.16

  - Possible values


      - 0 = Deactivate

      - 1 = Delete

### **changePassword**


Specifies whether or not a user is allowed to change its passwords from within the apps

### **name**


When it comes to users, this is the username that is used to login via our app.

### **userid**


May be empty, this is the id that will be returned by the NFC permission block when granting

access. In Loxone Config, this field is configured as NFC Code Touch ID

### **uuid**


Generated by the Miniserver, unique identifier.

### **isAdmin**


  - This flag is set if the user has administrative rights on the Miniserver.

  - There must be at least one user who has isAdmin set, in order to still have config


access.

  - Any modification that would violate this rule will result in error 403 “delete of last


admin not allowed”

### **masterAdmin (deprecated)**


In config versions prior to 11.0, there used to be one main admin, which could not be removed.

### **usergroups**


**getuser/{uuidUser}**


An array containing an object for each group the user is part of. Each group object contains both

the name and the UUID of the group.


**16.0**

Usermanagement                                                Page 15 of 22


**getgrouplist**


With this request, additional infos on the group are available, such as the userrights and the

type.

### **nfcTags**


  - An array with an entry for each NFC tag associated with this user.

  - Each tag is represented by a name and the NFC tag id

### **keycodes**


  - Even though this is an array, currently there is only one keycode for each user.

  - The only attribute of each keycode object is the code itself.

  - The code is a hashed representation of the keycode stored.

### **scorePWD**


Provides/sets info on how strong a password is.


  - -2 = not specified

  - -1 = empty password


  - 0 = low strength

  - 1 = good,

  - 2 = very good,

  - 3 = most powerful

### **scoreVisuPWD**


Same like scorePWD but for visualization passwords. (additional password that has to be

entered, even tough the connection itself is already authenticated - e.g. for disarming a burglar

alarm).

### **hashing**


Passwords are never transmitted, not even encrypted - they are hashed on the client and only

the hash is being transmitted to the Miniserver.


**Creating a hash for a password**


  - Use “jdev/sys/getkey2/{username}” to retrieve the {salt} and {hashAlg}


      - salt is user-specific, long-lived

      - hashAlg specifies which hashing algorithm to use (recent versions use SHA256)

      - A “key” property is also provided, but not used here, it is a temporarily valid key


to be used for hashing when authenticating.


**16.0**

Usermanagement                                                Page 16 of 22


  - Use the {hashAlg} provided and the long lived {salt} to create a **uppercase** {passHash}


for the new {password}


      - e.g.: SHA256({password} + “:” + {salt}).toUpperCase() → {passHash}


**Creating a hash for a visu password**


Same as creating a hash for a password, but “dev/sys/getvisusalt/{username}” is to be used
instead of “jdev/sys/getkey2/{username}”

### **trustMember**


If a user originates from a Trust Member/Peer this attribute is added containing the serial of the

source Miniserver

New with Version 12.1

### **disabledBySource**


Boolean. When set user was disabled by Trust source. Since V15.3

### **Additional User-Fields**


New with Version 14.3


   uniqueUserId


       - Do not confuse this one with “userid” - which is the “NFC Code Touch ID”

       uniqueUserIdis called “User ID” in Config and must be unique!

   - firstName

   - lastName

   - email

   phone


   - company

   department


   personalno

   - title

   - debitor

   - customField1 - customField5


       Individually named custom fields

       for generic usage via API, these fields are not addressed with a custom name

       - for Title of custom fields, see Get Names of Custom User Fields


**16.0**

Usermanagement                                                Page 17 of 22


## Trust

### **Available Peers**

**jdev/sps/trustusermanagement/peers**

Response: Native answer json


































### **Discovery**

**jdev/sps/trustusermanagement/discover/{peerSerial}**


  - {peerSerial}:


      - Serial number of the peer you want to search on


Return-Value:


  - UUID of created or adapted user

  - errortext if failed


Request
```
    jdev/sps/trustusermanagement/discover/504F94A00210

```

**16.0**

Usermanagement                                                Page 18 of 22


Response











































































**16.0**

Usermanagement                                                Page 19 of 22


### **Create user**

**jdev/sps/trustusermanagement/add/{peerSerial}/{userUuid}**


  - {peerSerial}


      - Serial number of the peer from which you want to add a user


  - {userUuid}


      - Uuid of the user as specified in the discovery answer


Return-Value:


  - errortext if failed


Request
```
    jdev/sps/trustusermanagement/add/504F94A00210/15f45069-01db-3b8a
    ffff504f94a00210

```

Response





Response - Failed








### **Remove user**

**jdev/sps/trustusermanagement/remove/{peerSerial}/{userUuid}**


  - {peerSerial}


      - Serial number of the peer from which you want to remove a user

  - {userUuid}


      - Uuid of the user as specified in the discovery answer


Return-Value:


  - errortext if failed


Request
```
    jdev/sps/trustusermanagement/remove/504F94A00210/15f45069-01db-3b8a
    ffff504f94a00210

```

**16.0**

Usermanagement                                                Page 20 of 22


Response





Response - Failed








### **Edit users**

**jdev/sps/trustusermanagement/edit/{json}**


  - {json}


      - Json containing list of users that need to be removed/added

      - Array of objects for each peer, array contains an object for each user with the


attribute ‘used’ and the uuid

  - Request may be a Post request! json is in post data.


Return-Value:


  - errortext if failed


Request
```
    jdev/sps/trustusermanagement/edit/{“peerSerial”:[{“uuid”:”...”,

    “used”:true}]}

```

Response





Response - Failed









**16.0**

Usermanagement                                                Page 21 of 22


## Revision History

### **V14.3 - W.i.p**


  - Additional User-Fields

  - Webservice GetCustomUserFields

### **V14.2 - 2023.06.16**


  - User expiration action added (validUntil)

### **V13.0 - 2022.07.20 - Improved hashing doc**


  - documentation on hashing was confusing, clarified.

### **V12.1 - Trust**


  - Add / Remove Trust users


  - Login with Host (Miniserver Name or Serialnumber)


**16.0**

Usermanagement                                                Page 22 of 22


