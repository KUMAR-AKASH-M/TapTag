# TapTag — NFC/RFID reader (v1: read only)

Tap a card to the back of the phone and TapTag shows its UID, tech,
ATQA/SAK, Mifare Classic sector dump (default keys), and any NDEF message.
There is no write code in v1, so a card cannot be changed by this app.

## Stack

- Kotlin, AndroidX AppCompat, minSdk 24, targetSdk 34
- `android.nfc` reader mode + `MifareClassic` with the factory default key
- Package: `com.kumar.taptag`

## Open it

1. Open Android Studio (Ladybug or newer).
2. File → Open → pick this `taptag-nfc-reader-app` folder.
3. Let the first Gradle sync finish (needs internet once, downloads Gradle 8.7
   and the Android Gradle Plugin 8.5.2).
4. If Android Studio asks about the Gradle wrapper, let it use the wrapper
   from this project.

## Run it on your phone

1. On the phone: Settings → Developer options → turn on USB debugging.
   (Tap Build number 7 times in About phone if Developer options is hidden.)
2. Plug the phone into the laptop, allow the USB debugging prompt.
3. In Android Studio pick your phone in the device menu and press Run.
4. Keep the app open, tap a card to the back of the phone.

## Test with the college card

Expected for the LPU ID card: Mifare Classic, UID, ATQA 0x0200, SAK 0x18,
sectors readable with the default key. Sector 0 block 1 holds the registration
number in plain text — TapTag shows it, it does not change it.

## Troubleshooting

- "This device has no NFC" — the test phone has no NFC chip.
- Gradle sync fails — check Android Studio is Ladybug or newer (JDK 17) and
  that the laptop has internet for the first sync.
- Nothing happens on tap — make sure NFC is on, the app is in the foreground,
  and the card is held still against the phone's NFC spot for a second.

## What comes after v1

- Scan history (saved list of past scans)
- Writing NDEF text/URL records to NTAG cards
- Reading Mifare sectors with a custom key (never sector 0 of the college card)
