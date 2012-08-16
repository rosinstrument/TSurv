package com.sugree.twitter;

import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.Log;
import javax.microedition.midlet.MIDlet;

public class TwitterMIDlet extends MIDlet {

    public static String NAME;
    public static String VERSION;
    private TwitterController controller;
    private boolean first;

    public TwitterMIDlet() {
        NAME = getAppProperty("MIDlet-Name");
        VERSION = getAppProperty("MIDlet-Version");
        first = true;
        try {
            controller = new TwitterController(this);
            HttpUtil.setTwitterController(controller);
        } catch (Exception e) {
            Log.error(e.toString());
        }
    }

    public void startApp() {
        if (first) {
            controller.showStart();
            first = false;
        } else {
            controller.showTimeline();
        }
    }

    public void pauseApp() {
        notifyPaused();
    }

    public void destroyApp(boolean unconditional) {
        try {
        } catch (Exception ex) {
        }
        notifyDestroyed();
    }
}
