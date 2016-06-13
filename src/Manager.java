/*
	TUIO 1.1 Simulator - Manager.java
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
import java.io.*;
import javax.swing.*;

import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

public class Manager {

	public static final String slash = System.getProperty("file.separator");
	public static final String resources = "."+slash+"resources"+slash;
	private String config_file = resources+"config.xml";

	private final float doublePi = (float)(Math.PI*2);
	private final float halfPi = (float)(Math.PI/2);
	private final float negPi = (float)(Math.PI*-1);
	private final float posPi = (float)(Math.PI);

	public boolean verbose = false;
	public boolean antialiasing = true;
	public boolean collision = false;
	
	public boolean invertx = false;
	public boolean inverty = false;
	public boolean inverta = false;
	
	public Hashtable<Integer,Tangible> objectList = new Hashtable<Integer,Tangible>();
	public Hashtable<Integer,Finger> cursorList = new Hashtable<Integer,Finger>();
	public Hashtable<String,TangibleType> objectType = new Hashtable<String,TangibleType>();	
	private JFrame parent;
	
	public Manager(JFrame parent, String config) {
	
		this.parent = parent;
		if(config!=null) config_file=config;
		reset();
	}

	private void readConfig() {
		Document doc = null;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse (new File(config_file));
			
			String docType = doc.getDocumentElement().getNodeName();
			if (!docType.equalsIgnoreCase("tuio")) {
				System.out.println("error parsing configuration file");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error reading configuration file");
			return;
		}

		NodeList classNodes = doc.getElementsByTagName("class");
		for (int i=0;i<classNodes.getLength();i++) {
			try {
				Node classNode = classNodes.item(i);
				String name = ((Element)classNode).getAttribute("name");
				String shape = ((Element)classNode).getAttribute("shape");
				String color = ((Element)classNode).getAttribute("color");
				String description = ((Element)classNode).getAttribute("description");
				
				TangibleType type = new TangibleType(name,shape,color,description);
				objectType.put(type.name,type);

			} catch (Exception e) {
				System.out.println("error parsing class node");
			}
		}
		
		int session_id = -1;
		NodeList objectNodes = doc.getElementsByTagName("object");
		for (int i=0;i<objectNodes.getLength();i++) {
			try {
				Node objectNode = objectNodes.item(i);
				String typeName = ((Element)objectNode).getAttribute("class");
				String fiducialList = ((Element)objectNode).getAttribute("fiducials");
				boolean active = new Boolean(((Element)objectNode).getAttribute("active")).booleanValue();
				float xpos = Float.parseFloat(((Element)objectNode).getAttribute("xpos"));
				float ypos = Float.parseFloat(((Element)objectNode).getAttribute("ypos"));
				float angle = Float.parseFloat(((Element)objectNode).getAttribute("angle"))/360*doublePi+(float)Math.PI;


				TangibleType type = (TangibleType)(objectType.get(typeName));
  				StringTokenizer st = new StringTokenizer(fiducialList,",");
  				
  				int sides = st.countTokens();
				Tangible tangible[] = new Tangible[sides];
				int fiducial_id = Integer.parseInt(st.nextToken());
				tangible[0] = new Tangible(session_id,fiducial_id, type, active, xpos, ypos, angle);
				objectList.put(session_id,tangible[0]); 
				session_id--;
				if (sides>1){
					for (int face=1;face<sides;face++) {
						fiducial_id = Integer.parseInt(st.nextToken());
						tangible[face] = new Tangible(session_id,fiducial_id, type, false,-100, -100, angle);
						objectList.put(session_id,tangible[face]);
						session_id--;
					}
					for (int face=0;face<sides;face++) {
						if (face>0) tangible[face].previous = tangible[face-1];
						else tangible[face].previous = tangible[sides-1];
						
						if (face<sides-1) tangible[face].next = tangible[face+1];
						else tangible[face].next = tangible[0];
					}
				}
			} catch (Exception e) {
				System.out.println("error parsing object node");
				e.printStackTrace();
			}
		}
		
									
	}
	
	public final void reset() {
		cursorList.clear();
		objectList.clear();
		readConfig();
		parent.repaint();	
	}

	public final void activateObject(int old_id, int session_id) {
	
		Tangible tangible = objectList.get(old_id);
		if(!tangible.isActive()) {
			if (verbose) System.out.println("add obj "+session_id+" "+tangible.fiducial_id);

			tangible.activate(session_id);
			objectList.remove(old_id);
			objectList.put(session_id,tangible);

			parent.repaint();
		}
	}

	public final void deactivateObject(Tangible tangible) {

		if(tangible.isActive()) {
			if (verbose) System.out.println("del obj "+tangible.session_id+" "+tangible.fiducial_id);
 			tangible.deactivate();
			parent.repaint();
		}
	}

	public final void updateObject(Tangible tangible, int x, int y, float a) {
	
		Point pt =  tangible.getPosition();
		float dx = x-pt.x;
		float dy = y-pt.y;
		float dt = a - tangible.getAngle();

		if (dt < negPi) dt += doublePi;
		if (dt > posPi) dt -= doublePi;
		
		if ((dx!=0) || (dy!=0)) tangible.translate(dx,dy);
		if (dt!=0) tangible.rotate(dt);
		parent.repaint();
	}
	
	public final Finger addCursor(int s_id, int x, int y) {
		
		Finger cursor = new Finger(s_id,x,y);
		cursorList.put(s_id, cursor);
		parent.repaint();
		return cursor;
	}

	public final void updateCursor(Finger cursor, int x, int y) {

		cursor.update(x,y);
		parent.repaint();
	}

	public final Finger getCursor(int s_id) {
		return cursorList.get(s_id);
	}
	
	public final void terminateCursor(Finger cursor) {
		cursorList.remove(cursor.session_id);
		parent.repaint();
	}
 

}
