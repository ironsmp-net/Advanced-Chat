# Advanced-Chat
A simple and lightweight chat formatting plugin for Paper and Folia servers.
It uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html) for the chat format and optionally hooks into [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) for extra placeholders.
Basically like EssentialsX Chat or LPC, just without needing Essentials or LPC installed.

Players with the `advchat.minimessage` permission can use MiniMessage tags in their messages, and players with `advchat.colorcodes` can use legacy `&` color codes.
Everything else gets escaped so nobody can mess up the chat.

The plugin will only function on Paper (or its compatible forks) and Folia servers.

## Installation
Just drop the jar into your `plugins` folder. No other plugins are required.

[PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) is optional, if installed its placeholders will work in the chat format automatically.

## Configuration
The chat format is fully configurable in `config.yml` using MiniMessage and the following placeholders:
`%player%`, `%displayname%`, `%message%`, `%world%`, `%server%` and any PlaceholderAPI placeholder.

## Permissions
| Permission | Description | Default |
|---|---|---|
| `advchat.reload` | Allows `/advchat reload` | OP |
| `advchat.minimessage` | Allows MiniMessage formatting in chat | `false` |
| `advchat.colorcodes` | Allows legacy `&` color codes in chat | `false` |

## Building
Clone the repository, `cd` into it, then `./gradlew shadowJar`.

The output file will be located in `./build/libs/ironsmp-AdvancedChat-VERSION.jar`.
