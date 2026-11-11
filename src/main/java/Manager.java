/*
	TUIO 1.1 Simulator - Manager.java
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
import java.util.*;
import java.io.*;

import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import TUIO.TuioTime;

public class Manager {

	public static final String slash = System.getProperty("file.separator");
	public static final String resources = "."+slash+"resources"+slash;
	private String config_file = resources+"config.xml";

	private final float doublePi = (float)(Math.PI*2);

	public boolean antialiasing = true;
	public boolean collision = true;

	public LinkedHashMap<Integer,Tangible> objectList = new LinkedHashMap<Integer,Tangible>();
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
				boolean active = Boolean.parseBoolean(((Element)objectNode).getAttribute("active"));
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
		objectList.clear();
		objectType.clear();
		readConfig();
		parent.repaint();
	}

	public final void activateObject(int old_id, int session_id) {

		Tangible tangible = objectList.get(old_id);
		if (tangible==null) return;
		tangible.activate();
		objectList.remove(old_id);
		objectList.put(session_id,tangible);
		parent.repaint();
	}

	public final void deactivateObject(Tangible tangible) {

		tangible.deactivate();
		parent.repaint();
	}

	public final void updateObject(Tangible tangible, int x, int y, float a, TuioTime ttime) {

		tangible.moveTo(ttime,x,y,a);
		parent.repaint();
	}
}
