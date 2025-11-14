--[[
    // Pseudocode will be written using syntax highlighting from the Lua programming language
    // Lua is very similar to pseudocode, so it can be used to make pseudocode look nicer
    // Nothing written here will be valid lua, the language is used solely for the colours
]]--

-- Add menu actions
function AddMenuActions(items, actions)
    max = (length(items) < length(actions)) and length(items) or length(actions)
    
    if length(items) ~= length(actions) then
        error("Size mismatch: List of size " + len(items) + " bound to runnable array of length " + len(actions))
    end
    
    for i = 1, max do items[i].addActionListener(actions[i].run()) end
end
