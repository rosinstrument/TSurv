package com.sugree.twitter.views;

import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import com.substanceofcode.utils.TimeUtil;
import com.substanceofcode.utils.Log;
import com.substanceofcode.twitter.model.Status;
import com.sugree.twitter.TwitterController;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class OAuthScreen extends javax.microedition.lcdui.Form implements CommandListener {

    private TwitterController controller;
    private Displayable nextDisplay;
    private javax.microedition.lcdui.StringItem textField;
    private javax.microedition.lcdui.TextField pinField;
    private Command okCommand;
    private Command retryCommand;
    private Command goCommand;
    private Command cancelCommand;
    private String url;

    public OAuthScreen(TwitterController controller, String url, Displayable nextDisplay) {
        super("OAuth");
        this.controller = controller;
        this.nextDisplay = nextDisplay;
        this.url = url;

        textField = new javax.microedition.lcdui.StringItem("Open this url to get PIN code", "\n\n" + url + "\n");
        append(textField);
        pinField = new javax.microedition.lcdui.TextField("PIN", "", 10, TextField.NUMERIC);
        append(pinField);

        okCommand = new Command("Submit PIN", Command.OK, 1);
        addCommand(okCommand);
        retryCommand = new Command("Retry", Command.SCREEN, 2);
        addCommand(retryCommand);
        goCommand = new Command("Go to URL", Command.SCREEN, 3);
        addCommand(goCommand);
        cancelCommand = new Command("Cancel", Command.CANCEL, 4);
        addCommand(cancelCommand);

        setCommandListener(this);
    }

    public void commandAction(Command cmd, Displayable display) {
        if (cmd == okCommand) {
            if ("".equals(pinField.getString())) {
                controller.showError("Error", "Missing PIN", AlertType.ERROR);
                return;
            }
            controller.startOAuthAccessToken(pinField.getString());
        } else if (cmd == retryCommand) {
            controller.startOAuthRequestToken();
        } else if (cmd == goCommand) {
            controller.openUrl(url);
        } else if (cmd == cancelCommand) {
            controller.setCurrent(nextDisplay);
        }
    }
}
