package com.sugree.twitter.views;

import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class LogScreen extends javax.microedition.lcdui.Form implements javax.microedition.lcdui.CommandListener {
	private TwitterController controller;

	private javax.microedition.lcdui.StringItem textField;
	private Command backCommand;
	private Command clearCommand;

	public LogScreen(TwitterController controller) {
		super("Log");
		this.controller = controller;

		textField = new javax.microedition.lcdui.StringItem("", Log.getText());
		append(textField);

		backCommand = new Command("Back", Command.OK, 1);
		addCommand(backCommand);
		clearCommand = new Command("Clear", Command.CANCEL, 2);
		addCommand(clearCommand);

		setCommandListener(this);
	}

	public void commandAction(Command cmd, Displayable display) {
		if (cmd == backCommand) {
			controller.showTimeline();
		} else if (cmd == clearCommand) {
			Log.clear();
			controller.showTimeline();
		}
	}
}
