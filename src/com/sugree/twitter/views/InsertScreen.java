package com.sugree.twitter.views;

import com.sugree.twitter.TwitterController;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class InsertScreen extends List implements CommandListener {

    protected TwitterController controller;
    protected Command insertCommand, cancelCommand;
    private Vector custom;
    private Vector words;

    public InsertScreen(TwitterController controller) {
        super("Insert", Choice.IMPLICIT);
        this.controller = controller;
        custom = new Vector();
        words = new Vector();
        insertCommand = new Command("Insert", Command.ITEM, 1);
        addCommand(insertCommand);
        cancelCommand = new Command("Cancel", Command.CANCEL, 2);
        addCommand(cancelCommand);
        setCommandListener(this);
    }

    public void removeAll() {
        while (size() > 0) {
            delete(0);
        }
    }

    public void setCustom(String[] words) {
        custom.removeAllElements();
        if (words == null || words.length == 0) {
            return;
        }
        for (int i = 0; i < words.length; i++) {
            custom.addElement(words[i]);
        }
    }

    public void setWords(Vector words) {
        if (words != null) {
            this.words = words;
        }
        updateInsert();
    }

    private void updateInsert() {
        Enumeration wordEnum;
        removeAll();
        wordEnum = custom.elements();
        while (wordEnum.hasMoreElements()) {
            append((String) wordEnum.nextElement(), null);
        }
        wordEnum = words.elements();
        while (wordEnum.hasMoreElements()) {
            append((String) wordEnum.nextElement(), null);
        }
    }

    public void commandAction(Command cmd, Displayable display) {
        int index = getSelectedIndex();
        if ((cmd == insertCommand || cmd == List.SELECT_COMMAND) && index >= 0) {
            controller.insertUpdate(getString(index));
        }
        if (cmd == cancelCommand) {
            controller.insertUpdate("");
        }
    }

    public void addWord(String word, int pos) {
        words.insertElementAt(word, pos);
        updateInsert();
    }
}
