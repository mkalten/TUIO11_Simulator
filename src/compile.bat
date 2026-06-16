javac -O -source 1.8 -target 1.8 -classpath src src\*.java
jar cfm TuioSimulator.jar src\manifest.inc -C src .
del src\*.class src\com\illposed\osc\*.class src\com\illposed\osc\utility\*.class
