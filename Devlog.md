Hello! Most changes will be summarised here, and I'll use this file to keep track of how I'm getting on with the project.
Note that this file is different to Snippets.ipynb, in that this is for release notes, and Snippets is for notes to myself and small sections of code.
TL;DR: Snippets is more for me to talk to myself, this file is more for me to talk to you.

# Development Log
## 10/10/25
- Initial Commit
  - JSONHandler and Base64Handler imported from early testing projects

## 12/10/25
**Additions**
- Added very basic UI with Quit button

## 13/10/25
**Fixes**
- Fixed UI sizing

## 14/10/25
**Additions**
- Added Kotlin "Person" Dataclass
- Added template register to "./saves"
- Added FileHandler

**Changes**
- Changed "Utils" to "Handlers"
- Moved file functionality from JSONHandler to FileHandler
- Updated .gitignore to exclude "./saves"

## 15/10/25
**Additions**
- Added ProgramState Kotlin object
- Added Snippets.ipynp
- Added NFCHandler class

**Changes**
- Refactored handler classes to be Static

## 16/10/25
**Additions**
- Added fullscreen, toggle bound to F11

**Removals**
- Removed ProgramState object
  - This was intended to store your fullscreen preference between forms, but the project is now only one form, so this was unnecessary

## 17/10/25
**Changes**
- Temporarily rewrote Person.kt in Java as Person.java
  - College PCs have no access to Kotlin, so project was untestable
- Changed fullscreen mode to be borderless

## 24/10/25
**Additions**
- Added ability to create and delete people on a register
- Added ability to mark people as present/absent
- Added ability to delete people from register
- Added Status label
- Added additional keybinds (ESC for quit, DEL to delete a person)
- Added confirmation popups to most actions

**Fixes**
- Fixed a crash that occurred on windowing the main form

## 25/10/25
**Additions**
- Added devlog to Git (whoops)
- Added saving and loading to/from files
  - File format is .rsave, which contains a JSONArray of people, formatted into a string, and encoded in Base64
- Added ability to mark people as present/late/absent
- Added ability to edit a person after they've been created
- Added more keybinds (CTRL + O to open file, CTRL + N to create a person, CTRL + S to save to file)

**Changes**
- Changed Status label to use an Enum and HashMap from a raw string
- Changed person presence to use an Enum and HashMap from a boolean
- Refactored JSONObject[] creation in SaveRegister() to use the JSONHandler
- Changed the X button on the JFrame to trigger the Save prompt
- Changed the keybind input detection to use an Enum and a List, so I don't make a typo and break everything

**Removals**
- Removed unused Person.kt and Person.java data classes
- Removed Resources directory and template.json

**Issues**
- Files outside the "./saves" directory cannot be selected
  - New branch (IssueOne) created from the master branch to attempt fixes.
  - [Link to GitHub issue](https://github.com/ThatAmeliaH/SmartCardRegister/issues/1)

## 26/10/25
**Additions**
- Added keybinds for presences and deleting people
  - DELETE: Delete User (with confirmation)
  - CTRL + DELETE: Delete User (skips confirmation message)
  - ALT + 1/2/3: Sets the selected user to present/late/absent

**Changes**
- Changed DeleteUser() to take in a boolean input - "overrideWarning" - that skips the popup asking the user to confirm deleting the selected user
- Changed the click detection and handling for the delete button
  - New system allows for CTRL Clicking, this passes true into overrideWarning making it function the same as CTRL + DELETE