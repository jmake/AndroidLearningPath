
"""
wget https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
unzip gradle-8.11.1-bin.zip

gradle wrapper

./gradlew tasks

"""

clear 


key_create() 
{
    keytool -genkey -v -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass KEYSTORE_PASSWORD \
    -keypass KEY_PASSWORD \
    -alias KEY_ALIAS \
    -keystore keystore.jks \
    -dname "CN=${1},OU=,O=,L=,S=,C=US"    

    mv keystore.jks ./app
}


setup_gradle()
{
    export GRADLE_PATH="/home/somebody/z2026_2/TrainingAI/gradle-8.11.1/bin"
    export PATH="$PATH:$GRADLE_PATH"

    gradle --version
    ##gradle wrapper
}

build_android()
{
    ./gradlew clean --refresh-dependencies
    ##./gradlew tasks
    #./gradlew assembleDebug 
    #./gradlew assembleRelease

    find . -name "app*.apk"
    echo 
}


setup_cmdline_tools() 
{
    ## Settings > Language and Frameworks > Android SDK > SDK Tools > command lines 
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin 
    sdkmanager --version 

}

setup_sdk()  
{
    export ANDROID_HOME=$HOME/Android/Sdk
    #export PATH=$PATH:$ANDROID_HOME/emulator
    #export PATH=$PATH:$ANDROID_HOME/tools
    #export PATH=$PATH:$ANDROID_HOME/tools/bin
    export PATH=$PATH:$ANDROID_HOME/platform-tools
    adb --version  ## Android Debug Bridge version 1.0.41
    java --version  ## openjdk 21.0.11 


    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

}


install_apk() 
{
    AAPT_EXE=$(find "$ANDROID_HOME/build-tools" -name aapt | sort -V | tail -1)
    AAPT_VERSION=$(basename "$(dirname "$AAPT_EXE")")
    echo "AAPT_VERSION:'$AAPT_VERSION'"  


    find . -name "app*.apk"

##adb disconnect adb-R9YT61DRRYB-5PQ46V._adb-tls-connect._tcp

    adb devices
    ## List of devices attached
    ## 192.168.179.22:38895	device

    PORT=45411
    HOST="192.168.179.22:$PORT"

    adb connect "$HOST" | grep -q "connected to $HOST"
    ## already connected to 192.168.179.22:38895
    if [ $? -ne 0 ]; then
        echo "!!! ADB connection failed '$HOST' "
        exit 1
    else 
        echo "'$HOST' ok!!"
    fi

    #wget https://github.com/jmake/spicytech/releases/download/v0.1.0/app-release.apk  
    #adb install -r app-release.apk 

    adb install -r ./app/build/outputs/apk/debug/app-debug.apk ## :) 
    #adb install -r ./app/build/outputs/apk/release/app-release.apk ## :) 
    ## Performing Streamed Install
    ## Success 

    adb shell pm list packages | grep concept2ble3 ## com.spicy.concept2ble3

    adb shell monkey -p com.spicy.concept2ble3 1
    ##adb shell monkey -p com.spicy.concept2ble3 -c android.intent.category.LAUNCHER 1 

    adb shell settings get global bluetooth_on ## 1 
    adb shell dumpsys package com.spicy.concept2ble3 | grep granted 
    #adb logcat | grep com.spicy.concept2ble3

}


create_project_java_simplest() 
{

    AAPT_EXE=$(find "$ANDROID_HOME/build-tools" -name aapt | sort -V | tail -1)
    AAPT_VERSION=$(basename "$(dirname "$AAPT_EXE")")
    echo "AAPT_VERSION:'$AAPT_VERSION'"  

    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1) 

    rm -rf .git* .gradle *
    #find . -name build -type d -prune -exec rm -rf {} +

    APP_NAME="app_a" 

    ## groovy | kotlin 
    printf "1\nno\n" | gradle init \
    --dsl groovy \
    --type java-application \
    --test-framework junit \
    --project-name $APP_NAME \
    --java-version "$JAVA_VERSION"  \
    --package "spicytech" 

    rm -rf app/src/test 
    find . #app/src/

    ##ls -la 

    ##./gradlew tasks

    #./gradlew build
    #./gradlew run
    #./gradlew test


}


create_project_android_simplest() 
{
    rm gradlew*
    rm -rf .gradle gradle 
    rm -rf build 
    rm -rf app/build 

    ##git init 
    gradle wrapper
    #./gradlew tasks 
    ./gradlew assembleDebug
    ##./gradlew uninstallRelease 

    #ls -la 
}


setup_sdk 
setup_gradle
setup_cmdline_tools

##rm -rf ~/.gradle/caches

##key_create 
#build_android
#install_apk 


create_project_android_simplest 

