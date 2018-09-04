
-- Simple values
nil1 = nil
bool1 = true
bool2 = false
int1 = 1
int2 = 1000
double1 = 0
double2 = 12.34
string1 = ""
string2 = "abc"
string3 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" -- 50x

-- Tables
table1 = {}
table2 = {11, 12, 13}
table3 = {a = 11, b = 12, c = 13}

-- Weak tables
function weakTable(mode, key, value)
    local t = {}
    setmetatable(t, {__mode = mode})
    t[key] = value
    return t
end

weakRef0 = {"a"}
weakRef1 = {"b"}
weakRef2 = {"c"}
weakRef3 = {"d"}
weakRef4 = {"e"}
weakRef5 = {"f"}
weakKeys1 = weakTable("k", weakRef0, weakRef1)
weakValues1 = weakTable("v", weakRef2, weakRef3)
weakDouble1 = weakTable("kv", weakRef4, weakRef5)

-- Multiple references to the same variable
complexTable1 = {
    sub1 = {
        table3
    }
}
