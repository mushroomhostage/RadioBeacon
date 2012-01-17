#!/bin/sh -x
CLASSPATH=../craftbukkit-1.0.1-R1.jar javac *.java
rm -rf me 
mkdir -p me/exphc/RadioBeacon
mv *.class me/exphc/RadioBeacon
jar cf RadioBeacon.jar me/ *.yml
cp RadioBeacon.jar ../plugins/
