package com.sugree.twitter.views;

import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;

/**
 *
 * @author mvlad
 */
class AboutUserScreen extends Form implements CommandListener {
    
    Displayable prevScreen;
    TwitterController twiController;
    Command okCommand, timelineCommand;
    Status status;
    
    AboutUserScreen(Status status, Displayable screen, TwitterController controller) {
        super("About @" + status.getScreenName());
        //setTitle("");
        prevScreen = screen;
        twiController = controller;
        this.status = status;
        addCommand(okCommand = new Command("OK", Command.ITEM, 1));
        addCommand(timelineCommand = new Command("Timeline", Command.SCREEN, 2));
        setCommandListener(this);
        drawUserInfo();
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == okCommand) {
            twiController.setCurrent(prevScreen);
        } else if (c == timelineCommand) {
            twiController.newSearch("@" + status.getScreenName());
        }
    }
    
    private void drawUserInfo() {
        new Thread(new Runnable() {
            public void run() {
                Vector ab = status.aboutUser();
                if (ab != null) {
                    for (int i = 0; i < ab.size(); i++) {
                        final String[] kv = (String[]) ab.elementAt(i);
                        if (kv[0].endsWith("_image_url")) {
                            new Thread(new Runnable() {
                                public void run() {
                                    if (kv[1].startsWith("http")) {
                                        try {
                                            Image im = HttpUtil.getImage(
                                                    kv[1],
                                                    twiController.maxPicSize());
                                            append(kv[0] + ": ");
                                            append(im);
                                        } catch (IOException e) {
                                            Log.error(e.toString());
                                        } catch (OutOfMemoryError om) {
                                            Log.error(om.toString());
                                        }
                                    }
                                }
                            }).start();
                            
                        } else {
                            StringItem it = new StringItem(kv[0] + ":", kv[1]);
                            AboutUserScreen.this.append(it);
                        }
                    }
                } else {
                    AboutUserScreen.this.append("no info about @" + status.getScreenName());
                }
                AboutUserScreen.this.twiController.setCurrent(AboutUserScreen.this);
            }
        }).start();
    }
}
