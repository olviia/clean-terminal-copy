# Clean Terminal Copy

A JetBrains IDE plugin (Rider, IntelliJ IDEA, PyCharm, …) that cleans text you copy from the
built-in terminal, made for replies of Claude Code and other CLI agents.

Copying a reply from the terminal normally brings the terminal's layout with it:

```
  Instead of taking over Ctrl+C, we let Rider handle it exactly as
  now: copy when text is selected, interrupt when nothing is. Rider
  - Collaborators: IndentStripper, and Rider's clipboard
    (CopyPasteManager).
```

With the plugin, the same selection pastes as:

```
Instead of taking over Ctrl+C, we let Rider handle it exactly as now: copy when text is selected, interrupt when nothing is. Rider
- Collaborators: IndentStripper, and Rider's clipboard (CopyPasteManager).
```

## What it does

Right after a terminal copy (Ctrl+C with a selection, or *Copy* in the terminal's context menu):

- removes the margin shared by all lines, keeping relative indentation;
- re-joins lines broken only because the terminal was too narrow, including wrapped list items;
- drops trailing padding spaces and the agent's leading message bullet (`●`).

Ctrl+C itself is untouched: without a selection it still interrupts. Copies anywhere else in the
IDE are not changed.

A line counts as wrapped when the first word of the next row would not have fit on it, and that
row lines up with the text above it. Two short lines are never joined, so code and command lists
stay as they are.

## Install

Download the zip from Releases, then *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

## Build

Requires JDK 21+ (the JetBrains Runtime bundled with your IDE works).

```
./gradlew test buildPlugin
```

The zip lands in `build/distributions/`. `platformLocalPath` in `gradle.properties` builds against
an installed IDE; leave it empty to download the platform instead.

## License

MIT
