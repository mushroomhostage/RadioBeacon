#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.3-R0.1.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me 
mkdir -p me/exphc/RadioBeacon
mv *.class me/exphc/RadioBeacon
jar cf RadioBeacon.jar me/ *.yml *.java LICENSE *.md
