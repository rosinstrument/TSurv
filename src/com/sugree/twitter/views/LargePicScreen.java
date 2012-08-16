/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sugree.twitter.views;

import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.game.Sprite;

/**
 *
 * @author mvlad
 */
class LargePicScreen extends Form implements CommandListener {

    Displayable prevScreen;
    TwitterController twiController;
    Command okCommand, cancelCommand, leftCommand, rightCommand,
            topCommand, bottomCommand, saveCommand;
    private int x, y;
    Image image = null;
    byte[] bimage = null;
    int imageId = -1;
    public boolean okReturn = false;
    private boolean cancel = false;
    private final static int DIV = 4;

    public LargePicScreen(Image image, String title, Displayable screen, TwitterController controller, boolean cancel) {
        super(title);
        prevScreen = screen;
        twiController = controller;
        this.image = image;
        this.cancel = cancel;
        try {
            init();
        } catch (Throwable ofm) {
            Log.error("init: " + ofm.toString());
            exit();
            destroy();
            controller.alert("Error", "Show picture", ofm);
        }
    }

    public LargePicScreen(byte[] bimage, String title, Displayable screen, TwitterController controller, boolean cancel) {
        super(title);
        prevScreen = screen;
        twiController = controller;
        this.bimage = bimage;
        this.cancel = cancel;
        try {
            this.image = Image.createImage(bimage, 0, bimage.length);
            init();
        } catch (OutOfMemoryError ofm) {
            Log.error("init: " + ofm.toString());
            exit();
            destroy();
            controller.showAlert(ofm.toString(), ofm.getMessage());
        }
    }

    private void init() {
        x = y = 0;
        addCommand(okCommand = new Command("Back", Command.BACK, 1));
        if (bimage != null) {
            addCommand(saveCommand = new Command("Save", Command.ITEM, 2));
        }
        if (cancel) {
            addCommand(cancelCommand = new Command("Cancel", Command.EXIT, 2));
        }
        leftCommand = new Command("<", Command.OK, 4);
        rightCommand = new Command(">", Command.CANCEL, 3);
        topCommand = new Command("^", Command.ITEM, 3);
        bottomCommand = new Command("V", Command.ITEM, 4);
        drawImg(0, 0);
        setCommandListener(this);
        twiController.setCurrent(this);
    }

    public void draw() {
        drawImg(0, 0);
    }

    private void drawImg(int xdirection, int ydirection) {
        try {
            int iw = image.getWidth(), ih = image.getHeight(), sw = getWidth(), sh = getHeight();
            if (xdirection == 0 && ydirection == 0) {
                if (iw <= sw) {
                    removeCommand(leftCommand);
                    removeCommand(rightCommand);
                } else {
                    addCommand(leftCommand);
                    addCommand(rightCommand);
                }
                if (ih <= sh) {
                    removeCommand(topCommand);
                    removeCommand(bottomCommand);
                } else {
                    addCommand(topCommand);
                    addCommand(bottomCommand);
                }
            }
            if (xdirection > 0) {
                x += sw / DIV;
            } else if (xdirection < 0) {
                x -= sw / DIV;
            }
            if (x >= iw - sw) {
                x = iw - sw - 1;
            }
            if (x < 0) {
                x = 0;
            }
            int dwx = iw - x;
            if (sw - 1 < dwx) {
                dwx = sw - 1;
            }
            if (ydirection > 0) {
                y += sh / DIV;
            } else if (ydirection < 0) {
                y -= sh / DIV;
            }
            if (y >= ih - sh) {
                y = ih - sh - 1;
            }
            if (y < 0) {
                y = 0;
            }
            int dhy = ih - y;
            if (sh - 1 < dhy) {
                dhy = sh - 1;
            }
            Image im = Image.createImage(image, x, y, dwx, dhy, Sprite.TRANS_NONE);
            ImageItem ii = new ImageItem(null, im, ImageItem.LAYOUT_DEFAULT, null);
            if (imageId >= 0) {
                set(imageId, ii);
            } else {
                imageId = append(ii);
            }
        } catch (Throwable ia) {
            String emsg = ia.toString();
            deleteAll();
            imageId = append(emsg);
            Log.error(emsg);
        }
    }

    private void destroy() {
        image = null;
        bimage = null;
        prevScreen = null;
    }

    private void exit() {
        twiController.setCurrent(prevScreen);
        synchronized (prevScreen) {
            prevScreen.notifyAll();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == okCommand || c == cancelCommand) {
            okReturn = (c == okCommand) ? true : false;
            exit();
            destroy();
        } else if (c == leftCommand) {
            drawImg(+1, 0);
        } else if (c == rightCommand) {
            drawImg(-1, 0);
        } else if (c == topCommand) {
            drawImg(0, +1);
        } else if (c == bottomCommand) {
            drawImg(0, -1);
        } else if (c == saveCommand) {
            saveImg(null);
        }

    }

    private void saveImg(Displayable prevDisp) {
        try {
            String url = getTitle();
            if (url != null) {
                url = url.substring(url.lastIndexOf('/') + 1);
            }
            twiController.setSnapshot(bimage, url);
            if (prevDisp == null) {
                prevDisp = this;
            }
            FileScreen fs = new FileScreen(twiController, prevDisp, "Save picture to..");
            fs.start(true);
        } catch (Exception e) {
            Log.error(e.toString());
        } catch (OutOfMemoryError ofm) {
            Log.error(ofm.toString());
        }
    }
}
