
function setup_paths() 
{
    ## adb.exe 
    ## java.exe 
    ## emulator.exe 
    $env:Path += ";$env:JAVA_HOME\bin"
    $env:Path += ";$env:ANDROID_HOME\emulator"
    $env:Path += ";$env:ANDROID_HOME\platform-tools"

    adb --version
    java --version 
    emulator -list-avds


    $env:Path += ";$env:ANDROID_HOME\cmdline-tools\latest\bin"
    sdkmanager.bat --version 

    $env:Path += ";$env:GRADLE_HOME\bin"
    gradle.bat --version
}

function adb_devices()
{
    adb kill-server 
    adb start-server 
    adb devices

}


<#----------------------------------------------------------------------#>
clear

## X) SetUp
$originalPath = $env:Path

$env:JAVA_HOME = "D:\z2025_1\Android\Studio\jbr"
$env:GRADLE_HOME = "F:\z2025_1\Unity\Github\Gradle9_0_0\gradle-9.0.0"
$env:ANDROID_HOME = "D:\z2025_1\Android\SDK"


setup_paths
adb_devices 
