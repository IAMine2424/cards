# Cards

A card wallet for the Light Phone III: Cineville, Museumkaart, loyalty cards, anything with a barcode.

- Any number of cards, stored only on the phone
- Each card shows its number above the barcode
- A card has a separate printed number and barcode value, since some cards (e.g. Museumkaart) encode something other than what's printed
- Barcode types: Code 128 (default), QR, EAN 13, Code 39, PDF 417, Aztec, Data Matrix; tap the type at the bottom to switch

Built on the [Light SDK](https://github.com/lightphone/light-sdk); the app lives in [`tool/`](tool). SDK docs: [SDK_README.md](SDK_README.md).

## Build

```
./gradlew -DlightSdk.toolOnly=true :tool:assembleRelease
adb install tool/build/outputs/apk/release/tool-release.apk
```
