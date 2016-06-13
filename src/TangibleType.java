/*
	TUIO 1.1 Simulator - TangibleType.java
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

public class TangibleType {

	public String name;
	public String shape;
	public String hexcolor;
	public String description;

	public Point2D center;
	public Color color;

	public static final float size = TuioSimulator.height/12.5f;

	public TangibleType(String name, String shape, String hexcolor, String description) {

 			this.name = name;
			this.shape = shape;
			this.hexcolor = hexcolor;
			this.description = description;
	}

	public Shape getShape() {


		int R = Integer.parseInt(hexcolor.substring(0,2),16);
		int G = Integer.parseInt(hexcolor.substring(2,4),16);
		int B = Integer.parseInt(hexcolor.substring(4,6),16);
		color = new Color(R,G,B);

		Shape geom;
		if (shape.equals("circle")) {
			geom = new Ellipse2D.Float(-0.05f*size,-0.05f*size,1.1f*size,1.1f*size);
			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("square")) {
			geom = new Rectangle2D.Float(0,0,size,size);
			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("roundsquare")) {
			geom = new RoundRectangle2D.Float(0,0,size,size,size/2,size/2);
			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("triangle")) {
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,2);
			float height = (float)(Math.sqrt(3)*size/2);
			path.moveTo(size/2, size-height);
			path.lineTo(size, size);
			path.lineTo(0, size);
			path.closePath();
			geom = path;
			center = new Point2D.Float(size/2,size-height/2);
		} else if (shape.equals("star")) {
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,2);
			path.moveTo(size/2, 0);
				path.lineTo(7*size/10, 3*size/10);
				path.lineTo(size, size/2);
				path.lineTo(7*size/10, 7*size/10);
				path.lineTo(size/2, size);
				path.lineTo(3*size/10, 7*size/10);
				path.lineTo(0, size/2);
				path.lineTo(3*size/10, 3*size/10);
			path.closePath();

			AffineTransform  trans = AffineTransform.getRotateInstance(Math.PI/4,size/2,size/2);
			Shape shape = trans.createTransformedShape(path);

			Area area = new Area(path);
			area.add(new Area(shape));
			
			trans = AffineTransform.getScaleInstance(1.2,1.2);
			shape = trans.createTransformedShape(area);
			trans = AffineTransform.getTranslateInstance(-0.1*size,-0.1*size);
			shape = trans.createTransformedShape(shape);
			
			geom = shape;

			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("octagon")) {
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,2);
			path.moveTo(size/4, 0);
                        path.lineTo(3*size/4, 0);
                        path.lineTo(size, size/4);
                        path.lineTo(size, 3*size/4);
                        path.lineTo(3*size/4, size);
                        path.lineTo(size/4, size);
                        path.lineTo(0, 3*size/4);
                        path.lineTo(0, size/4);
                        path.closePath();
                        geom = path;
			path.closePath();
			geom = path;
			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("ellipse")) {
			geom = new Ellipse2D.Float(0,0,3*size/4,size);
			center = new Point2D.Float(3*size/8,size/2);
		} else if (shape.equals("cross")) {
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,2);
			path.moveTo(size/4, 0);
                        path.lineTo(3*size/4, 0);
                        path.lineTo(3*size/4, size/4);
                        path.lineTo(size, size/4);
                        path.lineTo(size, 3*size/4);
                        path.lineTo(3*size/4, 3*size/4);
                        path.lineTo(3*size/4, size);
                        path.lineTo(size/4, size);
                        path.lineTo(size/4, 3*size/4);
                        path.lineTo(0, 3*size/4);
                        path.lineTo(0, size/4);
			path.lineTo(size/4, size/4);
 			path.closePath();
			geom = path;
			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("pentagon")) {
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,2);
			path.moveTo(size/2, size);

			float x,y,a;
			for (int i=1;i<5;i++) {
				a = (float)((2*Math.PI/5)*i);
				x = (float)(size/2 + size/2*Math.sin(a));
				y = (float)(size/2 + size/2*Math.cos(a));
				path.lineTo(x, y);
			}
			path.closePath();
			
			AffineTransform trans = AffineTransform.getScaleInstance(1.1,1.1);
			Shape shape = trans.createTransformedShape(path);
			trans = AffineTransform.getTranslateInstance(-0.05*size,-0.05*size);
			shape = trans.createTransformedShape(shape);
			
			geom = shape;
			center = new Point2D.Float(size/2,size/2);
		} else if (shape.equals("hexagon")) {
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,2);
			path.moveTo(size/2, size);

			float x,y,a;
			for (int i=1;i<6;i++) {
				a = (float)((2*Math.PI/6)*i);
				x = (float)(size/2 + size/2*Math.sin(a));
				y = (float)(size/2 + size/2*Math.cos(a));
				path.lineTo(x, y);
			}
			path.closePath();
			geom = path;
			center = new Point2D.Float(size/2,size/2);
		}  else {
			geom = new Ellipse2D.Float(0,0,size,size);
			center = new Point2D.Float(size/2,size/2);
		}

		return geom;
	}
}
