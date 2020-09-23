
# v3.2.0

- Better stack traces (available through `DebugTrace.stackTrace()`)
- Turned a few array parameters into varargs where possibly (notably `LuaValue.varargsof()`)
- fix: The 'call' debug hook was wrongly called with `__call` instead of `call`.
- performance: stack/upValue arrays are sized smaller and reused to reduce garbage collection pressure.
