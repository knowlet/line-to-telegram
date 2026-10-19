# line-to-telegram

### What?
Tool to convert a set of Line stickers into Telegram stickers.

### Download
Download the [latest JAR][1]

### Support
If you want to support the the project or show your appreciation feel free to **[buy me a coffee / donate](https://www.paypal.me/kekc1304/1)**

### How to use
1. Go to [Line stickershop][2]
1. Pick the set of stickers that you want ([Dragonball][3] for example)
1. Execute jar with the link as an argument
```
java -jar line-to-telegram.jar https://store.line.me/stickershop/product/1439/en
```
When work is done, a folder with images on it will be created.

Now you can publish your sticker pack, just do it via the Telegram [stickers bot][5]

### Result example

#### [DRAGONBALL][4]

<img src="https://github.com/kekc42/line-to-telegram/blob/master/Dragonball.PNG?raw=true" width="50%" />

[1]: https://github.com/lndmflngs/line-to-telegram/releases/latest
[2]: https://store.line.me/stickershop/showcase/top/en
[3]: https://store.line.me/stickershop/product/1439/en
[4]: https://t.me/addstickers/LineDBStrickers
[5]: https://t.me/Stickers

### Output path safety and offline tests

Pack titles are preserved, including Unicode, spaces and internal dots, but must
be safe single directory names. Invalid names are rejected rather than silently
renamed: this includes path separators, `.`/`..`, control characters, Windows
reserved characters/device names, and trailing dots/spaces. In particular, a
colon in a title is now rejected instead of being silently removed.
Images and captions are confined to that pack directory under the working
directory. Existing symbolic-link output directories/files are rejected. This
protects against untrusted titles; the working directory must still be trusted.
Pre-existing hard-link aliases to files outside it and concurrent modification
by another local process are outside this boundary.

Run `mvn test` for offline HTML/path/image fixtures (no LINE or Telegram calls).
The symlink fixture requires a filesystem/runtime that supports symbolic links.

### Continuous integration

The `Maven verification` GitHub Actions workflow runs
`mvn --batch-mode --no-transfer-progress clean verify` on pull requests and pushes
to `master`, using Linux and a Java 21 build JDK. The existing Java 8 bytecode
target and project dependencies are unchanged.

CI runs the offline HTML/path/image tests (including symbolic-link rejection)
and builds the Maven JAR. Maven may download build dependencies; the tests do not
call LINE or Telegram. This does not verify live sticker downloads, Telegram
publishing, Windows/macOS behavior, or the separate IntelliJ executable-JAR
assembly. It does not publish artifacts or create releases.
