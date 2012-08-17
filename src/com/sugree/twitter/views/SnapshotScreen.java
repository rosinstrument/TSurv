/**
 * optimized thumbnail code is written by Kamanashis Roy.
 *
 * http://miniim.blogspot.com/2008/05/image-thumbnail-in-optimized-way-for.html
 */
package com.sugree.twitter.views;

import com.substanceofcode.infrastructure.Device;
import com.substanceofcode.twitter.Settings;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.RecordControl;
import javax.microedition.media.control.VideoControl;

public class SnapshotScreen extends Form implements CommandListener, PlayerListener {

    private TwitterController controller;
    private Player player;
    private VideoControl videoControl = null;
    private Item videoItem = null, actionDecriptionItem = null;
    private String status;
    private byte[] snapshotRaw;
    private boolean visible;
    private Command stopCommand, snapCommand, okCommand, cancelCommand, visibleCommand = null;
    private String currentLocator;
    private static Vector badLocator = new Vector();
    private int videoItemId = -1;
    private String recordLocator = null, recordContentType = null;
    private int mediaType = 0; // video 1, audio 2
    private Thread recMedia = null;
    private FileConnection fileConnection;
    private OutputStream fcOutputStream;

    public SnapshotScreen(TwitterController controller, String status, String mediaFileLocator) throws Exception {
        super("");
        if (mediaFileLocator != null) {
            if (mediaFileLocator.startsWith("audio:")) {
                mediaType = 2;
                recordLocator = mediaFileLocator.substring(6);
            } else {
                mediaType = 1;
                recordLocator = mediaFileLocator;
            }
        }
        setTitle("Get a " + mediaString());
        this.controller = controller;
        this.status = status;
        try {
            init();
        } catch (Exception me) {
            destroy();
            throw me;
        }
        okCommand = new Command("OK", Command.OK, 3);
        addCommand(cancelCommand = new Command("Cancel", Command.CANCEL, 3));
        stopCommand = new Command("StopRec", Command.OK, 1);
        snapCommand = new Command("Get " + mediaString(), Command.OK, 2);
        if (mediaType < 2) {
            visibleCommand = new Command("Visible/Hide", Command.SCREEN, 5);
        }
        snapCommands(true);
        setCommandListener(this);
    }

    private String mediaString() {
        String ret = mediaString(mediaType);
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        return "shot";
    }

    public static String mediaString(int mediaTypeId) {
        if (mediaTypeId == 1) {
            return "video";
        } else if (mediaTypeId == 2) {
            return "audio";
        }
        return "";
    }

    public void playerUpdate(Player player, String event, Object eventData) {
        //Log.info("playerUpdate " + event);
    }

    private void init() throws MediaException {
        boolean fullscreen = controller.getSettings().getBooleanProperty(Settings.SNAPSHOT_FULLSCREEN, false);
        player = null;
        if (mediaType == 0) {
            String locator = controller.getSettings().getStringProperty(Settings.CAPTURE_DEVICE, Device.getSnapshotLocator());
            Vector v = new Vector();
            v.addElement(locator);
            for (int i = 0; i < SetupScreen.captureDevicesValue.length; i++) {
                String cd = SetupScreen.captureDevicesValue[i];
                if (cd != null && !locator.equalsIgnoreCase(cd)) {
                    v.addElement(cd);
                }
            }
            Enumeration en = v.elements();
            String ne = null;
            while (en.hasMoreElements()) {
                ne = (String) en.nextElement();
                if (isBadLocator(ne)) {
                    continue;
                }
                Log.info("opening " + ne);
                synchronized (this) {
                    try {
                        player = Manager.createPlayer(ne);
                        break;
                    } catch (Exception e) {
                        Log.error("createPlayer(\"" + ne + "\"): " + e.toString());
                    }
                }
            }
            if (player == null) {
                throw new MediaException("error finding locator");
            } else {
                Log.info("Locator " + ne + " opened");
                currentLocator = ne;
            }
            if (locator == null || !locator.equalsIgnoreCase(ne)) {
                controller.getSettings().setStringProperty(Settings.CAPTURE_DEVICE, ne);
            }
        } else {
            try {
                player = Manager.createPlayer("capture://" + mediaString());
            } catch (IOException e) {
                throw new MediaException(e.getMessage());
            }
        }

        player.addPlayerListener(this);
        try {
            player.realize();
        } catch (Exception me) {
            Log.error(me.toString());
            throw new MediaException("error player.realize");
        }
        Log.info("realize() " + player.getState());
        if (mediaType == 2) {
            actionDecriptionItem = new StringItem(null, "Record Audio");
            actionDecriptionItem.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_BEFORE);
        } else {
            videoControl = (VideoControl) player.getControl("VideoControl");
            Log.info("getControl() " + videoControl);
            videoItem = (Item) videoControl.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, null);
            Log.info("initDisplayMode() " + videoItem);
            if (!fullscreen) {
                videoControl.setDisplaySize(getHeight() * videoControl.getSourceWidth()
                        / videoControl.getSourceHeight(), getHeight());
                videoItem.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
                videoItem.setPreferredSize(getHeight() * videoControl.getSourceWidth()
                        / videoControl.getSourceHeight(), getHeight());
            }
            if (fullscreen) {
                try {
                    videoControl.setDisplayFullScreen(true);
                } catch (MediaException e) {
                    Log.error(e.toString());
                }
            }
        }
    }

    public void start(boolean visibleFlag) {
        deleteAll();
        if (videoItem != null) {
            videoItemId = append(videoItem);
            visible = visibleFlag;
            videoControl.setVisible(visible);
        } else if (actionDecriptionItem != null) {
            append(actionDecriptionItem);
        }
        try {
            player.start();
        } catch (Exception e) {
            Log.error("start() " + e.toString());
        }
    }

    private void destroy() {
        if (videoControl != null) {
            videoControl.setVisible(visible = false);
            videoControl = null;
        }
        if (videoItemId != -1) {
            delete(videoItemId);
            videoItemId = -1;
        }
        videoItem = null;
        destroyPlayer();
    }

    void defplayer() throws MediaException {
        if (player != null) {
            if (player.getState() == Player.STARTED) {
                player.stop();
            }
            if (player.getState() == Player.PREFETCHED) {
                player.deallocate();
            }
            if (player.getState() == Player.REALIZED
                    || player.getState() == Player.UNREALIZED) {
                player.close();
            }
        }
        player = null;
    }

    private void destroyPlayer() {
        try {
            defplayer();
        } catch (MediaException me) {
            Log.error("destroyPlayer exception: " + me.getMessage());
        }
        player = null;
    }

    public void quickSnapshot() throws Exception {
        quickSnapshot(false);
    }

    public void quickSnapshot(boolean background) throws Exception {
        String encoding = controller.getSettings().getStringProperty(Settings.SNAPSHOT_ENCODING, null);
        if (encoding != null && encoding.length() == 0) {
            encoding = null;
        }
        controller.setSnapshot(snapshotRaw = null);
        if (!background) {
            controller.vibrate(100);
        }
        try {
            snapshotRaw = videoControl().getSnapshot(encoding);
            controller.setSnapshot(snapshotRaw);
        } catch (Exception me) {
            Log.error("getSnapshot(encoding=\"" + encoding + "\"): " + me.toString());
            badLocator.addElement(currentLocator);
            destroy();
            snapshotRaw = null;
            throw me;
        }
        destroy();
        if (!background) {
            controller.showUpdate(status);
        }
    }

    private Object[] getSnapshot() throws MediaException {
        String encoding = controller.getSettings().getStringProperty(Settings.SNAPSHOT_ENCODING, null);
        Boolean resize =
                controller.getSettings().
                getBooleanProperty(Settings.RESIZE_THUMBNAIL, false)
                ? Boolean.TRUE : Boolean.FALSE;
        Object[] ret = {null, resize};
        if (encoding != null && encoding.length() == 0) {
            encoding = null;
        }
        controller.setSnapshot(snapshotRaw = null);
        try {
            controller.vibrate(10);
            snapshotRaw = videoControl().getSnapshot(encoding);
            controller.vibrate(100);
        } catch (Exception e) {
            Log.error("getSnapshot(\"" + encoding + "\") " + e.toString());
        }
        snapCommands(false);
        addCommand(okCommand);
        destroy();
        if (snapshotRaw == null) {
            badLocator.addElement(currentLocator);
            throw new MediaException("getSnaphot() error");
        }
        Image snapshotImage = null;
        try {
            snapshotImage = Image.createImage(snapshotRaw, 0, snapshotRaw.length);
            if (resize.booleanValue()) {
                snapshotImage = createThumbnail(snapshotImage);
            }
        } catch (OutOfMemoryError e) {
            Log.error("create image: " + e.toString());
            StringItem si = new StringItem("", e.toString());
            si.setLayout(Item.LAYOUT_VCENTER | Item.LAYOUT_CENTER);
            append(si);
            ret[0] = snapshotImage;
            ret[1] = Boolean.FALSE;
            return ret;
        } catch (Exception ex) {
            Log.error("create image: " + ex.toString());
            ret[0] = snapshotImage;
            ret[1] = Boolean.FALSE;
            return ret;
        }
        ret[0] = snapshotImage;
        return ret;
    }

    public RecordControl getRecordControl() {
        try {
            if (player != null) {
                return (RecordControl) player.getControl("RecordControl");
            }
        } catch (IllegalStateException ise) {
            Log.error(ise.toString());
        }
        return null;
    }

    private void startRecording(String locator) throws MediaException {
        if (locator != null) {
            recordLocator = locator;
        }
        RecordControl rc = getRecordControl();
        if (rc != null) {
            try {
                rc.setRecordSizeLimit(1024 * 1024);
            } catch (MediaException me) {
                Log.error(me.getMessage());
            }
            try {
                rc.stopRecord();
                rc.reset();
                Log.info("recording to " + recordLocator);
                fileConnection = (FileConnection) Connector.open(recordLocator, Connector.WRITE);
//                rc.setRecordLocation(recordLocator);
                rc.setRecordStream(fcOutputStream = fileConnection.openOutputStream());
                rc.startRecord();
            } catch (IOException io) {
                throw new MediaException(io.getMessage());
            }
        } else {
            throw new MediaException("Could not get RecordControl!");
        }
    }

    private void stopRecording() {
        try {
            final RecordControl rc = getRecordControl();
            if (rc != null) {
                rc.stopRecord();
                recordContentType = rc.getContentType();
                controller.vibrate(10);
                // The commit() must be performed in a thread that is not this one:
                // we are running on the event dispatcher thread.
                // rc.commit() MIGHT display a security dialog (depending on the URL
                // that the audio is being recorded to) and the MIDP spec recommends
                // performing actions that could bring up a security dialog
                // on a thread that is NOT the event dispatcher thread.
                new Thread(
                        new Runnable() {
                            public void run() {
                                try {
                                    rc.commit();
                                    fcOutputStream.flush();
                                    fcOutputStream.close();
                                    fileConnection.close();
                                    Log.info("Recorded " + recordLocator + " successfully.");
                                } catch (IOException e) {
                                    Log.error(e.getMessage());
                                } catch (SecurityException e) {
                                }
                                synchronized (rc) {
                                    rc.notifyAll();
                                }
                            }
                        }).start();
                synchronized (rc) {
                    rc.wait();
                }
            } else {
                throw new MediaException("Could not get RecordControl!");
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    private String getMedia() {
        try {
            controller.vibrate(10);
            player.stop();
            startRecording(null);
            player.start();
            if (recMedia != null) {
                try {
                    synchronized (recMedia) {
                        recMedia.wait(600000);
                    }
                } catch (InterruptedException ie) {
                    Log.info("interrupted sleep");
                }
            }
            recMedia = null;
            stopRecording();
            player.close();
        } catch (MediaException me) {
            Log.error(me.toString());
            return null;
        } catch (SecurityException me) {
            Log.error(me.toString());
            return null;
        } finally {
            destroy();
        }
        return recordLocator;
    }

    public void commandAction(Command cmd, final Displayable display) {
        if (cmd == okCommand) {
            destroy();
            controller.setSnapshot(snapshotRaw);
            controller.showUpdate(status);
        } else if (cmd == stopCommand) {
            if (recMedia != null) {
                synchronized (recMedia) {
                    recMedia.notifyAll();
                }
            }
        } else if (cmd == snapCommand) {
            if (mediaType > 0) {
                recMedia = new Thread(new Runnable() {
                    public void run() {
                        String locator = getMedia();
                        controller.showUpdate(status, locator, recordContentType);
                    }
                });
                addCommand(stopCommand);
                removeCommand(snapCommand);
                recMedia.start();
                return;
            } else {
                boolean resize;
                Image snap;
                try {
                    Object[] o = getSnapshot();
                    resize = ((Boolean) o[1]).booleanValue();
                    snap = (Image) o[0];
                } catch (Exception ex) {
                    Log.error("getSnapshot: " + ex.toString());
                    destroy();
                    controller.showUpdate(status);
                    return;
                }
                try {
                    if (snap == null) {
                        return;
                    }
                    if (resize) {
                        ImageItem item =
                                new ImageItem("", snap,
                                Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_SHRINK, null);
                        append(item);
                        setTitle("Confirm?");
                    } else {
                        final CanvasScreen lps = new CanvasScreen(snap, "Confirm?",
                                SnapshotScreen.this, controller, true);
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    synchronized (lps) {
                                        lps.wait();
                                    }
                                    commandAction(lps.okReturn ? okCommand : cancelCommand, display);
                                } catch (InterruptedException e) {
                                    Log.error(e.toString());
                                }
                            }
                        }).start();
                    }
                } catch (Exception me) {
                    Log.error("confirmSnapshot: " + me.toString());
                    destroy();
                    if (snapshotRaw != null) {
                        controller.setSnapshot(snapshotRaw);
                    }
                    controller.showUpdate(status);
                }
            }
        } else if (cmd == cancelCommand) {
            destroy();
            snapshotRaw = null;
            controller.showUpdate(status);
        } else if (cmd == visibleCommand) {
            try {
                videoControl().setVisible(visible = !visible);
            } catch (Exception me) {
                Log.error("setVisible error: " + me.toString());
            }
        }
    }

    Image createThumbnail(Image im) {
        return createThumbnail(im, this);
    }

    public static Image createThumbnail(Image image, Displayable display) {
        int sw = image.getWidth();
        int sh = image.getHeight();
        int pw = display.getWidth(); //ph * sw / sh;
        int ph = sh * pw / sw;
        for (int i = 0; i <= 4; i++) {
            int pws = pw >> i, phs = ph >> i;
            if (pws < 1 || phs < 1) {
                break;
            }
            try {
                return getThumbnailWrapper(image, pws, phs, 0);
            } catch (OutOfMemoryError ofm) {
                Log.error("OutOfMem: getThumbnailWrapper(" + pws + "," + phs + ")");
            }
        }
        throw new OutOfMemoryError("Unable to create thumbnail");
    }

    /**
     * Gets the thumbnail that fit with given screen width, height and padding.
     *
     * @param image The source image
     * @param padding padding to the screen
     * @return scaled image
     */
    private static Image getThumbnailWrapper(Image image, int expectedWidth, int expectedHeight, int padding) {
        final int sourceWidth = image.getWidth();
        final int sourceHeight = image.getHeight();
        int thumbWidth, thumbHeight;

        // big width
        if (sourceWidth >= sourceHeight) {
            thumbWidth = expectedWidth - padding;
            thumbHeight = thumbWidth * sourceHeight / sourceWidth;
            // fits to height ?
            if (thumbHeight > (expectedHeight - padding)) {
                thumbHeight = expectedHeight - padding;
                thumbWidth = thumbHeight * sourceWidth / sourceHeight;
            }
        } else {
            // big height
            thumbHeight = expectedHeight - padding;
            thumbWidth = thumbHeight * sourceWidth / sourceHeight;
            // fits to width ?
            if (thumbWidth > (expectedWidth - padding)) {
                thumbWidth = expectedWidth - padding;
                thumbHeight = thumbWidth * sourceHeight / sourceWidth;
            }
        }

        // XXX As we do not have floating point, sometimes the thumbnail resolution gets bigger ...
        // we are trying hard to avoid that ..
        thumbHeight = (sourceHeight < thumbHeight) ? sourceHeight : thumbHeight;
        thumbWidth = (sourceWidth < thumbWidth) ? sourceWidth : thumbWidth;

        //return getThumbnail(image, thumbWidth, thumbHeight);
        Log.debug("scaleImage(" + thumbWidth + "," + thumbHeight + ")");
        //return org.kobjects.lcdui.ScaleImage.scaleImage(image, thumbWidth, thumbHeight);
        return resizeImage(image, thumbWidth, thumbHeight);
    }

    private VideoControl videoControl() throws MediaException {
        if (videoControl == null) {
            destroy();
            try {
                init();
            } catch (MediaException me) {
                destroy();
                throw me;
            }
        }
        return videoControl;
    }

    private boolean isBadLocator(String ne) {
        Enumeration e = badLocator.elements();
        while (e.hasMoreElements()) {
            if (((String) e.nextElement()).equalsIgnoreCase(ne)) {
                return true;
            }
        }
        return false;
    }

    public static Image resizeImage(Image image, int resizedWidth, int resizedHeight) {
        int width = image.getWidth(), height = image.getHeight();
        int[] in = new int[width];
        int i = 0;
        int dy, dx;
        int[] out = new int[resizedWidth * resizedHeight];
        for (int y = 0; y < resizedHeight; y++) {
            dy = y * height / resizedHeight;
            image.getRGB(in, 0, width, 0, dy, width, 1);
            for (int x = 0; x < resizedWidth; x++) {
                dx = x * width / resizedWidth;
                out[(resizedWidth * y) + x] = in[dx];
            }
        }
        Image resized = Image.createRGBImage(out, resizedWidth, resizedHeight, true);
        return resized;
    }

    private void snapCommands(boolean b) {
        if (b) {
            addCommand(snapCommand);
            if (mediaType < 2) {
                addCommand(visibleCommand);
            }
        } else {
            removeCommand(snapCommand);
            if (mediaType < 2) {
                removeCommand(visibleCommand);
            }
        }
    }
}
