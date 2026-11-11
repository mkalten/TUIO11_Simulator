# TUIO Simulator

Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>

This software is part of reacTIVision, an open source fiducial tracking and multi-touch framework based on computer vision.
<https://github.com/mkalten/reacTIVision>

## Introduction

This Simulator can be used by application developers for the design of tangible user interfaces based on the TUIO framework, without the actual need of a real table/camera setup in an earlier project phase.

The TuioSimulator simulates a virtual table with tangibles. It tracks the ID, position and orientation of virtual objects on a virtual table surface. This data is sent via a network connection to any client application. It implements the TUIO protocol, which is based on Open Sound Control. See the included PDF file for more information on this protocol.

Additionally the simulator provides a multi-touch finger tracking simulation, which is also included in the TUIO specification.

## Building

The project is built with Maven 3 and targets Java 8. It depends on the libTUIO Java library (`org.tuio:tuio:1.1.6`), which is not published on Maven Central. Build and install it first from the [TUIO11_JAVA](https://github.com/mkalten/TUIO11_JAVA) project, or install the bundled `libTUIO.jar` into your local Maven repository:

```bash
mvn install:install-file -Dfile=libTUIO.jar -DgroupId=org.tuio -DartifactId=tuio -Dversion=1.1.6 -Dpackaging=jar
```

Then package the simulator:

```bash
mvn package      # builds target/TuioSimulator.jar
```

## Application Start

This application needs a Java Runtime Environment version 1.8 or later installed on your system in order to function properly.

You can start the application by double clicking `TuioSimulator.jar`, or alternatively:

```bash
java -jar target/TuioSimulator.jar
```

The default TUIO host is 127.0.0.1 (localhost) and the default TUIO port is 3333 (UDP). If you want to send TUIO messages to an alternative host or port, you can start the application with the `-host` and `-port` command line options:

```bash
java -jar target/TuioSimulator.jar -host 192.168.0.1 -port 3333 -config resources/config.xml
```

## Handling

- Dragging objects onto the white table surface will make them visible for the "virtual sensor". An activated object is marked with a green frame. The corresponding object ID number is written onto the object surface.
- Dragging an object with both mouse buttons is equivalent to lifting it from the surface.
- Right-Dragging an object rotates it. The black bar within the object indicates its current rotation angle.
- Shift-Dragging an object changes the face of volumetric objects. Volumetric objects (e.g. cubes) are marked with a dot in their centre.
- Dragged objects push other objects out of their way; pushed objects keep sliding briefly and settle due to friction. Collision detection is enabled by default and can be toggled in the "Options" menu.
- Objects cannot be moved outside the window bounds.
- Simple mouse-dragging on the table creates cursor events; dragging a cursor into an object pushes it along.
- Press SHIFT while dragging to create a sticky cursor
- Continue to manipulate any sticky cursor by moving its gray area
- Remove sticky cursors by SHIFT clicking in its gray area
- Press CTRL while dragging to add a cursor to a group
- Moving any cursor of a group will move the other group members
- Remove cursors from a group by CTRL clicking in its dark gray area
- Choosing "Reset" from the "Options" menu resets the Simulator to its original state.
- Enabling "Verbose" in the "Options" menu will print TUIO events to the console.

## Session Configuration

- see `resources/config.xml` for an example
- generic object types: name, shape, colour, description
- the current object list: type, faces, position, angle

## References

This application uses the libTUIO Java reference implementation.
See <https://github.com/mkalten/TUIO11_JAVA> for more information and the source code.
