package com.sugree.twitter.views;

import com.sugree.twitter.TwitterController;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class LinkScreen extends List implements CommandListener {

    private TwitterController controller;
    private static final String[] linkLabels = {
        "Official Home",
        "JAD link",
        "JAR link",
        "Mobile Twitter",};
    private final String[] linkValues = {
        "http://twi.rosinstrument.com/dist/",
        "http://twi.rosinstrument.com/dist/TSurv.jad",
        "http://twi.rosinstrument.com/dist/TSurv.jar",
        "http://m.twitter.com/",};
    private Command goCommand;
    private Command cancelCommand;

    public LinkScreen(TwitterController controller) {
        super("Links", Choice.IMPLICIT, linkLabels, null);
        this.controller = controller;

        goCommand = new Command("Go", Command.ITEM, 1);
        addCommand(goCommand);
        cancelCommand = new Command("Cancel", Command.CANCEL, 2);
        addCommand(cancelCommand);

        setCommandListener(this);
    }

    public void commandAction(Command cmd, Displayable display) {
        int index = getSelectedIndex();
        if ((cmd == goCommand || cmd == List.SELECT_COMMAND) && index >= 0) {
            controller.openUrl(linkValues[index]);
            if (index > 1) {
                controller.exit();
            } else {
                controller.showTimeline();
            }
        }
        if (cmd == cancelCommand) {
            controller.showTimeline();
        }
    }
}
