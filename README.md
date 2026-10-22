# Evil Alarm

A very loud Android alarm that makes you solve math to turn it off. Later phases add an app blocker
that locks Instagram/TikTok for 30 minutes after the first alarm of the day.

## Status

- **Phase 1 (done):** alarms, max-volume ringing, math to dismiss, sound/difficulty/snooze/hard-mode settings.
- **Phase 2 (done):** app blocker (Accessibility Service), blocked-apps picker, block screen with countdown,
  5 math problems to end a block early, manual "start block" for testing. EVIL ALARM track with
  beat-synced vibration.
- **Phase 3 (done):** the first alarm of the day starts the block the moment it rings. Snoozes and later
  alarms that day don't start another; the Settings test alarm always starts one without using up the day.
- **Phase 4 (in progress):** step-by-step setup screen on first launch (also under Settings → Run setup again),
  with phone-specific battery tips for Samsung, Xiaomi, OnePlus/Oppo/Realme, Huawei/Honor and Vivo.
  Still to do: hard mode enforcement.
- **Extras (done):**
  - Turn the alarm off by math, scanning a QR code/barcode you registered, or shaking the phone
    (20/40/70 shakes for easy/medium/hard). A failed scan falls back to 5 math problems.
  - Wake-up check: 5 minutes after you turn it off, tap a notification within 2 minutes or it rings again.
  - Stats: streak of blocked mornings not ended early, last 7 days, blocked-app attempts, time to get up,
    times you fell back asleep.
  - Bedtime reminder 7/8/9/10 hours before the day's first alarm (or off).

## Run it on your phone

1. Install [Android Studio](https://developer.android.com/studio) and open this folder.
   Let it sync. It may offer to install Android SDK 36; accept.
2. On your phone: **Settings → About phone** → tap **Build number** 7 times, then enable
   **Developer options → USB debugging**. Plug the phone in and allow the computer.
3. Pick your phone in the device dropdown and press **Run ▶**.
4. In the app, fix anything listed under **Finish setup**, then use
   **Settings → Test alarm in 10 seconds** and lock the phone.

## Adding alarm sounds

Drop `.mp3` files into `app/src/main/assets/sounds/` and rebuild. Each one appears in
**Settings → Alarm sound** (file `air_horn.mp3` shows as "Air horn").

## Layout

```
app/src/main/java/com/evilalarm/app/
  alarm/   scheduling (AlarmManager), ringing service, boot receiver, sounds
  data/    alarms + settings stored in DataStore
  math/    question generator (easy / medium / hard)
  ring/    full-screen ring activity with the math keypad
  ui/      alarm list, editor, settings, glass theme
```
