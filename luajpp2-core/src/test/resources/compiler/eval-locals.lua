
local x = 1

function f()
    local y = x + 1
    yield()
end

f()
