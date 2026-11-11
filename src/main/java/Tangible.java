/*
	TUIO 1.1 Simulator - Tangible.java
	http://www.tuio.org/

	Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>

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
import java.awt.geom.*;
import TUIO.*;

public class Tangible extends TuioObject {

	public TangibleType type;
	public Shape geom;
	public Color color;

	private Shape baseGeom;
	private float bcx, bcy;
	private boolean active;
	public boolean pushed = false;
	public boolean moved = false;
	public float vx = 0;
	public float vy = 0;
	public Tangible previous = null;
	public Tangible next = null;

	private final float pi = (float)Math.PI;
	private final float halfPi = (float)(Math.PI/2);
	private final float doublePi = (float)(Math.PI*2);

	public Tangible(int s_id, int f_id, TangibleType type, boolean active, float xpos, float ypos, float angle) {

		super(s_id,f_id,nx(xpos),ny(ypos),wire(angle));
		this.type = type;
		this.active = active;

		baseGeom = type.getShape();
		bcx = (float)type.center.getX();
		bcy = (float)type.center.getY();
		color = type.color;

		syncGeom();
	}

	private static float nx(float x) {
		return (x-TuioSimulator.border_width)/(float)TuioSimulator.table_width;
	}

	private static float ny(float y) {
		return (y-TuioSimulator.border_height)/(float)TuioSimulator.table_height;
	}

	private static float wire(float visual) {
		float angle = visual - (float)Math.PI;
		if (angle<0) angle += (float)(2*Math.PI);
		return angle;
	}

	private float px() {
		return xpos*TuioSimulator.table_width+TuioSimulator.border_width;
	}

	private float py() {
		return ypos*TuioSimulator.table_height+TuioSimulator.border_height;
	}

	public final void syncGeom() {
		AffineTransform trans = AffineTransform.getTranslateInstance(px(),py());
		trans.rotate(visualAngle());
		trans.translate(-bcx,-bcy);
		geom = trans.createTransformedShape(baseGeom);
	}

	public final void moveTo(TuioTime ttime, float x, float y, float a) {
		update(ttime,nx(x),ny(y),wire(a));
		syncGeom();
		moved = true;
		if ((px()<0) || (py()<0)) deactivate();
	}

	public final void moveBy(TuioTime ttime, float dx, float dy) {
		update(ttime,xpos+dx/TuioSimulator.table_width,ypos+dy/TuioSimulator.table_height,angle);
		syncGeom();
		moved = true;
		if ((px()<0) || (py()<0)) deactivate();
	}

	public final Point getPixelPosition() {
		return new Point((int)px(),(int)py());
	}

	public Point getPointer() {

		int x = (int)Math.round(px()-Math.cos(visualAngle()-halfPi)*type.size/3.0f);
		int y = (int)Math.round(py()-Math.sin(visualAngle()-halfPi)*type.size/3.0f);
		return new Point(x,y);
	}

	public final float visualAngle() {
		return (angle+pi)%doublePi;
	}

	public final boolean containsPoint(int x, int y) {
		return geom.intersects(x,y,1,1);
	}

	public final boolean containsArea(Area testArea) {

		Area localArea = new Area(geom);
		testArea.intersect(localArea);

		return!(testArea.isEmpty());
	}

	public final void activate() {
		active = true;
	}

	public final void deactivate() {
		active = false;
	}

	public final boolean isActive() {
		return active;
	}
}
