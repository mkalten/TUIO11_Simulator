/*
    TUIO 1.1 Simulator - Simulation.java
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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import TUIO.*;

public class Simulation extends JPanel {

	private Manager manager;
	private TuioServer tuioServer;

	private TuioTime frameTime;
	private long lastFrameTime = -1;
	private long lastPeriodicTime = 0;
	private boolean periodic = false;
	private javax.swing.Timer timer;

	private final float doublePi = (float)(Math.PI*2);
	private static final float FRICTION = 20.0f;
	private static final float RESTITUTION = 0.2f;
	private static final float TRANSFER = 0.5f;
	private static final float MIN_SPEED = 40f;
	private static final float MIN_PUSH = 3f;
	private static final float MAX_SPEED = 1500f;

	Shape vision, vision_rectangle, vision_circle;
	Shape table_border,table,table_rectangle,table_circle;
	private Stroke normalStroke = new BasicStroke(2.0f);
	private Stroke borderStroke = new BasicStroke(2.0f);
	private Color borderColor = new Color(0,200,0);
	private Stroke gestureStroke = new BasicStroke(1.0f);

	Tangible selectedObject = null;
	TuioCursor selectedCursor = null;
	int lastX  = -1;
	int lastY  = -1;
	int clickX = 0;
	int clickY = 0;

	Vector<Integer> stickyCursors = new Vector<Integer>();
	Vector<Integer> jointCursors = new Vector<Integer>();

	boolean showName = false;
	int nameId, nameX, nameY;
	String objectName = "";

	int window_width = TuioSimulator.width;
	int window_height = TuioSimulator.height;
	int table_width = TuioSimulator.table_width;
	int table_height = TuioSimulator.table_height;
	int border_width = TuioSimulator.border_width;
	int border_height = TuioSimulator.border_height;

	public Simulation(Manager manager, String host, int port) {
		super(true);
		this.manager=manager;

		TuioTime.initSession();
		frameTime = TuioTime.getSessionTime();
		tuioServer = new TuioServer(host,port);
		tuioServer.setSourceName("tsim");
		tuioServer.enableBlobProfile(false);
		tuioServer.initFrame(frameTime);

		// init the table setup
		vision_rectangle = new Area(new Rectangle2D.Float(border_width-8,border_height-8,table_width+16,table_height+16));
		vision_circle = new Area(new Ellipse2D.Float((table_width-table_height)/2+border_width-8,border_height-8,table_height+16,table_height+16));
		vision = vision_rectangle;

		table_border = new Rectangle2D.Float(0,0,window_width,window_height);
		table_rectangle = new Rectangle2D.Float(border_width,border_height,table_width,table_height);
		table_circle = new Ellipse2D.Float((window_width-table_height)/2,border_height,table_height,table_height);
		table = table_rectangle;

		// listens to the mouseDown event
		addMouseListener (
			new MouseAdapter () {
				public void mousePressed (MouseEvent evt) {
					mouse_pressed(evt);

				}
			}
		);

		// listens to the mouseDragged event
		addMouseMotionListener (
			new MouseMotionAdapter () {
				public void mouseDragged (MouseEvent evt) {
					mouse_dragged(evt);
				}
			}
		);

		// listens to the mouseReleased event
		addMouseListener (
			new MouseAdapter () {
				public void mouseReleased (MouseEvent evt) {
					mouse_released(evt);
				}
			}
		);

		// listens to the mouseMoved event
		addMouseMotionListener (
			new MouseMotionAdapter () {
				public void mouseMoved (MouseEvent evt) {
					mouse_moved(evt);
				}
			}
		);

		// frame pump: commits the TUIO state at a fixed rate,
		// then opens the next pending frame for the coming events
		timer = new javax.swing.Timer(20, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				physicsStep();
				tuioServer.stopUntouchedMovingObjects();
				tuioServer.stopUntouchedMovingCursors();
				tuioServer.commitFrame();
				frameTime = TuioTime.getSessionTime();
				tuioServer.initFrame(frameTime);

				long currentTime = System.currentTimeMillis();
				if (periodic && (currentTime-lastFrameTime>1000) && (currentTime-lastPeriodicTime>=1000)) {
					tuioServer.sendFullMessages();
					lastPeriodicTime = currentTime;
				}
			}
		});
		timer.start();

		reset();
	}

	public void limit (boolean circular) {

		if (circular) {
			vision = vision_circle;
			table = table_circle;
		} else {
			vision = vision_rectangle;
			table = table_rectangle;
		}

		for (Tangible tangible : new ArrayList<Tangible>(manager.objectList.values())) {
			if (vision.contains(tangible.geom.getBounds2D())) {
				if (!tangible.isActive()) addObject(tangible);
			} else if (tangible.isActive() || (tuioServer.getTuioObject(tangible.getSessionID())!=null)) removeObject(tangible);
		}
	}

	private float xpos(Point point) {
		return (point.x-border_width)/(float)table_width;
	}

	private float ypos(Point point) {
		return (point.y-border_height)/(float)table_height;
	}

	private int pointX(TuioPoint point) {
		return (int)(point.getX()*table_width)+border_width;
	}

	private int pointY(TuioPoint point) {
		return (int)(point.getY()*table_height)+border_height;
	}

	private Point cursorPoint(TuioCursor cursor) {
		return new Point(pointX(cursor),pointY(cursor));
	}

	private void addObject(Tangible tangible) {
		int old_id = (int)tangible.getSessionID();
		tuioServer.addExternalTuioObject(tangible);
		manager.activateObject(old_id,(int)tangible.getSessionID());
	}

	private void updateObject(Tangible tangible) {
		tuioServer.updateExternalTuioObject(tangible);
	}

	private void removeObject(Tangible tangible) {
		tuioServer.removeExternalTuioObject(tangible);
		manager.deactivateObject(tangible);
	}

	private TuioCursor addCursor(int x, int y) {
		return tuioServer.addTuioCursor(xpos(new Point(x,y)),ypos(new Point(x,y)));
	}

	private void updateCursor(TuioCursor cursor, int x, int y) {
		tuioServer.updateTuioCursor(cursor,xpos(new Point(x,y)),ypos(new Point(x,y)));
		repaint();
	}

	private void removeCursor(TuioCursor cursor) {
		tuioServer.removeTuioCursor(cursor);
		repaint();
	}

	// activates, updates or deactivates all objects moved during this event
	private void syncMovedObjects() {
		for (Tangible tangible : new ArrayList<Tangible>(manager.objectList.values())) {
			if (!tangible.moved) continue;
			tangible.moved = false;
			if (vision.contains(tangible.geom.getBounds2D())) {
				if (!tangible.isActive()) addObject(tangible);
				else updateObject(tangible);
			} else if (tangible.isActive() || (tuioServer.getTuioObject(tangible.getSessionID())!=null)) {
				removeObject(tangible);
			}
		}
	}

	private boolean overlaps(Tangible a, Tangible b) {
		return b.containsArea(new Area(a.geom));
	}

	private boolean hitsObject(int x, int y) {
		for (Tangible tangible : manager.objectList.values()) {
			if (tangible.containsPoint(x,y)) return true;
		}
		return false;
	}

	// clamps a translation step to the window bounds
	private float clampX(Tangible tangible, float dx) {
		Rectangle2D bounds = tangible.geom.getBounds2D();
		if ((dx<0) && (bounds.getMinX()+dx<0)) return (float)-bounds.getMinX();
		if ((dx>0) && (bounds.getMaxX()+dx>window_width)) return (float)(window_width-bounds.getMaxX());
		return dx;
	}

	private float clampY(Tangible tangible, float dy) {
		Rectangle2D bounds = tangible.geom.getBounds2D();
		if ((dy<0) && (bounds.getMinY()+dy<0)) return (float)-bounds.getMinY();
		if ((dy>0) && (bounds.getMaxY()+dy>window_height)) return (float)(window_height-bounds.getMaxY());
		return dy;
	}

	// clamps a target position so the whole shape stays inside the window
	private Point clampTarget(Tangible t, int x, int y) {

		Rectangle2D bounds = t.geom.getBounds2D();
		Point center = t.getPixelPosition();
		double dx = x-center.x;
		double dy = y-center.y;
		double minDx = -bounds.getMinX();
		double maxDx = window_width-bounds.getMaxX();
		double minDy = -bounds.getMinY();
		double maxDy = window_height-bounds.getMaxY();
		if (dx<minDx) dx = minDx;
		if (dx>maxDx) dx = maxDx;
		if (dy<minDy) dy = minDy;
		if (dy>maxDy) dy = maxDy;
		return new Point(center.x+(int)Math.round(dx),center.y+(int)Math.round(dy));
	}

	// shoves the topmost object under the cursor along the drag direction
	private void cursorPush(int x, int y, int dx, int dy) {

		if ((dx==0) && (dy==0)) return;
		ArrayList<Tangible> objects = new ArrayList<Tangible>(manager.objectList.values());
		for (int i=objects.size()-1; i>=0; i--) {
			Tangible hit = objects.get(i);
			if (hit.containsPoint(x,y)) {
				Point pos = hit.getPixelPosition();
				pushMove(hit,pos.x+dx,pos.y+dy);
				return;
			}
		}
	}

	// moves the dragged object to its target and pushes obstructing objects away
	private void pushMove(Tangible dragged, int x, int y) {

		Point target = clampTarget(dragged,x,y);
		float dx = target.x-dragged.getPixelPosition().x;
		float dy = target.y-dragged.getPixelPosition().y;
		manager.updateObject(dragged,target.x,target.y,dragged.visualAngle(),frameTime);

		if ((dx!=0) || (dy!=0)) {
			float dist = (float)Math.sqrt(dx*dx+dy*dy);
			float dt = (System.currentTimeMillis()-lastFrameTime)/1000f;
			if (dt<=0) dt = 0.02f;
			float speed = Math.min(dist/dt,MAX_SPEED);
			dragged.pushed = true;
			resolvePushes(dragged,dx/dist,dy/dist,0,speed);
			dragged.pushed = false;
		}
	}

	private void resolvePushes(Tangible pushed, float dirx, float diry, int depth, float speed) {

		if (depth>32) return;
		for (Tangible other : new ArrayList<Tangible>(manager.objectList.values())) {
			if ((other==pushed) || other.pushed) continue;
			if (!overlaps(pushed,other)) continue;

			// push along the contact direction, from pusher to pushee
			float ox = other.getPixelPosition().x-pushed.getPixelPosition().x;
			float oy = other.getPixelPosition().y-pushed.getPixelPosition().y;
			float odist = (float)Math.sqrt(ox*ox+oy*oy);
			float odirx = dirx;
			float odiry = diry;
			if (odist>0.01f) {
				odirx = ox/odist;
				odiry = oy/odist;
			}

			// a held object acts as an immovable wall
			if (other==selectedObject) {
				for (int step=0; step<128 && overlaps(pushed,other); step++) {
					pushed.moveBy(frameTime,-odirx,-odiry);
				}
				pushed.vx = 0;
				pushed.vy = 0;
				continue;
			}

			// push the colliding object along the contact direction until clear
			other.pushed = true;
			boolean cleared = false;
			float displaced = 0;
			for (int step=0; step<128 && !cleared; step++) {
				float stepx = clampX(other,odirx);
				float stepy = clampY(other,odiry);
				if ((stepx==0) && (stepy==0)) break;
				other.moveBy(frameTime,stepx,stepy);
				displaced += Math.abs(stepx)+Math.abs(stepy);
				cleared = !overlaps(pushed,other);
			}
			// momentum only transfers on a real shove, not a grazing contact
			if (cleared && (displaced>=MIN_PUSH)) {
				other.vx = odirx*speed*TRANSFER;
				other.vy = odiry*speed*TRANSFER;
			}
			resolvePushes(other,odirx,odiry,depth+1,speed*TRANSFER);

			// if the obstacle could not be pushed away, push the pressed object back to contact and stop it
			if (!cleared) {
				for (int step=0; step<128 && overlaps(pushed,other); step++) {
					pushed.moveBy(frameTime,-odirx,-odiry);
				}
				pushed.vx = 0;
				pushed.vy = 0;
			}
			other.pushed = false;
		}
	}

	// slides pushed objects with momentum, friction and gentle edge bounces
	private void physicsStep() {

		if (!manager.collision) return;
		boolean moved = false;
		float dt = 0.02f;
		for (Tangible tangible : new ArrayList<Tangible>(manager.objectList.values())) {
			if ((tangible==selectedObject) || tangible.moved) continue;
			float speed = (float)Math.sqrt(tangible.vx*tangible.vx+tangible.vy*tangible.vy);
			if (speed<MIN_SPEED) {
				tangible.vx = 0;
				tangible.vy = 0;
				continue;
			}

			float stepx = clampX(tangible,tangible.vx*dt);
			float stepy = clampY(tangible,tangible.vy*dt);
			if (stepx!=tangible.vx*dt) tangible.vx = -tangible.vx*RESTITUTION;
			if (stepy!=tangible.vy*dt) tangible.vy = -tangible.vy*RESTITUTION;
			tangible.moveBy(frameTime,stepx,stepy);

			tangible.pushed = true;
			resolvePushes(tangible,tangible.vx/speed,tangible.vy/speed,0,speed);
			tangible.pushed = false;

			float decay = (float)Math.exp(-FRICTION*dt);
			tangible.vx *= decay;
			tangible.vy *= decay;
			moved = true;
		}
		if (moved) {
			syncMovedObjects();
			repaint();
		}
	}

	public void setVerbose(boolean verbose) {
		tuioServer.setVerbose(verbose);
	}

	public void setInvertXpos(boolean invert) {
		tuioServer.setInvertXpos(invert);
	}

	public void setInvertYpos(boolean invert) {
		tuioServer.setInvertYpos(invert);
	}

	public void setInvertAngle(boolean invert) {
		tuioServer.setInvertAngle(invert);
	}

	public void quit() {
		reset();
		timer.stop();
		tuioServer.close();
	}

	public void reset() {
		stickyCursors.clear();
		jointCursors.clear();

		tuioServer.resetTuioObjects();
		tuioServer.resetTuioCursors();

		for (Tangible tangible : new ArrayList<Tangible>(manager.objectList.values())) {
			if (tangible.isActive()) manager.deactivateObject(tangible);
			if (vision.contains(tangible.geom.getBounds2D())) addObject(tangible);
		}

		lastFrameTime = -1;
	}

	public void paint(Graphics g) {
		update(g);
	}

	public void update(Graphics g) {

		// setup the graphics environment
		Graphics2D g2 = (Graphics2D)g;
		g2.setFont(new Font("Helvetica", Font.PLAIN, 14));

		if (manager.antialiasing) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		} else {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		}

		// draw the table & border
		g2.setPaint(Color.gray);
		g2.fill(table_border);
		g2.setPaint(Color.white);
		g2.fill(table_rectangle);

		//draw the round table edge
		g2.setPaint(Color.lightGray);
		g2.draw(table_circle);

		// paint the cursors
		for (TuioCursor cursor : tuioServer.getTuioCursors()) {
			java.util.List<TuioPoint> gesture = cursor.getPath();
			if (gesture.size()>0) {
				g2.setPaint(Color.blue);
				g2.setStroke(gestureStroke);
				TuioPoint start = gesture.get(0);
				for (TuioPoint end : gesture) {
					g2.draw(new Line2D.Double(pointX(start),pointY(start),pointX(end),pointY(end)));
					start = end;
				}
				if (jointCursors.contains((int)cursor.getSessionID())) g2.setPaint(Color.darkGray);
				else g2.setPaint(Color.lightGray);
				g2.fill(new Ellipse2D.Double(pointX(start)-5,pointY(start)-5,10,10));
			}
		}

		// paint the objects
		for (Tangible tangible : manager.objectList.values()) {
			if (tangible==selectedObject) {
				g2.setStroke(borderStroke);
				g2.setPaint(tangible.color.darker());
				g2.draw(tangible.geom);
			}

			// draw the objects
			g2.setStroke(normalStroke);
			g2.setPaint(tangible.color);
			g2.fill(tangible.geom);

			// draw the rotation mark
			if (tangible.isActive()) g2.setPaint(Color.black);
			else g2.setPaint(Color.lightGray);

			g2.setStroke(normalStroke);
			g2.draw(new Line2D.Float(tangible.getPixelPosition(),tangible.getPointer()));
			if ((tangible.previous!=null)  && (tangible.next!=null))
				g2.fillOval(tangible.getPixelPosition().x-3,tangible.getPixelPosition().y-3,6,6);

			// draw the object id
			g2.setPaint(Color.white);
			g2.drawString(tangible.getSymbolID()+"",tangible.getPixelPosition().x+2,tangible.getPixelPosition().y);
		}

		// draw the object name
		if (showName) {
			int boxwidth = getFontMetrics(getFont()).stringWidth(objectName)+12;
			g2.setPaint(Color.white);
			g2.fill(new Rectangle2D.Float(nameX,nameY,boxwidth,16));
			g2.setPaint(Color.blue);
			g2.draw(new Rectangle2D.Float(nameX,nameY,boxwidth,16));
			g2.drawString(objectName,nameX+6,nameY+12);
		}

	}

	public final void switchSide(Tangible old_side, int increment) {

		if ((old_side.previous!=null) && (old_side.next!=null)) {

			int x_save = old_side.getPixelPosition().x;
			int y_save = old_side.getPixelPosition().y;
			float a_save = old_side.visualAngle();

			Tangible new_side = old_side;
			if (increment>0) new_side = old_side.next;
			else  if (increment<0) new_side = old_side.previous;
			selectedObject = new_side;

			removeObject(old_side);
			manager.updateObject(old_side,-100,-100,a_save,frameTime);
			old_side.moved = false;
			manager.updateObject(new_side,x_save,y_save,a_save,frameTime);
			new_side.moved = false;
			if (vision.contains(new_side.geom.getBounds2D())) addObject(new_side);
		}
	}

	public void mouse_dragged (MouseEvent evt) {

		long currentFrameTime = System.currentTimeMillis();

		Point pt = evt.getPoint();
		int x = (int)pt.getX();
		int y = (int)pt.getY();

		if (selectedObject!=null){
			switch (evt.getModifiersEx()) {
				// translation while lifted off the table
				case InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK: {
					if (selectedObject.isActive()) {
						removeObject(selectedObject);
					}
					Point target = clampTarget(selectedObject,x+clickX,y+clickY);
					manager.updateObject(selectedObject,target.x,target.y,selectedObject.visualAngle(),frameTime);
					selectedObject.moved = false;
					break;
				}
				// translation
				case InputEvent.BUTTON1_DOWN_MASK: {
					if (manager.collision) pushMove(selectedObject,x+clickX,y+clickY);
					else {
						Point target = clampTarget(selectedObject,x+clickX,y+clickY);
						manager.updateObject(selectedObject,target.x,target.y,selectedObject.visualAngle(),frameTime);
					}
					syncMovedObjects();
					break;
				}
				// rotation
				case InputEvent.BUTTON3_DOWN_MASK: {
					if (lastY<0) break;
					int diff = lastY-y;
					if (diff>15) diff = 15;
					if (diff<-15) diff = -15;
					float newAngle = (float)(selectedObject.visualAngle()+(Math.PI/90*diff));
					if(newAngle<0) newAngle+=doublePi;
					if(newAngle>doublePi) newAngle-=doublePi;
					manager.updateObject(selectedObject,selectedObject.getPixelPosition().x,selectedObject.getPixelPosition().y,newAngle,frameTime);
					syncMovedObjects();
					break;
				}

				// switch sides
				case InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK: //mac
				case InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK: {
					if (lastY<0) break;
					int diff = lastY-y;
					if (diff>=3) switchSide(selectedObject,-1);
					if (diff<=-3) switchSide(selectedObject,1);
					break;
				}
			}

			} else if (selectedCursor!=null){
				if (table.contains(pt)) {

					Point target = pt;
					if (manager.collision) {
						Point cp = cursorPoint(selectedCursor);
						cursorPush(x,y,x-cp.x,y-cp.y);
						syncMovedObjects();
					}

					int selId = (int)selectedCursor.getSessionID();
					if (jointCursors.contains(selId)) {
						Point selPoint = cursorPoint(selectedCursor);
						int dx = target.x - selPoint.x;
						int dy = target.y - selPoint.y;

						for (int jointId : jointCursors) {
							if (jointId == selId) continue;
							TuioCursor joint_cursor = tuioServer.getTuioCursor(jointId);
							if (joint_cursor==null) continue;
							Point joint_point = cursorPoint(joint_cursor);
							updateCursor(joint_cursor, joint_point.x+dx,joint_point.y+dy);
						}
						updateCursor(selectedCursor,target.x,target.y);
					} else {
						updateCursor(selectedCursor,target.x,target.y);
					}
				} else {
					int selId = (int)selectedCursor.getSessionID();
					stickyCursors.removeElement(Integer.valueOf(selId));
					jointCursors.removeElement(Integer.valueOf(selId));
					removeCursor(selectedCursor);
					selectedCursor = null;
				}
			} else {
				if (table.contains(pt) && !hitsObject(pt.x,pt.y)) {
					selectedCursor = addCursor(x,y);
					if ((evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0) stickyCursors.addElement((int)selectedCursor.getSessionID());
				}
			}

			lastX = x;
			lastY = y;
			lastFrameTime = currentFrameTime;
	}

	public void mouse_moved(MouseEvent evt) {

		if (evt.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {

			int x = evt.getX();
			int y = evt.getY();

			for (Tangible tangible : manager.objectList.values()) {
				if (tangible.containsPoint(x,y)) {

					objectName = tangible.type.description;
					showName=true;
					nameId = (int)tangible.getSymbolID();
					nameX = x+24;
					nameY = y;

					repaint();
					return;
				}
			}

			if (showName) {
				showName = false;
				repaint();
			}
		} else if (showName)  {
			showName = false;
			repaint();
		}
	}

	public void mouse_pressed (MouseEvent evt) {

		int x = evt.getX();
		int y = evt.getY();

		// pick the topmost object (reverse paint order)
		ArrayList<Tangible> objects = new ArrayList<Tangible>(manager.objectList.values());
		for (int i=objects.size()-1;i>=0;i--) {
			Tangible tangible = objects.get(i);
			if (tangible.containsPoint(x,y)) {
				selectedObject = tangible;
				selectedObject.vx = 0;
				selectedObject.vy = 0;
				selectedCursor = null;

				clickX = tangible.getPixelPosition().x-x;
				clickY = tangible.getPixelPosition().y-y;
				lastX  = x;
				lastY  = y;
				repaint();
				return;
			}
		}

		for (TuioCursor cursor : tuioServer.getTuioCursors()) {
			Point point = cursorPoint(cursor);
			if (point.distance(x,y)<7) {

				int curId = (int)cursor.getSessionID();
				int selId = -1;
				if (selectedCursor!=null) selId = (int)selectedCursor.getSessionID();
				if (((evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0) && selId != curId) {
					stickyCursors.removeElement(Integer.valueOf(curId));
					jointCursors.removeElement(Integer.valueOf(curId));
					removeCursor(cursor);
					selectedCursor = null;
					repaint();
					return;
				} else if ((evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0) {
					if (jointCursors.contains(curId)) jointCursors.removeElement(Integer.valueOf(curId));
					else jointCursors.addElement(curId);
					repaint();
					return;
				} else {
					selectedCursor = cursor;
					selectedObject = null;
					lastX = x;
					lastY = y;
					repaint();
					return;
				}
			}
		}

		if ((evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0) return;

		if (table.contains(new Point(x, y))) {

			selectedCursor = addCursor(x,y);
			if ((evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0) stickyCursors.addElement((int)selectedCursor.getSessionID());
			repaint();
			return;
		}

		selectedObject = null;
		selectedCursor = null;
		lastX = -1;
		lastY = -1;
		repaint();
	}

	public void mouse_released (MouseEvent evt) {

		if (selectedObject!=null) {

			// activate if inside the table
			if (vision.contains(selectedObject.geom.getBounds2D())) {
				if (!selectedObject.isActive()) addObject(selectedObject);
				else if (!selectedObject.getTuioTime().equals(frameTime)) updateObject(selectedObject);
			} else if (selectedObject.isActive() || (tuioServer.getTuioObject(selectedObject.getSessionID())!=null)) {
				removeObject(selectedObject);
			}

			selectedObject = null;
		} else if (selectedCursor!=null) {

			int selId = (int)selectedCursor.getSessionID();
			if (!stickyCursors.contains(selId)) {
				jointCursors.removeElement(Integer.valueOf(selId));
				removeCursor(selectedCursor);
			} else {
				Point cp = cursorPoint(selectedCursor);
				updateCursor(selectedCursor,cp.x,cp.y);
			}

			selectedCursor = null;
		}

		repaint();
	}

	public void enablePeriodicMessages() {
		periodic = true;
	}

	public void disablePeriodicMessages() {
		periodic = false;
	}
}
