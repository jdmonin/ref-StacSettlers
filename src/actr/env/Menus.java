package actr.env;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

class Menus extends JMenuBar
{	
	private Actions actions;
	private Preferences prefs;
	private JMenu fileMenu, editMenu, runMenu, outputMenu;
	private JMenu openRecentMenu;
	private int accelerator = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	Menus (Actions actions, Preferences prefs)
	{
		this (actions, prefs, false);
	}

	Menus (Actions actions, Preferences prefs, boolean fileOnly)
	{
		this.actions = actions;
		this.prefs = prefs;

		fileMenu = new JMenu ("File");
		addToMenu (fileMenu, actions.newAction, KeyEvent.VK_N);
		addToMenu (fileMenu, actions.openAction, KeyEvent.VK_O);

		openRecentMenu = new JMenu ("Open Recent");
		fileMenu.add (openRecentMenu);

		if (!fileOnly)
		{
			fileMenu.addSeparator();
			addToMenu (fileMenu, actions.closeAction, KeyEvent.VK_W);
			addToMenu (fileMenu, actions.saveAction, KeyEvent.VK_S);
			addToMenu (fileMenu, actions.saveAsAction, KeyEvent.VK_S, accelerator + ActionEvent.SHIFT_MASK);
			fileMenu.addSeparator();
			addToMenu (fileMenu, actions.printAction, KeyEvent.VK_P);
			if (! Main.onMac())
			{
				fileMenu.addSeparator();
				addToMenu (fileMenu, actions.aboutAction);
				fileMenu.addSeparator();
				addToMenu (fileMenu, actions.exitAction, KeyEvent.VK_Q);
			}

			editMenu = new JMenu ("Edit");
			addToMenu (editMenu, actions.undoAction, KeyEvent.VK_Z);
			addToMenu (editMenu, actions.redoAction, KeyEvent.VK_Y);
			editMenu.addSeparator();
			addToMenu (editMenu, actions.cutAction, KeyEvent.VK_X);
			addToMenu (editMenu, actions.copyAction, KeyEvent.VK_C);
			addToMenu (editMenu, actions.pasteAction, KeyEvent.VK_V);
			editMenu.addSeparator();
			addToMenu (editMenu, actions.findAction, KeyEvent.VK_F);
			addToMenu (editMenu, actions.findNextAction, KeyEvent.VK_G);
			addToMenu (editMenu, actions.findPreviousAction, KeyEvent.VK_G, accelerator + ActionEvent.SHIFT_MASK);
			if (! Main.onMac())
			{
				editMenu.addSeparator();
				addToMenu (editMenu, actions.prefsAction);
			}
			
			runMenu = new JMenu ("Run");
			addToMenu (runMenu, actions.runAction, KeyEvent.VK_R);
			addToMenu (runMenu, actions.runAnalysisAction, KeyEvent.VK_R, accelerator + ActionEvent.SHIFT_MASK);

			runMenu.addSeparator();
			addToMenu (runMenu, actions.stopAction, KeyEvent.VK_PERIOD);
			addToMenu (runMenu, actions.resumeAction, KeyEvent.VK_PERIOD, accelerator + ActionEvent.SHIFT_MASK);

			outputMenu = new JMenu ("Output");
			addToMenu (outputMenu, actions.outputBuffersAction);
			addToMenu (outputMenu, actions.outputWhyNotAction);
			addToMenu (outputMenu, actions.outputDMAction);
			addToMenu (outputMenu, actions.outputPRAction);
			addToMenu (outputMenu, actions.outputVisiconAction);
			//outputMenu.addSeparator();
			//addToMenu (outputMenu, actions.outputTasksAction);
		}

		add (fileMenu);
		if (!fileOnly)
		{
			add (editMenu);
			add (runMenu);
			add (outputMenu);
		}

		updateOpenRecent();
	}

	void addToMenu (JMenu menu, Action action, int vk, int modifiers)
	{
		action.putValue (Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke (vk, modifiers));
		JMenuItem item = new JMenuItem (action);
		item.setIcon (null);
		menu.add (item);
	}

	void addToMenu (JMenu menu, Action action, int vk)
	{
		addToMenu (menu, action, vk, accelerator);
	}

	void addToMenu (JMenu menu, Action action)
	{
		JMenuItem item = new JMenuItem (action);
		item.setIcon (null);
		menu.add (item);
	}

	void updateOpenRecent ()
	{
		openRecentMenu.removeAll();
		for (int i=0 ; i<prefs.recentFiles.size() ; i++)
		{
			String filename = prefs.recentFiles.elementAt(i);
			if (filename!=null && !filename.equals(""))
			{
				Action action = actions.createOpenRecentAction (new File (filename));
				addToMenu (openRecentMenu, action, KeyEvent.VK_1 + i);
			}
		}
		repaint();
	}
}
