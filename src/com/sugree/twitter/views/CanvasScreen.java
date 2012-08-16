package com.sugree.twitter.views;

import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.TwitterException;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public class CanvasScreen extends Canvas implements CommandListener {

    Displayable prevScreen;
    TwitterController twiController;
    byte[] bimage = null;
    public boolean okReturn = false;
    private boolean cancel = false;
    public static final int ROTATE_CCW = 2;
    public static final int ROTATE_CW = 1;
    public static final int ORIENT_UP = 0;
    public static final int ORIENT_RIGHT = 1;
    public static final int ORIENT_DOWN = 2;
    public static final int ORIENT_LEFT = 3;
    int xscroll, yscroll;
    private boolean showZoomStatus = false;
    private boolean isFullScreen = true;
    private int dispWidth, dispHeight;
    Image im = null;
    Image imoriginal = null;
    // private Command salir;
    // private Command zoom;
    private int x, y, w, h;
    private int zoomlevel = 1;
    int orientation;
    private String title;
    private Command okCommand, saveCommand, cancelCommand,
            rotate, zoomin, zoomout;

    public CanvasScreen(Image image, String title, Displayable screen, TwitterController controller, boolean cancel) throws TwitterException {
        super();
        imoriginal = image;
        prevScreen = screen;
        twiController = controller;
        this.cancel = cancel;
        this.title = title;
        try {
            init();
        } catch (Throwable ofm) {
            Log.error("init: " + ofm.toString());
            exit();
            throw new TwitterException(ofm);
        }
    }

    public CanvasScreen(byte[] bimage, String title, Displayable screen, TwitterController controller, boolean cancel) throws TwitterException {
        super();
        prevScreen = screen;
        twiController = controller;
        this.bimage = bimage;
        this.cancel = cancel;
        this.title = title;
        try {
            imoriginal = Image.createImage(bimage, 0, bimage.length);
            init();
        } catch (OutOfMemoryError ofm) {
            Log.error("init: " + ofm.toString());
            exit();
            throw new TwitterException(ofm);
        }
    }

    public CanvasScreen(InputStream is, String title, Displayable screen, TwitterController controller, boolean cancel) throws TwitterException {
        super();
        prevScreen = screen;
        twiController = controller;
        this.cancel = cancel;
        this.title = title;
        try {
            imoriginal = Image.createImage(is);
            is.close();
            init();
        } catch (OutOfMemoryError ofm) {
            Log.error("init: " + ofm.toString());
            exit();
            throw new TwitterException(ofm);
        } catch (IOException io) {
            Log.error("init: " + io.toString());
            exit();
            throw new TwitterException(io);
        }
    }

    public CanvasScreen(String fileName, String title, Displayable screen, TwitterController controller, boolean cancel) throws TwitterException {
        super();
        prevScreen = screen;
        twiController = controller;
        this.cancel = cancel;
        this.title = title;
        InputStream fis = null;
        try {
            fis = Connector.openInputStream(fileName);
            imoriginal = Image.createImage(fis);
            init();
        } catch (OutOfMemoryError ofm) {
            Log.error("init: " + ofm.toString());
            exit();
            throw new TwitterException(ofm);
        } catch (IOException io) {
            Log.error("init: " + io.toString());
            exit();
            throw new TwitterException(io);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private void init() throws TwitterException {
        if (prevScreen == null) {
            prevScreen = twiController.getCurrentScreen();
        }
        setFullScreenMode(isFullScreen);
        im = Image.createImage(imoriginal);
        w = im.getWidth();
        h = im.getHeight();
        dispWidth = getWidth();
        dispHeight = getHeight();
        xscroll = dispWidth / 3;
        yscroll = dispHeight / 3;
        x = y = 0;
        orientation = ORIENT_UP;
        addCommand(zoomin = new Command("zoom +", Command.SCREEN, 1));
        addCommand(zoomout = new Command("zoom -", Command.SCREEN, 1));
        addCommand(rotate = new Command("Rotate", Command.SCREEN, 1));
        addCommand(okCommand = new Command(cancel ? "OK" : "Back", Command.BACK, 1));
        if (bimage != null) {
            addCommand(saveCommand = new Command("Save", Command.ITEM, 2));
        }
        if (cancel) {
            addCommand(cancelCommand = new Command("Cancel", Command.EXIT, 2));
        }
        setCommandListener(this);
        twiController.setCurrent(this);
    }

    public void paint(Graphics g) {
        if (im == null) {
            return;
        }
        String zm = "";
        g.setColor(255, 255, 255);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(0, 0, 0);
        g.drawImage(im, x, y, Graphics.TOP | Graphics.LEFT);
        switch (zoomlevel) {
            case 1:
                zm = "100%";
                break;
            case 2:
                zm = "50%";
                break;
            case 3:
                zm = "33%";
                break;
            case 4:
                zm = "25%";
                break;
            case 5:
                zm = "fit to width";
                break;
        }
        if (showZoomStatus) {
            g.drawString("zoom: " + zm, 0, 0, Graphics.TOP | Graphics.LEFT);
            showZoomStatus = false;
        }
    }

    protected void keyPressed(int keyCode) {
        scrollImage(keyCode, 1.3);
    }

    protected void keyRepeated(int keyCode) {
        scrollImage(keyCode, 1);
    }

    void scrollImage(int keyCode, double scrollAccel) {
        int keyAction;
        try {
            keyAction = getGameAction(keyCode);
        } catch (IllegalArgumentException e) {
            keyAction = 0;
        }
        if (keyAction == 0) {
            keyAction = keyCode;
        }
//      Alert a = new Alert("keyAction = " + keyAction);
//      Display.getDisplay(midlet).setCurrent(a, this);
        switch (keyAction) {
            // (x,y) im anchor relative to canvas
            case Canvas.UP:
            case Canvas.KEY_NUM2:
                y += yscroll * scrollAccel;
                break;
            case Canvas.RIGHT:
            case Canvas.KEY_NUM6:
                x -= xscroll * scrollAccel;
                break;
            case Canvas.DOWN:
            case Canvas.KEY_NUM8:
                y -= yscroll * scrollAccel;
                break;
            case Canvas.LEFT:
            case Canvas.KEY_NUM4:
                x += xscroll * scrollAccel;
                break;
            case Canvas.FIRE:
            case Canvas.KEY_NUM5:
                isFullScreen = !isFullScreen;
                setFullScreenMode(isFullScreen);
                break;
            case Canvas.KEY_STAR:
            case Canvas.GAME_C:
                zoomout();
                return;
            case Canvas.KEY_POUND:
            case Canvas.GAME_D:
                zoomin();
                return;
            case Canvas.KEY_NUM0:
                rotateImage(ROTATE_CW);
                return;
            default:
        }
        y = Math.max(y, -im.getHeight() + dispHeight);
        x = Math.max(x, -im.getWidth() + dispWidth);
        if (y > 0) {
            y = 0;
        }
        if (x > 0) {
            x = 0;
        }
        repaint();
    }

    public void zoomin() {
        if (zoomlevel != 1) {
            zoomlevel--;
        }
        rescaleImage(zoomlevel);
    }

    public void zoomout() {
        if (zoomlevel != 5) {
            zoomlevel++;
        }
        rescaleImage(zoomlevel);
    }

    /*
     *  zoomlevel
     * 1=100%
     * 2=50%
     * 3=33%
     * 4=25%
     * 5=fit to screen
     */
    public void rescaleImage(int zoom) {
        showZoomStatus = true;
        zoomlevel = zoom;
        im = imoriginal;
        w = im.getWidth();
        h = im.getHeight();
        x = 0;
        y = 0;
        orientation = ORIENT_UP;
        if (zoom == 1) {           // return original image (fasterrrrrrrrrrrr)!!
            repaint();
            return;
        } else if (zoom == 5) {    // fit to screen
            zoom = w / getWidth();
        }
        im = null;
        System.gc();
//      double d_zoom = (double) zoom;  
        int d_zoom = zoom;
//      if (zoom == 0) {
//          d_zoom = .5;    // double zoom, unused, causes out of memory
//      }
        int width = (int) (w / d_zoom);
        int height = (int) (h / d_zoom);
        Image newImage;
        try {
            newImage = Image.createImage(width, height);
        } catch (OutOfMemoryError e) {
            twiController.alert("Error", "Out of Memory! [resi]");
            System.gc();
            im = imoriginal;
            return;
        }
        Graphics g = newImage.getGraphics();
        int rgb[] = new int[w];
        int rgb_buf[] = new int[width];
        for (int y = 0; y < height; y++) {
            imoriginal.getRGB(rgb, 0, w, 0, y * d_zoom, w, 1);
            for (int x = 0; x < width; x++) {
                rgb_buf[x] = rgb[x * d_zoom];
            }
            g.drawRGB(rgb_buf, 0, width, 0, y, width, 1, false);
// too slow
//          for (int x = 0; x < width; x++) {
//
//              // void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height)
//              imoriginal.getRGB(rgb, 0, w, (int) (x * d_zoom), (int) (y * d_zoom), 1, 1);
//              g.drawRGB(rgb, 0, width, x, y, 1, 1, false);
//          }
        }
        im = newImage;
        w = im.getWidth();
        h = im.getHeight();
        repaint();
    }

    // low memory rotate at 100% zoom
    private void rotateImageInPlace(int direction) {
        final int nextOrientation;
        final int currOrientation = orientation;
        if (direction == ROTATE_CW) {
            nextOrientation = (orientation + 1) % 4;
        } else if (direction == ROTATE_CCW) {
            nextOrientation = (orientation - 1 + 4) % 4;  //+4 because % returns negative if dividend < 0 
        } else {
            return;
        }
        im = null;
        System.gc();
        try {
            im = rotateImage(imoriginal, nextOrientation);
            rotateImageViewCenter(currOrientation, nextOrientation); // recalcs x,y
            w = im.getWidth();
            h = im.getHeight();
        } catch (OutOfMemoryError e) {
            // try again
            System.gc();
            try {
                im = rotateImage(imoriginal, nextOrientation);
                rotateImageViewCenter(currOrientation, nextOrientation);
                w = im.getWidth();
                h = im.getHeight();
            } catch (OutOfMemoryError e2) {
                twiController.alert("Error", "Out of Memory! [riip]");
                Runnable runn = new Runnable() {
                    public void run() {
                        System.gc();
                        im = imoriginal;
                        w = im.getWidth();
                        h = im.getHeight();
                        orientation = ORIENT_UP;
                        repaint();
//                        midlet.commandAction(midlet.back, ScrollCanvas.this);
                    }
                };
                delayCallSerially(1000, runn);
                return;
            }
        }
        repaint();
    }

    void delayCallSerially(final long msec, final Runnable runn) {
        new Thread(
                new Runnable() {
                    public void run() {
                        synchronized (Thread.currentThread()) {
                            try {
                                Thread.currentThread().wait(msec);
                            } catch (Exception ew) {
                            }
                        }
                        twiController.getCurrent().callSerially(runn);
                    }
                }).start();
    }

    // calculate the new (-x,-y) top left rotated image 
    private void rotateImageViewCenter(int orientation, int nextOrientation) {
        int dispHeight_2 = dispHeight / 2;
        int dispWidth_2 = dispWidth / 2;
        int cx, cy; // center
        int x_anchor = -this.x;
        int y_anchor = -this.y;
        int h = this.h;
        int w = this.w;
        cx = x_anchor + dispWidth_2;
        cy = y_anchor + dispHeight_2;
        // CW rotate the center point until we reach nextOrientation
        for (; orientation % 4 != nextOrientation; orientation++) {
            // each rotation swaps h,w
            int temp = w;
            w = h;
            h = temp;
            int cy0 = cy;
            cy = cx;
            cx = w - cy0;
        }
        x_anchor = cx - dispWidth_2;
        y_anchor = cy - dispHeight_2;
        this.x = -x_anchor;
        this.y = -y_anchor;
    }

    // h, w is dimension of imSrc
    private Image rotateImage(Image imSrc, int orientation)
            throws OutOfMemoryError {
//      public void drawRegion(Image src,
//                             int x_src,
//                             int y_src,
//                             int width,
//                             int height,
//                             int transform,
//                             int x_dest,
//                             int y_dest,
//                             int anchor)
        // out of memory if rotate all at one shot, so draw in 100x100 regions
        int transform;
        Image newImage;
        Image im = imoriginal;
        int w_imorig = im.getWidth();
        int h_imorig = im.getHeight();
        this.orientation = orientation;
        if (orientation == ORIENT_RIGHT) {
            newImage = Image.createImage(h_imorig, w_imorig);
            Graphics g = newImage.getGraphics();
            transform = Sprite.TRANS_ROT90;
            for (int ychunk = 0; ychunk * 100 < h_imorig; ychunk++) {
                int destx = Math.max(0, h_imorig - ychunk * 100 - 100);
                int regionh = Math.min(100, h_imorig - ychunk * 100);
                for (int xchunk = 0; xchunk * 100 < w_imorig; xchunk++) {
                    int desty = xchunk * 100;
                    int regionw = Math.min(100, w_imorig - xchunk * 100);
                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                            Graphics.TOP | Graphics.LEFT);
                }
            }
        } else if (orientation == ORIENT_LEFT) {
            newImage = Image.createImage(h_imorig, w_imorig);
            Graphics g = newImage.getGraphics();
            transform = Sprite.TRANS_ROT270;
            for (int ychunk = 0; ychunk * 100 < h_imorig; ychunk++) {
                int destx = ychunk * 100;
                int regionh = Math.min(100, h_imorig - ychunk * 100);
                for (int xchunk = 0; xchunk * 100 < w_imorig; xchunk++) {
                    int desty = Math.max(0, w_imorig - xchunk * 100 - 100);
                    int regionw = Math.min(100, w_imorig - xchunk * 100);
                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                            Graphics.TOP | Graphics.LEFT);
                }
            }
        } else if (orientation == ORIENT_DOWN) {
            newImage = Image.createImage(w_imorig, h_imorig);
            Graphics g = newImage.getGraphics();
            transform = Sprite.TRANS_ROT180;
            for (int ychunk = 0; ychunk * 100 < h_imorig; ychunk++) {
                int desty = Math.max(0, h_imorig - ychunk * 100 - 100);
                int regionh = Math.min(100, h_imorig - ychunk * 100);
                for (int xchunk = 0; xchunk * 100 < w_imorig; xchunk++) {
                    int destx = Math.max(0, w_imorig - xchunk * 100 - 100);
                    int regionw = Math.min(100, w_imorig - xchunk * 100);
                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                            Graphics.TOP | Graphics.LEFT);
                }
            }
        } else { // ORIENT_UP
            return Image.createImage(imSrc);
        }
        return newImage;
    }

    public void rotateImage(int direction) {
        // uses different method for 100% zoom rotate and zoom out rotate
        // for better memory use during 100% zoom, and zoom out rotate 
        // rotates zoomed image, and handles Out of Memory error
        // without losing zoomed image (resetting it back to imoriginal)
        // 100% zoom rotate
        if (zoomlevel == 1) {
            rotateImageInPlace(direction);
            return;
        }
        // zoomed rotate
        Image newImage;
        try {
            newImage = Image.createImage(h, w);
        } catch (OutOfMemoryError e) {
            twiController.alert("Error", "Out of Memory! [roti]");
            System.gc();
            return;
        }
        Graphics g = newImage.getGraphics();
//      public void drawRegion(Image src,
//                             int x_src,
//                             int y_src,
//                             int width,
//                             int height,
//                             int transform,
//                             int x_dest,
//                             int y_dest,
//                             int anchor)
        // out of memory if rotate all at one shot, so draw in 100x100 regions
        int transform;
        if (direction == ROTATE_CW) {
            transform = Sprite.TRANS_ROT90;
            for (int ychunk = 0; ychunk * 100 < h; ychunk++) {
                int destx = Math.max(0, h - ychunk * 100 - 100);
                for (int xchunk = 0; xchunk * 100 < w; xchunk++) {
                    int desty = xchunk * 100;
                    int regionh = Math.min(100, h - ychunk * 100);
                    int regionw = Math.min(100, w - xchunk * 100);
                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                            Graphics.TOP | Graphics.LEFT);
                }
            }
            // rotate center point (cx, cy) -> (h-cy, cx).  -> top left ref  (x-dispWidth/2, y-dispHeight/2) 
            // x+dispWidth/2, y+dispHeight/2 -rot-> h-(y+dispHeight/2), x+dispWidth/2 -TL-> 
            // h-y-(dispHeight+dispWidth)/2, x+(dispWidth-dispHeight)/2 
            int x0 = -x;
            int y0 = -y;
            x = h - y0 - (dispHeight + dispWidth) / 2;
            y = x0 + (dispWidth - dispHeight) / 2;
            x = -x;
            y = -y;
            orientation = (orientation + 1) % 4;
        } else if (direction == ROTATE_CCW) {
            transform = Sprite.TRANS_ROT270;
            for (int ychunk = 0; ychunk * 100 < h; ychunk++) {
                int destx = ychunk * 100;
                for (int xchunk = 0; xchunk * 100 < w; xchunk++) {
                    int desty = Math.max(0, w - xchunk * 100 - 100);
                    int regionh = Math.min(100, h - ychunk * 100);
                    int regionw = Math.min(100, w - xchunk * 100);
                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                            Graphics.TOP | Graphics.LEFT);
                }
            }
            int x0 = -x;
            int y0 = -y;
            y = w - x0 - (dispHeight + dispWidth) / 2;
            x = y0 + (dispHeight - dispWidth) / 2;
            x = -x;
            y = -y;
            orientation = (orientation - 1) % 4;
        } else {
            return;
        }
        im = newImage;
        w = im.getWidth();
        h = im.getHeight();
        newImage = null;
        System.gc();
        repaint();
    }

    protected void sizeChanged(int w, int h) {
        super.sizeChanged(w, h);
        dispHeight = h;
        dispWidth = w;
        xscroll = dispWidth / 3;
        yscroll = dispHeight / 3;
    }

    private void exit() {
        if (prevScreen != null) {
            twiController.setCurrent(prevScreen);
        }
        imoriginal = im = null;
        bimage = null;
        prevScreen = null;
        synchronized (this) {
            this.notifyAll();
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

    public void commandAction(Command c, Displayable d) {
        if (c == okCommand) {
            okReturn = true;
            exit();
        } else if (c == cancelCommand) {
            okReturn = false;
            exit();
        } else if (c == rotate) {
            rotateImage(ROTATE_CW);
        } else if (c == zoomin) {
            zoomin();
        } else if (c == zoomout) {
            zoomout();
        }
    }
}
