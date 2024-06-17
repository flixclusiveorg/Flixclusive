#!/bin/bash

DIR="build/stubs"
FILE="$DIR/AndroidManifest.xml"

rm -rf $DIR

./gradlew generateStubsJar

mkdir -p $DIR

cat <<EOL > $FILE
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.flixclusive" >

    <uses-sdk android:minSdkVersion="21" />

</manifest>
EOL

cd $DIR || exit
zip provider-stubs.aar *
echo "[AAR]> Stubs have been generated"