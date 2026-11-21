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

## What's in v1.3

- **NDEF writing:** type some text or a URL, tap Write to card, hold a blank
  NTAG card to the phone. Text writes an NDEF text record, URL writes an
  NDEF URI record. Blank cards are formatted automatically if needed.
- **Safety:** writing to any Mifare Classic card is blocked in code — your
  college ID card cannot be written to by this app. Locked (read-only) cards
  and cards too small for the message are refused with a clear message.

## What's in v1.4

- **Pretend to be card (HCE demo):** a toggle on the main screen turns on
  card emulation. When ON, the phone answers any reader that selects
  TapTag's own AID with the card's UID, name and reg number. Try it with a
  second phone: tap this phone to it while it runs a tag reader app.
- **Honest limit:** this is plain Android HCE (ISO-DEP). It cannot imitate a
  Mifare Classic card — the phone has no Crypto-1 and the UID it sends is
  random — so it will NOT work at the college reader. The note under the
  button says so in the app too.

## What comes after v1.4

- Reading Mifare sectors with a custom key (never sector 0 of the college card)
