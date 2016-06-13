TUIO SIMULATOR
--------------
Copyright (c) 2005-2016 Martin Kaltenbrunner <martin@tuio.org>
This software is part of reacTIVision, an open source fiducial
tracking and multi-touch framework based on computer vision. 

http://reactivision.sourceforge.net/

Introduction:
-------------
This Simulator can be used by application developers for the 
design of tangible user interfaces based on the TUIO framework,
without the actual need of a real table/camera setup in an
earlier project phase.

The TuioSimulator simulates a virtual table with tangibles.
It tracks the ID, position and orientation of virtual objects
on a virtual table surface. This data is sent via a network 
connection to any client application. It implements the TUIO protocol,
which is based on Open sound control. See the included PDF file for
more information on this protocol.

Additionally the simulator provides a multi-touch finger tracking
simulation, which is also included in the TUIO specification.

Application Start:
------------------
This application is based on the Java programming language.
It needs a Java Runtime Environment version 1.6 or later 
installed on your system in order to function properly.
See: http://java.sun.com/j2se/

You can start the application by double clicking TUIOSimulator.jar.
Alternativly you can call "java -jar TUIOSimulator.jar" from the command line.

The default TUIO host is 127.0.0.1 (localhost)
The default TUIO port is 3333.

If you want to send TUIO messages to an alternative host or port,
you can start the application with -host and -port command line options.

Handling:
---------
* Dragging objects onto the white table surface will make them visible
  for the "virtual sensor". An activated object is marked with a green frame.
  The corresponding object ID number is written onto the object surface.

* Shift-Dragging an object is equivalent to lifting an object from the surface.

* Right-Dragging an object rotates it.
  The black bar within the object indicates its current rotation angle. 

* Shift-Right-Dragging is changing the face of volumetric objects.
  Volumetric objects (e.g. cubes) are marked with a dot in their centre.

* Simple mouse-dragging on the table creates cursor events

* Press SHIFT while dragging to create a sticky cursor 

* Continue to manipulate any sticky cursor by moving its gray area 

* Remove sticky cursors by SHIFT clicking in its gray area 

* Press CTRL while dragging to add a cursor to a group

* Moving any cursor of a group will move the other group members

* Remove cursors from a group by CTRL clicking in its dark gray area 

* Choosing "Reset" from the "File" menu, resets the Simulator to its original state. 

* Enabling "Verbose" in the "File" menu, will print TUIO events to the console. 

Session Configuration:
----------------------
* see resources/config.xml for an example
* generic object types:
  name, shape, colour, description
* the current object list:
  type, faces, position, angle

References:
-----------
This application uses the JavaOSC OpenSound Control library.
See http://www.illposed.com/software/javaosc.html
for more information and the source code.
