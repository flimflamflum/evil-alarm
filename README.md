# Evil Alarm

An Android alarm that is very hard to ignore. It rings at full volume, makes you prove you're awake,
and locks your distracting apps for the first half hour of your day.

## What it can do

### Alarms
- Create, edit, enable and delete alarms, each with a time, optional label and repeat days (or one-off).
- Rings at maximum volume with a full-screen ring screen, even over the lock screen.
- Alarms are rescheduled after a reboot.
- Snooze for 3, 5 or 10 minutes.
- Choose the alarm sound, with preview. Includes the EVIL ALARM track with beat-synced vibration.
  Any `.mp3`, `.ogg`, `.m4a` or `.wav` you add to `app/src/main/assets/sounds/` appears in the list automatically.

### Turning the alarm off
- **Math:** solve 2 problems. Difficulty is Easy, Medium or Hard.
- **QR code / barcode:** scan a code you registered (put it somewhere you have to walk to).
  If the scan fails, you fall back to 5 math problems.
- **Shake:** shake the phone 20 / 40 / 70 times on Easy / Medium / Hard.

### App blocker
- Blocks the apps you pick (Instagram and TikTok by default) using an Accessibility Service.
- The first alarm of the day starts the block the moment it rings. Snoozes and later alarms that day
  don't start another.
- Block length is configurable. A full-screen block screen shows a countdown.
- End a block early by solving 5 math problems.
- Start a block manually, or use the 10-second test alarm to try the whole flow without using up the day.

### Staying awake
- **Wake-up check:** 5 minutes after you turn the alarm off, tap a notification within 2 minutes
  or the alarm rings again.
- **Bedtime reminder:** a nudge 7, 8, 9 or 10 hours before the day's first alarm (or off).

### Stats
- Streak of mornings where the block wasn't ended early.
- Last 7 days, blocked-app attempts, time to get up, and times you fell back asleep.

### Setup
- Step-by-step setup on first launch (re-runnable from Settings) that walks through the required permissions.
- Battery-optimisation tips for Samsung, Xiaomi, OnePlus/Oppo/Realme, Huawei/Honor and Vivo.

## Run it on your phone

1. Install [Android Studio](https://developer.android.com/studio) and open this folder.
   Let it sync. It may offer to install Android SDK 36; accept.
2. On your phone: **Settings → About phone** → tap **Build number** 7 times, then enable
   **Developer options → USB debugging**. Plug the phone in and allow the computer.
3. Pick your phone in the device dropdown and press **Run ▶**.
4. In the app, fix anything listed under **Finish setup**, then use
   **Settings → Test alarm in 10 seconds** and lock the phone.
