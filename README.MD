# SasqPatch

A modding tool for patching the stream.dat asset file present in some games developed by Housemarque.

The name of this project comes from `UNP-Sasquatch`,
which is the namespace of the game engine's C++ classes used in games which use this file format. (discovered from crash reports)

Special thanks to @GoobyCorp who did the most of the initial reverse engineering of Nex Machina. This project couldn't have been done without him.

Games that are known to use this file format:
- [Nex Machina](https://housemarque.com/games/nexmachina/) (64-bit)
- [Outland](https://housemarque.com/games/outland/) (32-bit)

This project mainly focuses on patching `SCRI` assets, which are standard Lua 5.1 bytecode scripts used to define a lot of complex behaviour in these games.
Patching for other asset files is planned in the future due to file complexity.

## Dependencies
- Java 21 or newer installed on your system as the default java

## HOW TO USE
1. create the `./games/nexmachina/input` folder in this directory
2. Put `stream.dat` from Nex Machina into that input folder
3. Run `./gradlew run`
4. Edit scripts in `./games/nexmachina/dev`, add `--SP_recompile` to the start of the file for any scripts you want to modify
5. Run `./gradlew run` again
6. Copy `./games/nexmachina/output/stream.dat` into Nex Machina
7. Enjoy

Outland modding not yet implemented, but it's Work in Progress.