
properties = {}
for n=1,#instance do
    local val = instance[n]

    properties[n] = {
        original = val,
        hasCAS = val.compareAndSet ~= nil
    }
end
