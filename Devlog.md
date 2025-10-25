Hello! Most changes will be summarised here, and I'll use this file to keep track of how I'm getting on with the project.
Note that this file is different to Snippets.ipynb, in that this is for release notes, and Snippets is for notes to myself and small sections of code.
TL;DR: Snippets is more for me to talk to myself, this file is more for me to talk to you.

# Development Log
**24/10/25**
- Added ability to create and delete people on a register
- Added ability to mark people as present/absent
- Added ability to delete people from register
- Added Status label
- Added keybinds (ESC for quit, DEL to delete a person)

**25/10/25**
- Added devlog to Git (whoops)
- Added saving and loading to/from files
  - File format is .rsave, which contains a JSONArray of people, formatted into a string, and encoded in Base64
- Added ability to mark people as present/late/absent
- Added ability to edit a person after they've been created
- Added more keybinds (CTRL + O to open file, CTRL + N to create a person, CTRL + S to save to file)
- Changed Status label to use an Enum and HashMap from a raw string
- Changed person presence to use an Enum and HashMap from a boolean