#!/bin/sh -x
CLASSPATH=../craftbukkit-1.0.1-R1.jar javac *.java
rm -rf com
mkdir -p com/exphc/RadioBeacon
mv *.class com/exphc/RadioBeacon
jar cf RadioBeacon.jar com/ *.yml
cp RadioBeacon.jar ../plugins/
