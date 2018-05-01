-- test for dump/undump with upvalues
local a, b = 20, 30

x = loadstring(string.dump(function (x)
  if x == "set" then a = 10+b; b = b+1 else
  return a, b
  end
end))
assert(x() == nil)
assert(debug.setupvalue(x, 1, "hi") == "a")
assert(x() == "hi")
assert(debug.setupvalue(x, 2, 13) == "b")
assert(not debug.setupvalue(x, 3, 10))   -- only 2 upvalues
print(x())
x("set")
assert(x() == 23)
x("set")
assert(x() == 24)
