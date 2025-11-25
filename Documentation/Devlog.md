Hello! Most changes will be summarised here, and I'll use this file to keep track of how I'm getting on with the project.

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
- Added additional keyboard shortcuts (ESC for quit, DEL to delete a person)
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
- Added more keyboard shortcuts (CTRL + O to open file, CTRL + N to create a person, CTRL + S to save to file)

**Changes**
- Changed Status label to use an Enum and HashMap from a raw string
- Changed person presence to use an Enum and HashMap from a boolean
- Refactored JSONObject[] creation in SaveRegister() to use the JSONHandler
- Changed the X button on the JFrame to trigger the Save prompt
- Changed the keyboard shortcut input detection to use an Enum and a List, so I don't make a typo and break everything

**Removals**
- Removed unused Person.kt and Person.java data classes
- Removed Resources directory and template.json

**Issues**
- Files outside the "./saves" directory cannot be selected (Issue 1)
    - New branch (IssueOne) created from the master branch to attempt fixes.
    - [Link to GitHub issue](https://github.com/ThatAmeliaH/SmartCardRegister/issues/1)

## 26/10/25
**Additions**
- Added a start time input, allows the user to input the start time for automatic lateness detection
- Added a top bar with drop down buttons
    - File (for file management options)
    - Edit (for manual overrides and start time management)
    - View (for appearance options)
    - Student (for student management options)
- Added keyboard shortcuts
    - **DELETE**: Delete User (with confirmation)
    - **Ctrl + Delete**: Delete User (skips confirmation message)
    - **Alt + 1/2/3**: Sets the selected user to present/late/absent
    - **Ctrl + Enter**: Toggles a person as present/absent or late/absent depending on the start time
    - **Ctrl + E**: Edit selected user
    - **Ctrl + I**: Set start time
- Added Documentation folder
- Added "KeyboardShortcuts.md"

**Changes**
- New UI layout, moved a lot of buttons into the new top bar
- Changed DeleteUser() to take in a boolean input - "overrideWarning" - that skips the popup asking the user to confirm deleting the selected user
- Changed the click detection and handling for the delete button
    - New system allows for CTRL Clicking, this passes true into overrideWarning making it function the same as CTRL + DELETE
- Replaced all instances of the word "person" with "student"
- Moved devlog.md to "Documentation" folder
- Main development halted on Windows 11, software will now be developed primarily on [Linux Mint](https://linuxmint.com/)
    - Most tests will be performed on this operating system, and the UI will be designed around how it appears on Mint
    - Some tests and UI tweaks will be performed on a Windows 11 machine, both in college and through my stakeholders

**Removals**
- Removed unused Dataclasses folder

**Fixes**
- Issue 1 resolved on IssueOne test branch
    - Pull request created - test branch merged onto master

**Issues**
- No limit to student names (Issue 6)
    - [Link to GitHub issue](https://github.com/ThatAmeliaH/SmartCardRegister/issues/6)
- No duplicate checking when updating a student (Issue 7)
    - [Link to GitHub issue](https://github.com/ThatAmeliaH/SmartCardRegister/issues/7)
- Incorrect forename/surname splitting (Issue 8)
    - [Link to GitHub issue](https://github.com/ThatAmeliaH/SmartCardRegister/issues/8)
- Emojis not showing up in header buttons on Linux machines (Issue 10)
    - [Link to GitHub issue](https://github.com/ThatAmeliaH/SmartCardRegister/issues/10)

## 27/10/25
**Changes**
- Removed emojis from header buttons - see Issue 10

**Fixes**
- Issues 6, 7 and 8 resolved on test branch
    - Pull request created - test branch merged onto master
- Issue 10 workaround deployed to main branch (see "**Changes**")

## 05/11/25
**Changes**
- Rewrote Kotlin handler classes in Scala
    - This allows me to work on my project in college, as the college computers do not have a Kotlin compiler installed
    - Handlers are set up as singleton objects, akin to static Java classes or Kotlin object instances.

## 06/11/25
**Changes**
- Changed handler functions to be more null safe
    - Functions no longer return null, instead blank strings/arrays

## 07/11/25
**Additions**
- Added a basic loading screen, will likely show loading progress in the future

**Changes**
- Changed shortcuts list to instead use an array

## 17/11/25
**Additions**
- Added Pseudocode.md to Documentation
- Added timing to the initial startup, program now outputs the time taken for the register UI to load.

**Changes**
- Change Gradle wrapper to Groovy, replacing Kotlin
  - This allows for the program to compile properly on my college computer, where Kotlin is not configured
- Renamed constant variables to use all capital letters

**Removals**
- Removed "Snippets.ipynb"

## 21/11/25
**Additions**
- Added "NFCHandler.scala"
- Added "NFCTesting.java"
  - NFCTesting provides a second entry point to the program, for testing NFC reader integration and functionality

## 25/11/25
**Additions**
- Added Terminal Tester Utility for testing NFC Readers
  - Uses keybind Ctrl + Alt + T

**Changes**
- Changed status label and drop down button setup to use one common function
  - This removes a lot of repetition and makes formatting these easier