# Porter

Porter is an iOS app built using Re-Natal (ClojureScript and React Native).

## Development

### Install dependencies

    $ npm install
    $ cd node_modules/react-native-audio-streaming && pod install

Install dependencies for react-native-audio-streaming following the [document](https://github.com/tlenclos/react-native-audio-streaming#next-installation-steps-for-ios-choose-one-of-three-options-below)

### Run on simulator / device

    $ re-natal use-ios-device simulator (real)
    $ re-natal use-figwheel
    $ react-native run-ios

## Release

### Preparation

    $ vi node_modules/react-native/scripts/react-native-xcode.sh

Add `--expose-gc --max_old_space_size=4096 --optimize_for_size` options to `node` execution lines.

### Build

    $ lein prod-build
    $ react-native run-ios --configuration Release

## License

Copyright © 2017 @rinx

Distributed under the Eclipse Public License either version 1.0 or any later version.
