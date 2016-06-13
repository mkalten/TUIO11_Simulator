/*
	TUIO 1.1 Simulator - HelpBrowser.java
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
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class HelpBrowser extends JFrame implements HyperlinkListener {

	private JScrollPane scrollPane;
	private JEditorPane helpPane;
	private String helpIndex = "file:resources/index.html";

	public HelpBrowser() {
		
		try { helpPane = new JEditorPane(helpIndex); }
		catch (Exception e) { helpPane = new JEditorPane("text/html","<h1>error loading documentation ...</h1>"); }
		
		helpPane.setEditable(false);
		helpPane.addHyperlinkListener(this);

		scrollPane = new JScrollPane(helpPane);
		scrollPane.setViewportBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, Color.white));
		getContentPane().add(scrollPane);
		
		setTitle("reacTIVision manual");
		setBackground(Color.white);
	}

	public void reset() {
		try {  helpPane.setPage(helpIndex); }
		catch (Exception e) { helpPane.setText("<h1>error loading documentation ...</h1>"); }
		setSize(640,480);
		setVisible(true);
	}
		
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try { helpPane.setPage(e.getURL()); }
			catch (Throwable t) { helpPane.setText("<h1>error loading page ...</h1>"); }
		}
	}

}
