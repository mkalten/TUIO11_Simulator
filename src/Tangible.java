/*
	TUIO 1.1 Simulator - Tangible.java
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
import java.awt.geom.*;

public class Tangible {

	public int fiducial_id;
	public int session_id;
	public TangibleType type;
	
	public Shape geom;
	public Point2D center;
	public Color color;

	private boolean active;
	public boolean pushed = false;
	public float orientation = 0;

	private final float pi = (float)Math.PI;
	private final float halfPi = (float)(Math.PI/2);
	private final float doublePi = (float)(Math.PI*2);
	
	public float xspeed, yspeed, mspeed, maccel, rspeed, raccel;
	public Tangible previous = null;
	public Tangible next = null;

	private long lastTime;

	public Tangible(int s_id, int f_id, TangibleType type, boolean active, float xpos, float ypos, float angle) {
	
		this.session_id = s_id;
		this.fiducial_id = f_id;
		this.type = type;
		this.active = active;
		
		geom = type.getShape();
		center = type.center;
		color = type.color;

		float dx = xpos - (float)center.getX();
		float dy = ypos - (float)center.getY();
		translate(dx,dy);
	
		float dr = angle;
		rotate(dr-orientation);
		
		lastTime = System.currentTimeMillis();

		xspeed = 0.0f;
		yspeed = 0.0f;
		mspeed = 0.0f;
		rspeed = 0.0f;
		maccel = 0.0f;
		raccel = 0.0f;
	}

	public final void translate(float dx, float dy) {

		AffineTransform trans = AffineTransform.getTranslateInstance(dx,dy);
		geom = trans.createTransformedShape(geom);
		center = new Point2D.Float((float)(center.getX()+dx),(float)(center.getY()+dy));
		
		// normalized distances
		float nx = dx/TuioSimulator.width;
		float ny = dy/TuioSimulator.height;
		
		// time difference in seconds
		long currentTime = System.currentTimeMillis();
		float dt = (currentTime-lastTime)/1000.0f;
		
		float dist = (float)Math.sqrt(nx*nx+ny*ny);
		float new_speed = dist/dt;
		this.xspeed = nx/dt;
		this.yspeed = ny/dt;
		this.maccel = (new_speed-mspeed)/dt;
		this.mspeed = new_speed;
		lastTime = currentTime;
		
		if ((center.getX()<0) || (center.getY()<0)) deactivate();
	}

	public final void rotate(double theta) {

		orientation += theta;
		AffineTransform  trans = AffineTransform.getRotateInstance(theta,center.getX(),center.getY());
		geom = trans.createTransformedShape(geom);	

		// time difference in seconds
		long currentTime = System.currentTimeMillis();
		float dt = (currentTime - lastTime)/1000.0f;
		// normalized rotation
		double da = theta/doublePi;
		float new_speed  = (float)(da/dt);
		this.raccel = (new_speed-this.rspeed)/dt;
		this.rspeed = new_speed;
		lastTime = currentTime;		
	}
 
	public final Point getPosition() {
		return new Point((int)(center.getX()),(int)(center.getY()));
	}
	
	public Point getPointer() {

		int x = (int)Math.round(center.getX()-Math.cos(orientation-halfPi)*type.size/3.0f);
		int y = (int)Math.round(center.getY()-Math.sin(orientation-halfPi)*type.size/3.0f);
		return new Point(x,y);
	}
	
	public final float getAngle() {

		float angle =  (float)(orientation%doublePi);
		if (angle<0) angle += doublePi;
		return angle;
	}

	public final boolean containsPoint(int x, int y) {
		return geom.intersects(x,y,1,1);
	}

	public final boolean containsArea(Area testArea) {
		
		Area localArea = new Area(geom);
		testArea.intersect(localArea);

		return!(testArea.isEmpty());
	}

	public final void activate(int s_id) {

		active = true;
		session_id = s_id;
		
		lastTime = System.currentTimeMillis();
		xspeed = 0.0f;
		yspeed = 0.0f;
		mspeed = 0.0f;
		rspeed = 0.0f;
		maccel = 0.0f;
		raccel = 0.0f;
	}

	public final void stop() {		
		lastTime = System.currentTimeMillis();
		xspeed = 0.0f;
		yspeed = 0.0f;
		mspeed = 0.0f;
		rspeed = 0.0f;
		maccel = 0.0f;
		raccel = 0.0f;		
	}
	
	public final void deactivate() {

		active = false;
		
		lastTime = System.currentTimeMillis();
		xspeed = 0.0f;
		yspeed = 0.0f;
		mspeed = 0.0f;
		rspeed = 0.0f;
		maccel = 0.0f;
		raccel = 0.0f;		
	}
	
	public final boolean isActive() {

		return active;
	}
}
