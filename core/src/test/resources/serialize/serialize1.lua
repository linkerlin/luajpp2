
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
    t[key] = value
    return t
end

weakRef1 = "a"
weakRef2 = "b"
weakRef3 = "c"
weakRef4 = "d"
weakRef5 = "e"
weakRef6 = "f"
weakKeys1 = weakTable("k", weakRef1, weakRef2)
weakValues1 = weakTable("v", weakRef3, weakRef4)
weakDouble1 = weakTable("kv", weakRef5, weakRef6)

-- Multiple references to the same variable
complexTable1 = {
    sub1 = {
        table3
    }
}
