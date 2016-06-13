/*
    TUIO 1.1 Simulator - TuioSimulator.java
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
import java.awt.event.*;
import javax.swing.*;

public class TuioSimulator {
	
	public static int width = 800;
	public static int height = 600;
	
	public static void main(String[] argv) {
	
		String host = "127.0.0.1";
		int port = 3333;
		String config=null;
		
		for (int i=0;i<argv.length;i++) {
			if (argv[i].equalsIgnoreCase("-host")) {
				try { host = argv[i+1]; } catch (Exception e) {}
				i++;
			} else if (argv[i].equalsIgnoreCase("-port")) {
				try { port = Integer.parseInt(argv[i+1]); } catch (Exception e) {}
				i++;
			} else if (argv[i].equalsIgnoreCase("-config")) {
				try { config = argv[i+1]; } catch (Exception e) {}
				i++;
			} else {
				System.out.println("TuioSimulator options:");
				System.out.println("\t-host\ttarget IP");
				System.out.println("\t-port\ttarget port");
				System.out.println("\t-config\tconfig file");
				System.exit(0);
			}
		}

		System.out.println("sending TUIO messages to "+host+":"+port);

		JFrame app = new JFrame();
		app.setTitle("TUIO Simulator");

		final Manager manager = new Manager(app,config);
		final Simulation simulation = new Simulation(manager,host,port);
		//Thread simulationThread = new Thread(simulation);
		//simulationThread.start();

		app.getContentPane().add(simulation);

		app.addWindowListener( new WindowAdapter() { 
			public void windowClosing(WindowEvent evt) {	
				simulation.reset();
				System.exit(0);
			} 
		});

		JMenuBar menubar = new JMenuBar();
		JMenu optionMenu = new JMenu("Options");
		final JMenuItem resetItem = new JMenuItem("Reset");
		resetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final JCheckBoxMenuItem verboseItem = new JCheckBoxMenuItem("Verbose");
		verboseItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final JCheckBoxMenuItem collisionItem = new JCheckBoxMenuItem("Collision Detection",false);
		collisionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final JCheckBoxMenuItem updateItem = new JCheckBoxMenuItem("Periodic Messages",false);
		updateItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final JCheckBoxMenuItem antialiasItem = new JCheckBoxMenuItem("Antialiasing",true);
		antialiasItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final JCheckBoxMenuItem limitItem = new JCheckBoxMenuItem("Limit to Inner Circle");
		limitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final JMenuItem quitItem  = new JMenuItem("Quit");
		quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		resetItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent evt) {
				manager.reset();
				simulation.reset();
			} 
		} );

		verboseItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent evt) {
				manager.verbose=verboseItem.getState();
			} 
		} );

		collisionItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent evt) {
				manager.collision=collisionItem.getState();
			} 
		} );

		updateItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent evt) {
				if (updateItem.getState()==true) simulation.enablePeriodicMessages();
				else simulation.disablePeriodicMessages();
			} 
		} );

		antialiasItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent evt) {
				manager.antialiasing=antialiasItem.getState();
				simulation.repaint();
			} 
		} );
		
		limitItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent evt) {
				simulation.limit(limitItem.getState());
			} 
		} );


		quitItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				simulation.quit();
				System.exit(0);	
			} 
		} );

		optionMenu.add(verboseItem);
		optionMenu.add(collisionItem);
		optionMenu.add(antialiasItem);
		optionMenu.add(limitItem);
		optionMenu.add(updateItem);
		optionMenu.add(new JSeparator()); 
		optionMenu.add(resetItem);
		optionMenu.add(quitItem);

		
		JMenu invertMenu = new JMenu("Invert");
		final JCheckBoxMenuItem xaxisItem = new JCheckBoxMenuItem("X-axis");
		xaxisItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		xaxisItem.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent evt) {
			manager.invertx = xaxisItem.getState();
		} } );
		invertMenu.add(xaxisItem);		

		final JCheckBoxMenuItem yaxisItem = new JCheckBoxMenuItem("Y-axis");
		yaxisItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		yaxisItem.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent evt) {
			manager.inverty = yaxisItem.getState();
		} } );
		invertMenu.add(yaxisItem);		

		final JCheckBoxMenuItem angleItem = new JCheckBoxMenuItem("Angle");
		angleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		angleItem.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent evt) {
			manager.inverta = angleItem.getState();
		} } );
		invertMenu.add(angleItem);		
		
		
		JMenu helpMenu = new JMenu("Help");
		JMenuItem manualItem = new JMenuItem("User Manual");
		manualItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		final HelpBrowser helpBrowser = new HelpBrowser();
		manualItem.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent evt) {
			helpBrowser.reset();
		} } );
		helpMenu.add(manualItem);
		helpMenu.add(new JSeparator());
		JMenuItem aboutItem = new JMenuItem("About TuioSimulator");
		aboutItem.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent evt) {
			new AboutDialog().setVisible(true);
		} } );
		helpMenu.add(aboutItem);
		
		menubar.add(optionMenu);
		menubar.add(invertMenu);
		menubar.add(helpMenu);
		app.setJMenuBar(menubar);

		app.pack();
		Insets ins = app.getInsets();
		app.setSize(width + ins.left + ins.right, height + ins.top  + ins.bottom + 20);

		app.setResizable(false);
		app.setVisible(true);
	}
}

class AboutDialog extends JDialog {

	public AboutDialog()
	{
		super(new JFrame(), false);
		getContentPane().setLayout(null);
		getContentPane().setBackground(Color.white);
		setVisible(false);
		setSize(300,150);
		setLocation(220,180);
		setResizable(false);
		setTitle("about ...");
		
		Label headlineLabel = new Label("TUIO Simulator 1.1.6",Label.CENTER);
		headlineLabel.setBounds(5,20,280,20);
		headlineLabel.setBackground(Color.white);
		headlineLabel.setFont(new Font("Dialog", Font.BOLD, 18));
		getContentPane().add(headlineLabel);
		
		Label copyrightLabel = new Label("(c) 2005-2016 Martin Kaltenbrunner",Label.CENTER);
		copyrightLabel.setBounds(5,40,280,15);
		copyrightLabel.setBackground(Color.white);
		getContentPane().add(copyrightLabel);
		
		Label urlLabel = new Label("http://www.tuio.org/",Label.CENTER);
		urlLabel.setBounds(5,55,280,15);
		urlLabel.setBackground(Color.white);
		getContentPane().add(urlLabel);
		
		Button okButton = new Button();
		okButton.setLabel("OK");
		okButton.setBounds(100,85,90,25);
		okButton.setBackground(Color.white);
		getContentPane().add(okButton);
		
		addWindowListener (
			new WindowAdapter () {
				public void windowClosing (WindowEvent evt) {
					dispose();
				}
			}
		);

		okButton.addActionListener (
			new ActionListener () {
				public void actionPerformed (ActionEvent evt) {
					dispose();
				}
			}
		);
	} 
}
