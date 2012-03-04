#!/bin/sh -x
CLASSPATH=../craftbukkit-1.1-R6.jar javac *.java -Xlint:deprecation
rm -rf me 
mkdir -p me/exphc/RadioBeacon
mv *.class me/exphc/RadioBeacon
jar cf RadioBeacon.jar me/ *.yml *.java LICENSE *.md
