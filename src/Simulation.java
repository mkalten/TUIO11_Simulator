/*
    TUIO 1.1 Simulator - Simulation.java
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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import com.illposed.osc.*;

public class Simulation extends JPanel implements Runnable {

	private Manager manager;
	private OSCPortOut oscPort;

	private int currentFrame = 0;	
	private long lastFrameTime = -1;
	private int session_id = -1;

	private final float halfPi = (float)(Math.PI/2);
	private final float doublePi = (float)(Math.PI*2);

	Shape vision, vision_rectangle, vision_circle;
	Shape table_border,table,table_rectangle,table_circle;
	private Stroke normalStroke = new BasicStroke(2.0f);	
	private Stroke borderStroke = new BasicStroke(2.0f);
	private Color borderColor = new Color(0,200,0);
	private Stroke gestureStroke = new BasicStroke(1.0f);
	
	Tangible selectedObject = null;
	Finger selectedCursor = null;
	int lastX  = -1;
	int lastY  = -1;
	int clickX = 0;
	int clickY = 0;

	Vector<Integer> stickyCursors = new Vector<Integer>();
	Vector<Integer> jointCursors = new Vector<Integer>();
	
	boolean showName = false;
	int nameId, nameX, nameY;
	String objectName = "";

	private boolean running = false;
	
	int window_width = TuioSimulator.width;
	int window_height = TuioSimulator.height;
	int table_width = (int)(TuioSimulator.width/1.25);
	int table_height = (int)(TuioSimulator.height/1.25);
	int border_width = (int)(window_width-table_width)/2;
	int border_height = (int)(window_height-table_height)/2;

	private Robot robot = null;
	String sourceString;
	
	MouseEvent lastPressedEvent;
	
	public Simulation(Manager manager, String host, int port) {
		super(true);
		this.manager=manager;		

		try { oscPort = new OSCPortOut(java.net.InetAddress.getByName(host),port); }
		catch (Exception e) { oscPort = null; }
		

		try { sourceString = "tsim@"+java.net.InetAddress.getLocalHost().getHostAddress(); }
		catch (Exception e) { sourceString = "tsim"; }

		try { robot = new Robot(); }
		catch (Exception e) {}

		//String  os = System.getProperty("os.name");
		//if (os.equals("Mac OS X")) RepaintManager.currentManager(this).setDoubleBufferingEnabled(false);

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
		
		Enumeration<Tangible> objectList = manager.objectList.elements();
			while (objectList.hasMoreElements()) {
				Tangible tangible = objectList.nextElement();
				if (vision.contains(tangible.geom.getBounds2D())) {
					if (!tangible.isActive()) {
						session_id++;
						manager.activateObject(tangible.session_id,session_id);
						setMessageBundle(tangible);
					}
				} else {
					if (tangible.isActive()) {
						manager.deactivateObject(tangible);
						setMessageBundle(tangible);
					}
				}
		}
	}

	private void sendOSC(OSCPacket packet) {
		try { oscPort.send(packet); }
		catch (java.io.IOException e) {}
	}

	private OSCMessage setMessage(Tangible tangible) {		
		
		float xpos = (tangible.getPosition().x-border_width)/(float)table_width;
		if (manager.invertx) xpos = 1 - xpos;
		float ypos = (tangible.getPosition().y-border_height)/(float)table_height;
		if (manager.inverty) ypos = 1 - ypos;
		float angle = tangible.getAngle() - (float)Math.PI;
		if (angle<0) angle+=doublePi;
		if (manager.inverta) angle = doublePi - angle;
		OSCMessage setMessage = new OSCMessage("/tuio/2Dobj");
		setMessage.addArgument("set");
		setMessage.addArgument(tangible.session_id);
		setMessage.addArgument(tangible.fiducial_id);
		setMessage.addArgument(xpos);
		setMessage.addArgument(ypos);
		setMessage.addArgument(angle);
		setMessage.addArgument(tangible.xspeed);
		setMessage.addArgument(tangible.yspeed);
		setMessage.addArgument(tangible.rspeed);
		setMessage.addArgument(tangible.maccel);
		setMessage.addArgument(tangible.raccel);
		return setMessage;
	}

	private void cursorDelete() {
		
		OSCBundle cursorBundle = new OSCBundle();
		OSCMessage sourceMessage = new OSCMessage("/tuio/2Dcur");
		sourceMessage.addArgument("source");
		sourceMessage.addArgument(sourceString);
		
		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dcur");
		aliveMessage.addArgument("alive");
		Enumeration<Integer> cursorList = manager.cursorList.keys();
		while (cursorList.hasMoreElements()) {
			Integer s_id = cursorList.nextElement();
			aliveMessage.addArgument(s_id);
		}

		currentFrame++;
		OSCMessage frameMessage = new OSCMessage("/tuio/2Dcur");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(currentFrame);
		
		cursorBundle.addPacket(sourceMessage);
		cursorBundle.addPacket(aliveMessage);
		cursorBundle.addPacket(frameMessage);

		sendOSC(cursorBundle);
	}

	private void cursorMessage() {

		OSCBundle cursorBundle = new OSCBundle();
		OSCMessage sourceMessage = new OSCMessage("/tuio/2Dcur");
		sourceMessage.addArgument("source");
		sourceMessage.addArgument(sourceString);
		
		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dcur");
		aliveMessage.addArgument("alive");

		Enumeration<Integer> cursorList = manager.cursorList.keys();
		while (cursorList.hasMoreElements()) {
			Integer s_id = cursorList.nextElement();
			aliveMessage.addArgument(s_id);
		}

		Finger cursor = selectedCursor;
		Point point = cursor.getPosition();
		float xpos = (point.x-border_width)/(float)table_width;
		if (manager.invertx) xpos = 1 - xpos;
		float ypos = (point.y-border_height)/(float)table_height;
		if (manager.inverty) ypos = 1 - ypos;
		OSCMessage setMessage = new OSCMessage("/tuio/2Dcur");
		setMessage.addArgument("set");
		setMessage.addArgument(cursor.session_id);
		setMessage.addArgument(xpos);
		setMessage.addArgument(ypos);
		setMessage.addArgument(cursor.xspeed);
		setMessage.addArgument(cursor.yspeed);
		setMessage.addArgument(cursor.maccel);

		currentFrame++;
		OSCMessage frameMessage = new OSCMessage("/tuio/2Dcur");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(currentFrame);
		
		cursorBundle.addPacket(sourceMessage);
		cursorBundle.addPacket(aliveMessage);
		cursorBundle.addPacket(setMessage);
		cursorBundle.addPacket(frameMessage);

		if (manager.verbose) {
			System.out.println("set cur "+cursor.session_id+" "+xpos+" "+ypos+" "+cursor.xspeed+" "+cursor.yspeed+" "+cursor.maccel);
		}
		sendOSC(cursorBundle);
	}

	private void setMessageBundle(Tangible tangible) {

		OSCBundle oscBundle = new OSCBundle();
		OSCMessage sourceMessage = new OSCMessage("/tuio/2Dobj");
		sourceMessage.addArgument("source");
		sourceMessage.addArgument(sourceString);
		
		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dobj");
		aliveMessage.addArgument("alive");

		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible test = objectList.nextElement();
			if (test.isActive())
				aliveMessage.addArgument(test.session_id);
		}

		OSCMessage setMessage = setMessage(tangible);
		if (manager.verbose) {
			float xpos = (tangible.getPosition().x-border_width)/(float)table_width;
			if (manager.invertx) xpos = 1 - xpos;
			float ypos = (tangible.getPosition().y-border_height)/(float)table_height;
			if (manager.inverty) ypos = 1 - ypos;
			float angle = tangible.getAngle() - (float)Math.PI;
			if (angle<0) angle+=doublePi;
			if (manager.inverta) angle = doublePi - angle;

			System.out.println("set obj "+tangible.session_id+" "+tangible.fiducial_id+" "+xpos+" "+ypos+" "+angle+" "+tangible.xspeed+" "+tangible.yspeed+" "+tangible.rspeed+" "+tangible.maccel+" "+tangible.raccel);
		}
		currentFrame++;
		OSCMessage frameMessage = new OSCMessage("/tuio/2Dobj");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(currentFrame);

		oscBundle.addPacket(sourceMessage);
		oscBundle.addPacket(aliveMessage);
		oscBundle.addPacket(setMessage);
		oscBundle.addPacket(frameMessage);
		
		sendOSC(oscBundle);
	}

	public void aliveMessage() {

		OSCBundle oscBundle = new OSCBundle();
		OSCMessage sourceMessage = new OSCMessage("/tuio/2Dobj");
		sourceMessage.addArgument("source");
		sourceMessage.addArgument(sourceString);
		
		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dobj");
		aliveMessage.addArgument("alive");

		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible tangible = objectList.nextElement();
			if (tangible.isActive())
				aliveMessage.addArgument(tangible.session_id);
		}
		
		currentFrame++;
		OSCMessage frameMessage = new OSCMessage("/tuio/2Dobj");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(currentFrame);

		oscBundle.addPacket(sourceMessage);
		oscBundle.addPacket(aliveMessage);
		oscBundle.addPacket(frameMessage);
		
		sendOSC(oscBundle);
	}

	private void completeCursorMessage() {
		
		Vector<OSCMessage> messageList = new Vector<OSCMessage>(manager.objectList.size());
		
		OSCMessage sourceMessage = new OSCMessage("/tuio/2Dcur");
		sourceMessage.addArgument("source");
		sourceMessage.addArgument(sourceString);
		
		OSCMessage frameMessage = new OSCMessage("/tuio/2Dcur");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(-1);
		
		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dcur");
		aliveMessage.addArgument("alive");
		
		Enumeration<Integer> cursorList = manager.cursorList.keys();
		while (cursorList.hasMoreElements()) {
			Integer s_id = cursorList.nextElement();
			aliveMessage.addArgument(s_id);

			Finger cursor = manager.cursorList.get(s_id);
			Point point = cursor.getPosition();
					
			float xpos = (point.x-border_width)/(float)table_width;
			if (manager.invertx) xpos = 1 - xpos;
			float ypos = (point.y-border_height)/(float)table_height;
			if (manager.inverty) ypos = 1 - ypos;

			OSCMessage setMessage = new OSCMessage("/tuio/2Dcur");
			setMessage.addArgument("set");
			setMessage.addArgument(s_id);
			setMessage.addArgument(xpos);
			setMessage.addArgument(ypos);
			setMessage.addArgument(cursor.xspeed);
			setMessage.addArgument(cursor.yspeed);
			setMessage.addArgument(cursor.maccel);
			messageList.addElement(setMessage);
		}
		
		int i;
		for (i=0;i<(messageList.size()/10);i++) {
			OSCBundle oscBundle = new OSCBundle();
			oscBundle.addPacket(sourceMessage);
			oscBundle.addPacket(aliveMessage);
			
			for (int j=0;j<10;j++)
				oscBundle.addPacket((OSCPacket)messageList.elementAt(i*10+j));
			
			oscBundle.addPacket(frameMessage);
			sendOSC(oscBundle);
		} 
		
		if ((messageList.size()%10!=0) || (messageList.size()==0)) {
			OSCBundle oscBundle = new OSCBundle();
			oscBundle.addPacket(sourceMessage);
			oscBundle.addPacket(aliveMessage);
			
			for (int j=0;j<messageList.size()%10;j++)
				oscBundle.addPacket((OSCPacket)messageList.elementAt(i*10+j));	
			
			oscBundle.addPacket(frameMessage);
			sendOSC(oscBundle);
		}
	}
	
	private void completeObjectMessage() {

		Vector<OSCMessage> messageList = new Vector<OSCMessage>(manager.objectList.size());

		OSCMessage sourceMessage = new OSCMessage("/tuio/2Dobj");
		sourceMessage.addArgument("source");
		sourceMessage.addArgument(sourceString);
		
		OSCMessage frameMessage = new OSCMessage("/tuio/2Dobj");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(-1);

		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dobj");
		aliveMessage.addArgument("alive");

		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible tangible = objectList.nextElement();
			try {
				if (tangible.isActive()) {
					messageList.addElement(setMessage(tangible));
					aliveMessage.addArgument(tangible.session_id);
				}
			} catch (Exception e) { return; }
		}

		int i;
		for (i=0;i<(messageList.size()/10);i++) {
			OSCBundle oscBundle = new OSCBundle();
			oscBundle.addPacket(sourceMessage);

			oscBundle.addPacket(aliveMessage);

			for (int j=0;j<10;j++)
				oscBundle.addPacket((OSCPacket)messageList.elementAt(i*10+j));
	
			oscBundle.addPacket(frameMessage);
			sendOSC(oscBundle);
		} 

		if ((messageList.size()%10!=0) || (messageList.size()==0)) {
			OSCBundle oscBundle = new OSCBundle();
			oscBundle.addPacket(sourceMessage);

			oscBundle.addPacket(aliveMessage);
	
			for (int j=0;j<messageList.size()%10;j++)
				oscBundle.addPacket((OSCPacket)messageList.elementAt(i*10+j));	
	
			oscBundle.addPacket(frameMessage);
			sendOSC(oscBundle);
		}
	}

	public void quit() {
		reset();
		running = false;
	}

	public void reset() {
		session_id = -1;
		stickyCursors.clear();
		jointCursors.clear();
		
		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible tangible = objectList.nextElement();
			if (vision.contains(tangible.geom.getBounds2D())) {
				if (!tangible.isActive()) {
					session_id++;
					manager.activateObject(tangible.session_id,session_id);
				}
			} else {
				if (tangible.isActive()) {
					manager.deactivateObject(tangible);
				}
			}
		}
		
		lastFrameTime = -1;
		
		OSCBundle objBundle = new OSCBundle();
		OSCMessage osrcMessage = new OSCMessage("/tuio/2Dobj");
		osrcMessage.addArgument("source");
		osrcMessage.addArgument(sourceString);
		
		OSCMessage aliveMessage = new OSCMessage("/tuio/2Dobj");
		aliveMessage.addArgument("alive");

		OSCMessage frameMessage = new OSCMessage("/tuio/2Dobj");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(-1);

		objBundle.addPacket(osrcMessage);
		objBundle.addPacket(aliveMessage);
		objBundle.addPacket(frameMessage);		
		sendOSC(objBundle);
		
		OSCBundle curBundle = new OSCBundle();
		OSCMessage csrcMessage = new OSCMessage("/tuio/2Dcur");
		csrcMessage.addArgument("source");
		csrcMessage.addArgument(sourceString);
		
		aliveMessage = new OSCMessage("/tuio/2Dcur");
		aliveMessage.addArgument("alive");
		
		frameMessage = new OSCMessage("/tuio/2Dcur");
		frameMessage.addArgument("fseq");
		frameMessage.addArgument(-1);
		
		curBundle.addPacket(csrcMessage);
		curBundle.addPacket(aliveMessage);
		curBundle.addPacket(frameMessage);		
		sendOSC(curBundle);
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
		Enumeration<Finger> cursorList = manager.cursorList.elements();
		while (cursorList.hasMoreElements()) {
			Finger cursor = cursorList.nextElement();
			Vector<Point> gesture = cursor.getPath();
			if (gesture.size()>0) {
				g2.setPaint(Color.blue);
				g2.setStroke(gestureStroke);
				Point start = gesture.elementAt(0);
				for (int i=0;i<gesture.size();i++) {
					Point end  = gesture.elementAt(i);
					g2.draw(new Line2D.Double(start.getX(),start.getY(),end.getX(),end.getY()));
					start = end;
				}
				if (jointCursors.contains(cursor.session_id)) g2.setPaint(Color.darkGray);
				else g2.setPaint(Color.lightGray);
				g2.fill(new Ellipse2D.Double(start.getX()-5,start.getY()-5,10,10));
			}
		}

		// paint the objects
		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible tangible = objectList.nextElement();
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
			g2.draw(new Line2D.Float(tangible.getPosition(),tangible.getPointer()));
			if ((tangible.previous!=null)  && (tangible.next!=null))
				g2.fillOval(tangible.getPosition().x-3,tangible.getPosition().y-3,6,6);

			// draw the object id
			g2.setPaint(Color.white);
			g2.drawString(tangible.fiducial_id+"",tangible.getPosition().x+2,tangible.getPosition().y);
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
							
			int x_save = old_side.getPosition().x;
			int y_save = old_side.getPosition().y;
			float a_save =  old_side.getAngle();

			Tangible new_side = old_side;
			if (increment>0) new_side = old_side.next;
			else  if (increment<0) new_side = old_side.previous;	
			selectedObject = new_side;

			manager.deactivateObject(old_side);
			manager.updateObject(old_side,-100,-100,a_save);
			manager.updateObject(selectedObject,x_save,y_save,a_save);
			aliveMessage();
		}
	}

	
	/*private boolean pushObject(Tangible pushed, int x, int y) {

		pushed.pushed = true;

		int dx = (x+clickX)-pushed.getPosition().x;
		int dy = (y+clickY)-pushed.getPosition().y;
		AffineTransform trans = AffineTransform.getTranslateInstance(dx,dy);
		Shape shape = trans.createTransformedShape(pushed.geom);

		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible tangible = objectList.nextElement();
			if ((tangible!=pushed) && (tangible.containsArea(new Area(shape))) && (!tangible.pushed)) {
				dx = tangible.getPosition().x - pushed.getPosition().x;
				dy = tangible.getPosition().y - pushed.getPosition().y;

				if(!table_border.contains(new Point(dx+x+clickX,dy+y+clickY))) {
					if (robot!=null) robot.mouseMove((int)getLocationOnScreen().getX()+lastX,(int)getLocationOnScreen().getY()+lastY);
					pushed.pushed=false;
					return false;
				}	
				
				if (!pushObject(tangible,dx+x,dy+y)) { tangible.pushed=false; return false; }
				break;
			}
		}		
		
		//move the object
		manager.updateObject(pushed,x+clickX,y+clickY,pushed.getAngle());
		pushed.pushed = false;
		// activate
		if (vision.contains(pushed.geom.getBounds2D())) {
			if (!pushed.isActive()) {
				session_id++;
				manager.activateObject(pushed.session_id,session_id);
			}
			setMessageBundle(pushed);
			//aliveMessage();
		} else { 
			manager.deactivateObject(pushed);
			aliveMessage();
		}
		
		return true;
	}*/
	
	public void mouse_dragged (MouseEvent evt) {

		long currentFrameTime = System.currentTimeMillis();
		long dt = currentFrameTime - lastFrameTime;
		if (dt<5) return;

		Point pt = evt.getPoint();
		int x = (int)pt.getX();
		int y = (int)pt.getY();
		
		if (selectedObject!=null){
			//System.out.println(evt.getModifiers());
			switch (evt.getModifiers()) {
				// translation
				case 17: {
					if (manager.collision) {
						if(!table_border.contains(selectedObject.getPosition())) {
							if (robot!=null) robot.mouseMove((int)getLocationOnScreen().getX()+lastX,(int)getLocationOnScreen().getY()+lastY);
							return;
						}
					}

					// deactivate
					if (selectedObject.isActive()) {
						manager.deactivateObject(selectedObject);
						aliveMessage();
					}
					manager.updateObject(selectedObject,x+clickX,y+clickY,selectedObject.getAngle());

					break;
				}
				case 16: {

					if(manager.collision) {
						if(!table_border.contains(evt.getPoint())) {
							if (robot!=null) robot.mouseMove((int)getLocationOnScreen().getX()+lastX,(int)getLocationOnScreen().getY()+lastY);
							lastFrameTime = currentFrameTime;
							return;
						}

						int dx = (x+clickX)-selectedObject.getPosition().x;
						int dy = (y+clickY)-selectedObject.getPosition().y;
						AffineTransform trans = AffineTransform.getTranslateInstance(dx,dy);
						Shape shape = trans.createTransformedShape(selectedObject.geom);

						Enumeration<Tangible> objectList = manager.objectList.elements();
						while (objectList.hasMoreElements()) {
							Tangible tangible = objectList.nextElement();
							if ((tangible!=selectedObject) && (tangible.containsArea(new Area(shape)))) {
								if (robot!=null) robot.mouseMove((int)getLocationOnScreen().getX()+lastX,(int)getLocationOnScreen().getY()+lastY);
								lastFrameTime = currentFrameTime;
								return;
															
								/*dx = tangible.getPosition().x - selectedObject.getPosition().x;
								dy = tangible.getPosition().y - selectedObject.getPosition().y;
								
								if((Math.abs(lastX-x)>selectedObject.type.size) || (Math.abs(lastY-y)>selectedObject.type.size) || (!table_border.contains(new Point(dx+x+clickX,dy+y+clickY)))) {
									if (robot!=null) robot.mouseMove((int)getLocationOnScreen().getX()+lastX,(int)getLocationOnScreen().getY()+lastY);
									return;
								}								
								
								if (!pushObject(tangible,dx+x,dy+y)) return;
								break;*/
							}
						}
						
					}
					
					//move the object
					manager.updateObject(selectedObject,x+clickX,y+clickY,selectedObject.getAngle());

					// activate
					if (vision.contains(selectedObject.geom.getBounds2D())) {
						if (!selectedObject.isActive()) {
							session_id++;
							manager.activateObject(selectedObject.session_id,session_id);
						}
						setMessageBundle(selectedObject);
						//aliveMessage();
					} else if (selectedObject.isActive()) {
						manager.deactivateObject(selectedObject);
						aliveMessage();
					}
					break;
				}
				// rotation
				case 4: {
					if (lastY<0) break;
					int diff = lastY-y;
					if (diff>15) diff = 15;
					if (diff<-15) diff = -15;
					float newAngle = (float)(selectedObject.getAngle()+(Math.PI/90*diff));
					if(newAngle<0) newAngle+=doublePi;
					if(newAngle>doublePi) newAngle-=doublePi;
					manager.updateObject(selectedObject,selectedObject.getPosition().x,selectedObject.getPosition().y,newAngle);
					if (selectedObject.isActive())
						setMessageBundle(selectedObject);
					break;
				}
			
				// switch sides
				case 21: //mac
				case 5: {
					if (lastY<0) break;
					int diff = lastY-y;
					if (diff>=3) switchSide(selectedObject,-1);
					if (diff<=-3) switchSide(selectedObject,1);
					break;
				}
			}
			
			} else if (selectedCursor!=null){
				if (table.contains(pt)) {
					
					if(manager.collision) {
						Enumeration<Tangible> objectList = manager.objectList.elements();
						while (objectList.hasMoreElements()) {
							Tangible tangible = objectList.nextElement();
							if (tangible.containsPoint(pt.x,pt.y)) {
								if (robot!=null) robot.mouseMove((int)getLocationOnScreen().getX()+lastX,(int)getLocationOnScreen().getY()+lastY);
								lastFrameTime = currentFrameTime;
								return;
								/*int dx = tangible.getPosition().x - pt.x;
								int dy = tangible.getPosition().y - pt.y;
								clickX = x-lastX;
								clickY = y-lastY;
								if (!pushObject(tangible,x+dx,y+dy)) return;
								break;*/
							}
						}
					}					
					
					if(selectedCursor!=null) {
					if (jointCursors.contains(selectedCursor.session_id)) {
						Point selPoint = selectedCursor.getPosition();
						int dx = pt.x - selPoint.x;
						int dy = pt.y - selPoint.y;
					
						Enumeration<Integer> joints = jointCursors.elements();
						while (joints.hasMoreElements()) {
							int jointId = joints.nextElement();
							if (jointId == selectedCursor.session_id) continue;
							Finger joint_cursor = manager.getCursor(jointId);
							Point joint_point = joint_cursor.getPosition();
							manager.updateCursor(joint_cursor, joint_point.x+dx,joint_point.y+dy);
						}
						manager.updateCursor(selectedCursor,pt.x,pt.y);
						completeCursorMessage();
					} else {
						manager.updateCursor(selectedCursor,pt.x,pt.y);
						cursorMessage();
					} }
				} else {
					selectedCursor.stop();
					cursorMessage();
					if (manager.verbose) System.out.println("del cur "+selectedCursor.session_id);
					if (stickyCursors.contains(selectedCursor.session_id)) stickyCursors.removeElement(selectedCursor.session_id);
					if (jointCursors.contains(selectedCursor.session_id)) jointCursors.removeElement(selectedCursor.session_id);
					manager.terminateCursor(selectedCursor);
					cursorDelete();
					selectedCursor = null;
				}
			} else {
				if (table.contains(pt)) {
					
					 boolean insideObject = false;
					 Enumeration<Tangible> objectList = manager.objectList.elements();
					 while (objectList.hasMoreElements()) {
						 Tangible tangible = objectList.nextElement();
						 if (tangible.containsPoint(pt.x,pt.y)) {
							 insideObject =true;
							 break;
						 }
					 }
					
					if (!insideObject) {
						session_id++;
						if (manager.verbose) System.out.println("add cur "+session_id);
						selectedCursor = manager.addCursor(session_id,x,y);
						cursorMessage();
						if ((evt.getModifiers() & InputEvent.SHIFT_MASK) > 0) stickyCursors.addElement(selectedCursor.session_id);
					}
				}
			}

			lastX = x;
			lastY = y;			
			lastFrameTime = currentFrameTime;
	}

	public void mouse_moved(MouseEvent evt) {

		if (evt.getModifiers()==2) {

			int x = evt.getX();
			int y = evt.getY();
				
			Enumeration<Tangible> objectList = manager.objectList.elements();
			while (objectList.hasMoreElements()) {
				Tangible tangible = objectList.nextElement();
				if (tangible.containsPoint(x,y)) {
	
					objectName = tangible.type.description;
					showName=true;
					nameId = tangible.fiducial_id;
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
				
		Enumeration<Tangible> objectList = manager.objectList.elements();
		while (objectList.hasMoreElements()) {
			Tangible tangible = objectList.nextElement();
			if (tangible.containsPoint(x,y)) {
				selectedObject = tangible;
				selectedCursor = null;
			
				clickX = tangible.getPosition().x-x;
				clickY = tangible.getPosition().y-y;
				lastX  = x;
				lastY  = y;
				repaint();
				return;
			}
		}

		Enumeration<Finger> cursorList = manager.cursorList.elements();
		while (cursorList.hasMoreElements()) {
			Finger cursor = cursorList.nextElement();
			Point point = cursor.getPosition();
			if (point.distance(x,y)<7) {

				int selCur = -1;
				if (selectedCursor!=null) selCur = selectedCursor.session_id;
				if (((evt.getModifiers() & InputEvent.SHIFT_MASK) > 0) && selCur != cursor.session_id) {
					if (manager.verbose) System.out.println("del cur "+cursor.session_id);
					stickyCursors.removeElement(cursor.session_id);
					if (jointCursors.contains(cursor.session_id)) jointCursors.removeElement(cursor.session_id);
					manager.terminateCursor(cursor);
					cursorDelete();
					selectedCursor = null;
					repaint();
					return;
				} else if ((evt.getModifiers() & InputEvent.CTRL_MASK) > 0) {
					if (jointCursors.contains(cursor.session_id)) jointCursors.removeElement(cursor.session_id);
					else jointCursors.addElement(cursor.session_id);
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
			
		if ((evt.getModifiers() & InputEvent.CTRL_MASK) > 0) return;
		
		if (table.contains(new Point(x, y))) {
			
			session_id++;
			if (manager.verbose) System.out.println("add cur "+session_id);
			selectedCursor = manager.addCursor(session_id,x,y);
			cursorMessage();
			if ((evt.getModifiers() & InputEvent.SHIFT_MASK) > 0) stickyCursors.addElement(selectedCursor.session_id);
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
				if (!selectedObject.isActive()) {
					session_id++;
					manager.activateObject(selectedObject.session_id,session_id);
				} else selectedObject.stop();
				setMessageBundle(selectedObject);
			} else if (selectedObject.isActive()) {
				manager.deactivateObject(selectedObject);
				aliveMessage();
			}

			selectedObject = null;
		} else if ( (selectedCursor!=null) )  {
			
			if (!stickyCursors.contains(selectedCursor.session_id)) {
				selectedCursor.stop();
				cursorMessage();
				if (manager.verbose) System.out.println("del cur "+ selectedCursor.session_id);
				if (jointCursors.contains(selectedCursor.session_id)) jointCursors.removeElement(selectedCursor.session_id);
				manager.terminateCursor(selectedCursor);
				cursorDelete();
			} else {
				selectedCursor.stop();
				cursorMessage();
			}
			
			selectedCursor = null;
		}
		
		repaint();
	}

	public void enablePeriodicMessages() {
		if (!running) {
			running = true;
			new Thread(this).start();
		}

	}


	public void disablePeriodicMessages() {
		running = false;
	}

	// send table state every second
	public void run() {
		running = true;
		while(running) {
			try { Thread.sleep(1000); }
			catch (Exception e) {}

			long currentFrameTime = System.currentTimeMillis();
                	long dt = currentFrameTime - lastFrameTime;
                	if (dt>1000) {
				completeObjectMessage();
				completeCursorMessage();
			}
		}
	}
}
