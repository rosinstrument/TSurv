/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sugree.twitter.views;

import com.substanceofcode.twitter.Settings;
import com.substanceofcode.utils.StringUtil;
import com.sugree.twitter.TwitterController;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;

/**
 *
 * @author mvlad
 */
public class BookmarksScreen extends InsertScreen {

    Command emptyCommand, removeCommand, addCommand;
    private final String bookmarkTitle = "Bookmarks", delimiterBookmark = "\n";

    public BookmarksScreen(TwitterController controller) {
        super(controller);
        super.setTitle("Bookmarks");
        removeCommand(insertCommand);
        insertCommand = new Command("Open", Command.ITEM, 1);
        addCommand(insertCommand);
        removeCommand = new Command("Remove", Command.ITEM, 3);
        addCommand(removeCommand);
        emptyCommand = new Command("Empty", Command.ITEM, 4);
        addCommand(emptyCommand);
        addCommand = new Command("Add", Command.ITEM, 5);
        addCommand(addCommand);
        setCustom(getBookmarks());
        setWords(null);
    }

    public void commandAction(Command cmd, Displayable display) {
        int index = getSelectedIndex();
        if ((cmd == insertCommand || cmd == List.SELECT_COMMAND) && index >= 0) {
            if (index < 0) {
                return;
            }
            controller.newSearch(getString(index));
            return;
        } else if (cmd == cancelCommand) {
            controller.setCurrent(controller.getScreen(TwitterController.SCREEN_TIMELINE));
        } else if (cmd == emptyCommand) {
            if (index < 0) {
                return;
            }
            emptyBookmarks();
            controller.setCurrent(controller.getScreen(TwitterController.SCREEN_TIMELINE));
        } else if (cmd == removeCommand) {
            if (index < 0) {
                return;
            }
            removeBookmark(index);
        } else if (cmd == addCommand) {
            Form form = new Form("Add Bookmark");
            final TextField tf = new TextField("", "#@$", 140, TextField.ANY);
            form.append(tf);
            final Command okCommand = new Command("Add", Command.OK, 1);
            final Command cancel = new Command("Cancel", Command.CANCEL, 2);
            form.addCommand(okCommand);
            form.addCommand(cancel);
            CommandListener cl = new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    if (c == okCommand) {
                        String result = tf.getString();
                        if (result != null && result.length() > 0) {
                            saveBookmark(result);
                        }
                        controller.setCurrent(BookmarksScreen.this);
                    } else if (c == cancel) {
                        controller.setCurrent(BookmarksScreen.this);
                    }
                }
            };
            form.setCommandListener(cl);
            controller.setCurrent(form);
        }
    }

    public void saveBookmark(String searchTerm) {
        if (searchTerm != null) {
            Vector v = new Vector();
            v.addElement(searchTerm);
            String[] bs = getBookmarks();
            if (bs != null) {
                for (int count = 0, i = 0; i < bs.length; i++) {
                    if (!searchTerm.equalsIgnoreCase(bs[i])) {
                        v.addElement(bs[i]);
                        if (++count > 50) {
                            break;
                        }
                    } else {
                        delete(i);
                    }
                }
            }
            insert(0, searchTerm, null);
            setBookmarks(StringUtil.join(v.elements(), delimiterBookmark));
        }
    }

    private Vector bookmarks() {
        Vector v = new Vector();
        String[] bs = getBookmarks();
        if (bs != null) {
            for (int i = 0; i < bs.length; i++) {
                v.addElement(bs[i]);
            }
        }
        return v;
    }

    protected void removeBookmark(int index) {
        if (index >= 0) {
            Vector v = bookmarks();
            v.removeElementAt(index);
            delete(index);
            setBookmarks(StringUtil.join(v.elements(), delimiterBookmark));
        }
    }

    private String[] getBookmarks() {
        Settings s = controller.getSettings();
        String b = s.getStringProperty(bookmarkTitle, "");
        if (b == null || b.length() == 0) {
            return null;
        }
        String ba[] = StringUtil.split(b, delimiterBookmark);
        return ba;
    }

    public void emptyBookmarks() {
        setBookmarks("");
    }

    boolean setBookmarks(String bookmarks) {
        return controller.getSettings().setStringProperty(bookmarkTitle, bookmarks);
    }
}
