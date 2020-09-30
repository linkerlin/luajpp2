
global = 2
local x = 1

function f()
    local y = x + global + 1
    yield()
end

f()
