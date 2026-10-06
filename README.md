# BOING! v0.1

A tiny native Android arcade game. Android 6.0 or newer. No game engine, account, ads, downloaded assets, or internet permission. All art is drawn on the phone, sounds are generated locally, and your best score stays on that device.

## Play

Swipe upward anywhere in the bottom launch area and release to fire. Your swipe direction sets the aim; speed is fixed to keep it forgiving. Hold your swipe for 0.3 seconds to reveal dots showing the path up to the first wall bounce or target. Aim gently snaps toward nearby targets. The ball returns when it reaches the bottom or after 11 seconds.

Pink bunnies need one hit, mint bears two, yellow blobs three. Dots on each character show remaining hits. Each hit gives 5 points; clearing characters in one shot adds an increasing combo bonus (capped at eight). Characters marked **+** give three upcoming multi-ball shots. More pickups refresh those three shots. Three balls share the same shot and combo.

New characters arrive every six seconds initially, accelerating to every 1.6 seconds. The top bar fills with the arena; 20 characters ends the game. Clear space before that happens! Tap PLAY AGAIN to restart. Tap Sound ON/OFF at the top right to mute. Haptics follow your phone's system settings. Switching apps pauses the game; returning resumes it. An interrupted process starts a fresh run but preserves the best score and sound preference.

## Build and install using only your Android phone

The downloadable project ZIP is **source code, not an APK**. These steps turn it into an installable app through GitHub. You need internet for this setup and build, then the installed game works in airplane mode.

1. Download **BOING-v0.1-project.zip** from this chat to your phone's Downloads folder. Keep it zipped.
2. In Chrome, open https://github.com and sign in or create an account. Create a new repository named **boing**. Choose **Public** for standard free hosted Actions builds, or Private if your account has available Actions minutes. Add a README and create the repository. The source contains no personal information.
3. On the repository page choose **Code → Codespaces → Create codespace on main**. If controls are missing, enable Chrome's **Desktop site**. Codespaces requires available quota and can take a few minutes to open; no paid plan is needed when free quota is available. A small Bluetooth keyboard is optional.
4. In the Codespaces file explorer, open the menu on the repository folder and choose **Upload**. Select the project ZIP from Downloads. You should see it beside README.md. The ZIP already contains the hidden `.github` build folder.
5. Open **Terminal → New Terminal** in Codespaces. Paste each line below and press Enter after each one. These commands unpack the project, save it, and start the cloud build:

```sh
unzip -o BOING-v0.1-project.zip
git add .
git commit -m "Add BOING playable prototype"
git push
```

6. Return to the repository on github.com, open **Actions**, and select **Build BOING APK**. Wait for a green check. The first build may take several minutes. If it has not started, open the workflow and select **Run workflow → Run workflow**.
7. Open the completed run. Under **Artifacts**, tap **BOING-v0.1-APK** to download it. You must be signed in. Use your phone's Files app to extract the downloaded ZIP; inside is **app-debug.apk**.
8. Tap **app-debug.apk**, allow that Files app to install unknown apps if Android asks, and tap **Install**. Open **BOING!**. You do not need USB debugging or Android Studio. You can disable the unknown-app permission again afterward.
9. Test in airplane mode. Swipe up, hold to preview, hit a + character, and try restarting after the arena fills. Stop your Codespace from https://github.com/codespaces when done so it does not consume compute quota.

The APK is automatically signed with a development key and intended for personal prototype testing, not Play Store distribution. Hosted builds may use a different development key on later runs. If Android refuses an update with a signing conflict, uninstall the old prototype first; uninstalling removes its high score. Stable release signing is a later step.

### If something gets stuck

- No Codespaces access: check your account quota and Codespaces availability. This phone-only route depends on GitHub offering those services to your account.
- Red build: tap the failed step in Actions for its log and send the error text in this chat. The workflow installs Java, Gradle, and the Android SDK automatically.
- No artifact: artifacts appear only after a successful build; they expire after 30 days. Run the workflow again if needed.
- Can't install: extract the artifact ZIP first and open the `.apk`, not the ZIP. Android may label it an unknown app because it is a personally built prototype.
- No sound or vibration: check media volume and the phone's haptic setting. Sound is optional and the game remains playable without it.

## Project notes

- Native Java Activity + Canvas View, one fixed portrait arena, three target types.
- Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, compile/target SDK 35, minimum SDK 23.
- GitHub Actions runs `gradle --no-daemon :app:assembleDebug :app:lintDebug` and uploads the debug APK.
- No Gradle wrapper is needed in the cloud because setup-gradle supplies the pinned Gradle version. For local builds install Gradle 8.9 and Android SDK 35, then run the same command.
- Source: `app/src/main/java/com/boing/game/GameView.java`. Adjust the spawn interval or LIMIT there for difficulty tuning.
- Prototype limitations: no saved in-progress run, no menus/worlds/cosmetics, wall preview stops at the first target, generated simple tones, no music, no Play Store signing. A long shot gains downward pull and returns at 11 seconds so you never wait indefinitely.

## Validation status

Project structure and XML were checked during creation. Java compilation, Android lint, installation, and gameplay on a device have **not** been verified in this workspace, which has no Java/Android build toolchain. The included workflow performs compilation and lint in the cloud. A successful build still needs the short phone playtest above to confirm feel and device behavior.

Build workflow reference: https://github.com/gradle/actions/blob/main/setup-gradle/README.md
Android APK build reference: https://developer.android.com/build/building-cmdline
