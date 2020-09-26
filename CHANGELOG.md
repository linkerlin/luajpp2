
# v3.2.0

- eval now has (read-only) access to local variables in the top-most frame of the call stack
- Better stack traces (available through `DebugTrace.stackTrace()`)
- Threads now have a changeable name
- Turned a few array parameters into varargs where possibly (notably `LuaValue.varargsof()`)
- fix: The 'call' debug hook was wrongly called with `__call` instead of `call`.
- performance: stack/upValue arrays are sized smaller and reused to reduce garbage collection pressure.
