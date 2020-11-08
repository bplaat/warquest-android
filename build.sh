# keytool -genkey -validity 10000 -keystore key.keystore -keyalg RSA -keysize 2048 -storepass warquest -keypass warquest
PATH=$PATH:~/android-sdk/build-tools/30.0.2:~/android-sdk/platform-tools
PLATFORM=~/android-sdk/platforms/android-30/android.jar
if [ "$1" == "log" ]; then
    adb logcat *:E
else
    if aapt package -m -J src -M AndroidManifest.xml -S res -I $PLATFORM; then
        mkdir classes
        if javac -Xlint -cp $PLATFORM -d classes src/nl/plaatsoft/warquest3/*.java; then
            dx.bat --dex --output=classes.dex classes
            aapt package -F warquest-unaligned.apk -M AndroidManifest.xml -S res -I $PLATFORM
            aapt add warquest-unaligned.apk classes.dex
            zipalign -f -p 4 warquest-unaligned.apk warquest.apk
            rm -r classes src/nl/plaatsoft/warquest3/R.java classes.dex warquest-unaligned.apk
            apksigner.bat sign --ks key.keystore --ks-pass pass:warquest --ks-pass pass:warquest warquest.apk
            adb install -r warquest.apk
            adb shell am start -n nl.plaatsoft.warquest3/.MainActivity
        else
            rm -r classes src/nl/plaatsoft/warquest3/R.java
        fi
    fi
fi
