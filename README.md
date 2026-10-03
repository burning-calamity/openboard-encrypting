# Steganographia

**ars per occultam scripturam animi voluntatem absentibus** is a privacy-focused Android keyboard with integrated classical and modern cipher tools, direct cipher typing, emoji search, and multiple keyboard layouts, including a Japanese Kana keyboard with Hiragana and Katakana layers.

The cipher toolbox also includes ROT13, Unicode-safe text reversal, and classical Tap code alongside the existing cipher collection.

<p align="center"><img src="fastlane/metadata/android/en-US/images/icon.png" height="192" alt="Steganographia keyboard lock icon"></p>

## Building

Import the project into Android Studio or build it with the checked-in wrapper:

```sh
./gradlew assembleDebug
```

The project uses JDK 17, Gradle 8.5, and the Android Gradle Plugin declared in the root build file.

Before importing a source archive, verify that it does not contain a duplicated cipher block:

```sh
python3 tools/check-direct-cipher-source.py
```

If Android Studio reports that `getDirectCipherText`, `resetDirectCipherState`, or related methods are already defined, the local `LatinIME.java` contains two historical versions of the direct-cipher implementation. Repair that checkout with:

```sh
python3 tools/fix-duplicate-direct-cipher-source.py
python3 tools/check-direct-cipher-source.py
```

The repair creates a `.duplicate-backup` beside `LatinIME.java`. Then run **Build > Clean Project** and **Build > Rebuild Project**. In the canonical file, the cipher helper block ends before `onEvent`; do not merge another helper block after it.

## License and attribution

This project is distributed under the GNU General Public License v3.0. See [LICENSE](LICENSE).

Steganographia incorporates and modifies materials from the OpenBoard project, AOSP LatinIME, LineageOS, Simple Keyboard, and Indic Keyboard. Original copyright and license notices are retained in the source tree and Git history. OpenBoard is an upstream project and is not affiliated with or responsible for this application.
