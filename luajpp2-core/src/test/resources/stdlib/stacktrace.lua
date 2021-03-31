
-- Nested function calls

function x()
    y()
end

function y()
    z()
end

function z()
    yield()
end

x()

-- Nested tailcalls

function tailx()
    return taily()
end

function taily()
    return tailz()
end

function tailz()
    yield()
end

tailx()