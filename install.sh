#!/bin/bash

adb install -r demo/build/outputs/apk/demo-debug.apk

if [ "$1" == "y" ]; then
	adb shell am start -n "com.liulishuo.filedownloader.demo/com.liulishuo.filedownloader.demo.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
fi
