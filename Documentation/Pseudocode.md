# Project Pseudocode

## Add menu actions:
```
function AddMenuActions(items, actions)
    max = (length(items) < length(actions)) and length(items) or length(actions)

    if length(items) ~= length(actions) then
        error("Size mismatch: List of size " + len(items) + " bound to runnable array of length " + len(actions))
    end if

    for i = 1 to max do
        items[i].addActionListener(actions[i].run())
    next i
end function
```