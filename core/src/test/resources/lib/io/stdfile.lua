
-- Write to stdout/stderr
io.stdout:write("stdout")

-- Read a line from stdin and write it back to stdout
local input = io.stdin:read()
io.stderr:write("input=" .. input)
