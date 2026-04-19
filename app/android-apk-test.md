# Android APK Build and Test
The goal was to biuld the android apk, install it on a private phone and test the application

## Build
The APK was built using Android Studio via: ./gradlew assembleDebug
The generated APK is located at:
app/build/outputs/apk/debug/app-debug.apk

## Installation
apk was transferred to a private phone and installed manually
Device: Vivo V29
Android Version 15

## Test
-App did start normally, no errors were found
-UI loads properly
-Create Playername and select Playersymbol works
-Create Game works
-the created game do not appear in the gamelist, so no other players can join
-the games disappear if the player leaves the game.

