#!/bin/sh
javac -O -Xlint:unchecked -source 1.6 -target 1.6 -classpath src src/*.java
jar cfm TuioSimulator.jar src/manifest.inc -C src .
rm -f src/*.class src/com/illposed/osc/*.class src/com/illposed/osc/utility/*.class
