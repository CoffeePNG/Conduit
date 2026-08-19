# Conduit

A lightweight Velocity proxy plugin that lets server staff send players to — or retrieve players from — any server connected to the network.

---

## Commands

| Command | Description |
|---|---|
| `/ct send <player> <server>` | Push a player to any connected server |
| `/ct get <player>` | Pull a player to your current server |
| `/ct get` (no player) | List everyone online network-wide, click a name to pull them |
| `/cts <player> <server>` | Shorthand for `/ct send` |
| `/ctg <player>` | Shorthand for `/ct get` |
| `/ctg` (no player) | Shorthand for `/ct get` player list |
| `/conduit` | Alias for `/ct` |

### Examples

```
# You're on dev1 and want to send coff33__ to build1
/ct send coff33__ build1
/cts coff33__ build1        # same thing, shorter

# You're on dev1 and want to pull coff33__ from wherever they are
/ct get coff33__
/ctg coff33__               # same thing, shorter

# Not sure who's online or where? Just run it with no name for a clickable list
/ctg
```

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `conduit.send` | Use `/ct send` | op |
| `conduit.get` | Use `/ct get` | op |

Grant both at once with a permission group:
```yaml
# LuckPerms example
/lp group staff permission set conduit.send true
/lp group staff permission set conduit.get true
```

---

## Installation

1. Download `conduit-1.0.0.jar` (or build it yourself — see below).
2. Drop it into your Velocity proxy's `plugins/` folder.
3. Restart the proxy.
4. Assign the permissions above to your staff rank.

---

## Building from source

Requires Java 17+ and Maven.

```bash
git clone https://github.com/CoffeePNG/Conduit.git
cd Conduit
mvn package
```

The built jar will be at `target/conduit-1.0.0.jar`.

---

## Compatibility

| | Version |
|---|---|
| Platform | Velocity 3.x |
| Java | 17+ |

---

## Notes

- `/ct get` requires the staff member using the command to be **in-game** so Conduit knows which server to pull the player to.
- Both commands notify the moved player with a message so they aren't confused.
- Tab completion works for both player names and server names.
- `/ctg` with no player name lists everyone online across the whole network, grouped by server, with each name clickable to pull them straight to you.
