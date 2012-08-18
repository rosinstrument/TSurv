package com.sugree.twitter.views;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import com.sugree.utils.Loggable;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.StringItem;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class WaitScreen extends Form implements CommandListener, Runnable, Loggable {

    private TwitterController controller;
    private AbstractTask task;
    private int cancelScreenId = -1;
    private Displayable cancelScreen = null;
    private StringItem stateField, logField;
    private Gauge progressField;
    private Command cancelCommand;
    private Thread thread;
    public boolean finished = false;
    private Displayable prevScreen = null;

    public WaitScreen(TwitterController controller, AbstractTask task, int cancelScreenId) {
        super("Wait");
        this.controller = controller;
        this.task = task;
        this.cancelScreenId = cancelScreenId;
        init();
    }

    public WaitScreen(TwitterController controller, AbstractTask task, Displayable cancelScreen) {
        super("Wait");
        this.controller = controller;
        this.task = task;
        this.cancelScreen = cancelScreen;
        init();
    }

    private void init() {
        prevScreen = controller.getCurrentScreen();
        thread = new Thread(this);
        stateField = new StringItem("", "waiting");
        append(stateField);
        progressField = new Gauge("Progress", false, 100, 0);
        append(progressField);
        logField = new StringItem("", "");
        append(logField);
        cancelCommand = new Command("Cancel", Command.STOP, 1);
        addCommand(cancelCommand);
        setCommandListener(this);
    }

    public void setState(String text) {
        stateField.setText(text);
    }

    public void setProgress(int value) {
        progressField.setValue(value);
    }

    public void addProgressCircle(int value) {
        int newVal = progressField.getValue() + value;
        if (newVal > progressField.getMaxValue()) {
            newVal = 0;
        }
        if (newVal < 0) {
            newVal = progressField.getMaxValue();
        }
        progressField.setValue(newVal);
    }

    public void clear() {
        setText("");
    }

    public void print(String text) {
        setText(logField.getText() + text);
    }

    public void println(String text) {
        setText(logField.getText() + text + "\n");
    }

    public void setText(String text) {
        logField.setText(text);
    }

    public void commandAction(Command cmd, Displayable display) {
        if (cmd == cancelCommand) {
            try {
                thread.interrupt();
            } catch (SecurityException se) {
            }
            if (cancelScreen != null) {
                controller.setCurrent(cancelScreen);
            } else if (cancelScreenId >= 0) {
                controller.setCurrent(cancelScreenId);
            }
        }
    }

    public void start() {
        thread.start();
    }

    public void run() {
        Log.setConsole(this);
        try {
            task.run();
        } catch (Throwable e) {
            controller.alert("Error", "Error running '"
                    + task.getClass().getName() + "' task", e);
        }
        Log.setConsole(null);
        finished = true;
        //failed to set screen in Task due to unhandled Exception or error
        if (controller.getCurrentScreen() == this) {
            controller.setCurrent(prevScreen);
        }
    }
}
