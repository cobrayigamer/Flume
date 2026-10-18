# Flume

A Minecraft proxy built on [Velocity-CTD](https://github.com/GemstoneGG/Velocity-CTD), which is itself a fork of [Velocity](https://github.com/PaperMC/Velocity).

> **Status: pre-release.** Flume is under active development. There are no official builds and no published benchmarks yet. Nothing here is production-ready.

## What is Flume?

Flume tracks Velocity-CTD closely and adds its own optimizations and modules on top. The goal is a proxy that stays compatible with the Velocity plugin ecosystem while improving performance and operability for busy networks.

Planned focus areas:

- Performance work, to be validated with public benchmarks against plain Velocity
- Regular upstream syncs from Velocity-CTD, with security fixes taken immediately
- Custom modules maintained in this repository

Claims about performance will be added only after they are measured and the numbers are published here.

## Building

Requirements: JDK 21 or newer (check `build.gradle.kts` and the toolchain config for the exact version this branch needs).

```
git clone https://github.com/cobrayigamer/Flume.git
cd Flume
gradlew.bat build        # Windows
./gradlew build          # Linux / macOS
```

Built jars are produced under `proxy/build/libs/`.

## Upstream and credits

Flume is a fork. Full credit goes to:

- The [PaperMC](https://github.com/PaperMC) team and contributors for Velocity
- [GemstoneGG](https://github.com/GemstoneGG) and contributors for Velocity-CTD

Their copyright notices and license headers are kept intact in the source files.

## Contributing

Issues and pull requests are welcome. Please open an issue before starting large changes. Never include secrets, server addresses, or private configuration in issues or pull requests.

## License

Flume is licensed under the [GNU General Public License v3.0](LICENSE), the same license as its upstream projects. If you distribute builds, you must make the corresponding source available under the same license.
