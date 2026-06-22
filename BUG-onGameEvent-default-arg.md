# Bug: Lua transpiler drops default-valued args skipped at call site

**Component:** ktox Kotlin→Lua transpiler
**Severity:** High — produces silently broken runtime code

## Summary

When a call omits a parameter that has a default value (e.g. an optional
parameter before a trailing lambda), the transpiler emits the call
positionally **without** substituting the default. Following arguments shift
into the wrong slots and the last parameter becomes `nil`.

## Runtime error

```
Script Runtime Error: com.isycat.ktox.dota.lib(OnGameEvent.kt:3):
attempt to call upvalue 'listener' (a nil value)
stack traceback:
	[C]: in function 'listener'
	com.isycat.ktox.dota.lib(OnGameEvent.kt:3): in function
	<scripts\vscripts\com\isycat\ktox\dota\lib\OnGameEvent.lua:11>
```

## Reproduction

Library function with an optional middle parameter (`OnGameEvent.kt`):

```kotlin
inline fun <reified T : GameEvent> onGameEvent(
    eventName: EventKey<T>,
    context: Any? = null,                 // optional, in the middle
    crossinline listener: (T) -> Unit,    // trailing lambda
) = listenToGameEvent(eventName as Any as String, { listener(it as T) }, context)
```

Call site that omits `context` (`Main.kt:42`):

```kotlin
onGameEvent(PLAYER_CHAT) { event -> /* ... */ }
```

## Actual transpiled Lua (broken)

```lua
function onGameEvent(eventName, context, listener)   -- 3 params
    if context == nil then context = nil end         -- dead line
    return ListenToGameEvent(eventName, function(it)
        return listener(it)                          -- listener is nil
    end, context)
end

-- call site (Main.lua:40): only 2 args passed
onGameEvent("player_chat", function(event) ... end)
-- => eventName="player_chat", context=<lambda>, listener=nil
```

The lambda lands in `context`; `listener` is never bound, so it errors when
the event fires.

## Expected transpiled Lua

The omitted default must be emitted explicitly (Lua has no default params or
named args):

```lua
onGameEvent("player_chat", nil, function(event) ... end)
```

## Workaround

Pass the optional argument explicitly at the call site:

```kotlin
onGameEvent(PLAYER_CHAT, null) { event -> /* ... */ }
```

## Suggested fix

In the call-site codegen, fill omitted parameters that have defaults with
their default value (and emit `nil` / the literal as appropriate) before
emitting positional arguments.
