/*
	TUIO 1.1 Simulator - Finger.java
	http://www.tuio.org/
	
	Copyright (c) 2005-2016 Martin Kaltenbrunner <martin@tuio.org>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

import java.awt.*;
import java.util.*;

public class Finger {

	public int session_id;

	private final float pi = (float)Math.PI;
	private final float halfPi = (float)(Math.PI/2);
	private final float doublePi = (float)(Math.PI*2);
	
	public float xspeed, yspeed, mspeed,maccel;
	private long lastTime;
	public Vector<Point> path;

	public Finger(int s_id, int xpos, int ypos) {
	
		this.session_id = s_id;

		path = new Vector<Point>();
		path.addElement(new Point(xpos,ypos));
		this.xspeed = 0.0f;
		this.yspeed = 0.0f;
		this.maccel = 0.0f;
		
		lastTime = System.currentTimeMillis();
	}

	public final void update(int xpos, int ypos) {

		Point lastPoint = getPosition();
		path.addElement(new Point(xpos,ypos));

		// time difference in seconds
		long currentTime = System.currentTimeMillis();
		float dt = (currentTime - lastTime)/1000.0f;
		
		if (dt>0) {
			float dx = (xpos - lastPoint.x)/(float)TuioSimulator.width;
			float dy = (ypos - lastPoint.y)/(float)TuioSimulator.height;
			float dist = (float)Math.sqrt(dx*dx+dy*dy);
			float new_speed  = dist/dt;
			this.xspeed = dx/dt;
			this.yspeed = dy/dt;
			this.maccel = (new_speed-mspeed)/dt;
			this.mspeed = new_speed; 
		} 
		lastTime = currentTime;
	}

	public final void stop() {
		lastTime = System.currentTimeMillis();
		this.xspeed = 0.0f;
		this.yspeed = 0.0f;
		this.maccel = 0.0f;
		this.mspeed = 0.0f;
	}
 
	public final Point getPosition() {
		return path.lastElement();	
	}

	public final Vector<Point> getPath() {
		return path;	
	}

}
