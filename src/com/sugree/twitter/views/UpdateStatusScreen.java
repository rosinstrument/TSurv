package com.sugree.twitter.views;

import com.substanceofcode.infrastructure.Device;
import com.substanceofcode.twitter.Settings;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.TwitterException;
import com.sugree.utils.Location;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class UpdateStatusScreen extends Form implements CommandListener, ItemStateListener {

    private TwitterController controller;
    private Command sendCommand, cancelCommand, insertCommand, snapshotCommand;
    private Command quickSnapshotCommand, fileSnapshotCommand, videoCommand, audioCommand;
    private Command gpsCommand, reverseGeocoderCommand, showPicCommand, setupCommand;
    private Command cellIdCommand, statCommand, squeezeCommand, attachFileCommand;
    private Command cleanAllCommand;
    private final String LAST_COMMAND = "last_command";
    private TextField textField = null;
    private String attachFile = null, attachMimeType = null;
    private int idStat = -1;

    public UpdateStatusScreen(TwitterController controller, String text) {
        super("Tweet this:");
        this.controller = controller;
        Settings settings = controller.getSettings();
        addCommand(sendCommand = new Command("Send", Command.OK, 1));
        addCommand(cancelCommand = new Command("Cancel", Command.CANCEL, 2));
        addCommand(insertCommand = new Command("InsertText", Command.SCREEN, 3));
        addCommand(videoCommand = new Command("VideoFile", Command.SCREEN, 4));
        addCommand(audioCommand = new Command("AudioFile", Command.SCREEN, 4));
        addCommand(snapshotCommand = new Command("GetShot", Command.SCREEN, 4));
        addCommand(quickSnapshotCommand = new Command("TweetShot", Command.SCREEN, 5));
        addCommand(fileSnapshotCommand = new Command("PicFile", Command.SCREEN, 5));
        addCommand(attachFileCommand = new Command("AttachFile", Command.SCREEN, 5));
        addCommand(cleanAllCommand = new Command("CleanAll", Command.SCREEN, 5));
        showPicCommand = new Command("ShowPic", Command.SCREEN, 5);
        addCommand(setupCommand = new Command("Setup", Command.SCREEN, 16));
        if (System.getProperty("microedition.location.version") != null && settings.getBooleanProperty(Settings.ENABLE_GPS, true)) {
            addCommand(gpsCommand = new Command("GPS Location", Command.SCREEN, 6));
        }
        if (settings.getBooleanProperty(Settings.ENABLE_REVERSE_GEOCODER, true)) {
            addCommand(reverseGeocoderCommand = new Command("Rev Geo Loc", Command.SCREEN, 7));
        }
        if (settings.getBooleanProperty(Settings.ENABLE_CELL_ID, true)) {
            addCommand(cellIdCommand = new Command("Cell ID Loc", Command.SCREEN, 8));
        }
        addCommand(statCommand = new Command("Statistics", Command.SCREEN, 9));
        if (settings.getBooleanProperty(Settings.ENABLE_SQUEEZE, true)) {
            addCommand(squeezeCommand = new Command("Squeeze", Command.SCREEN, 10));
        }
        append(textField = new TextField("", "", 200, 0));
        setString(text);
        setCommandListener(this);
        setItemStateListener(this);
    }

    public final UpdateStatusScreen makeScreen() {
        deleteAll();
        idStat = -1;
        append(textField);
        appendStat();
        return this;
    }

    public UpdateStatusScreen refresh() {
        appendStat();
        return this;
    }

    public UpdateStatusScreen clean() {
        controller.setSnapshot(null);
        attachMimeType = attachFile = attachStat = null;
        textFieldEmpty();
        appendStat();
        return this;
    }
    private String attachStat = null;

    private String attachStat() {
        if (attachStat == null) {
            String attach = getAttachFile();
            if (attach != null) {
                try {
                    FileConnection fc = (FileConnection) Connector.open(attach, Connector.READ);
                    attachStat = "file: " + fc.getName() + ", " + fc.fileSize() + " bytes";
                } catch (IOException io) {
                } catch (SecurityException se) {
                }
            }
        }
        return attachStat;
    }

    private void appendStat() {
        StringItem si = new StringItem("", getStat());
        if (idStat == -1) {
            idStat = append(si);
        } else {
            set(idStat, si);
        }
        if (controller.getSnapshot() == null && getAttachFile() == null) {
            removeCommand(showPicCommand);
        } else {
            addCommand(showPicCommand);
        }
    }

    public void showStat() {
        Alert stat = new Alert("Statistics",
                getStat(), null, AlertType.INFO);
        stat.setTimeout(Alert.FOREVER);
        controller.setCurrent(stat);
    }

    public String getStat() {
        int maxChars = controller.getStatusMaxLength();
        String text = getString();
        int lenChars = text.length();
        int lenBytes = lenChars;
        try {
            lenBytes = new String(text.getBytes("UTF-8"), "ISO-8859-1").length();
        } catch (UnsupportedEncodingException e) {
        }
        String statText = lenChars + " chars, " + (maxChars - lenChars) + " left";
        byte b[] = controller.getSnapshot();
        if (b != null) {
            statText += ", image: " + b.length + " bytes";
        }
        String as = attachStat();
        if (as != null) {
            statText += ", " + as;
        }
        return statText + ", " + controller.memStat();
    }

    public void insert(String text) {
        textField.insert(text, textField.getCaretPosition());
    }

    public void commandAction(Command cmd, Displayable display) {
        try {
            if (cmd == sendCommand) {
                String comm = textField.getString();
                if (comm != null) {
                    String attach = getAttachFile();
                    if (attach != null) {
                        controller.updateStatus(comm, attach, getAttachFileMimeType());
                    } else {
                        controller.updateStatus(comm);
                    }
                    controller.getSettings().setStringProperty(LAST_COMMAND, comm);
                    Log.verbose(comm);
                    controller.getInsert().addWord(comm, 0);
                }
            } else if (cmd == showPicCommand) {
                //LargePicScreen lps = new LargePicScreen(controller.getSnapshot(), "update.jpg", this, controller, false);
                if (controller.getSnapshot() != null) {
                    try {
                        CanvasScreen lps = new CanvasScreen(controller.getSnapshot(), "update.jpg", this, controller, false);
                    } catch (TwitterException te) {
                        controller.alert("Error", "Loading snapshot", te);
                    }
                } else if (getAttachFile() != null) {
                    try {
                        CanvasScreen lps = new CanvasScreen(getAttachFile(), "update.jpg", this, controller, false);
                    } catch (TwitterException te) {
                        controller.alert("Error", "Loading attached file", te);
                    }
                }
            } else if (cmd == cancelCommand) {
                controller.showTimeline();
            } else if (cmd == cleanAllCommand) {
                clean();
            } else if (cmd == insertCommand) {
                controller.showInsert();
            } else if (cmd == videoCommand) {
                showMedia(1);
            } else if (cmd == audioCommand) {
                showMedia(2);
            } else if (cmd == snapshotCommand) {
                controller.showSnapshot();
            } else if (cmd == quickSnapshotCommand) {
                controller.quickSnapshot();
            } else if (cmd == fileSnapshotCommand) {
                fileSnapshot();
            } else if (cmd == attachFileCommand) {
                attachFile();
            } else if (cmd == gpsCommand) {
                controller.insertLocation(Location.MODE_GPS);
            } else if (cmd == reverseGeocoderCommand) {
                controller.insertLocation(Location.MODE_REVERSE_GEOCODER);
            } else if (cmd == cellIdCommand) {
                controller.insertLocation(Location.MODE_CELL_ID);
            } else if (cmd == statCommand) {
                showStat();
            } else if (cmd == setupCommand) {
                controller.showSetup();
            } else if (cmd == squeezeCommand) {
                textField.setString(controller.squeezeText(textField.getString()));
            }
        } catch (Throwable t) {
            controller.alert("Error", "error executing command", t);
        }
    }

    public final void setString(String text) {
        if (textField != null && text != null) {
            textField.setString(text);
        }
        appendStat();
    }

    public void setString(String text, String file) {
        attachFile = file;
        attachStat = null;
        setString(text);
    }

    public void setString(String text, String file, String mimetype) {
        if (file != null) {
            controller.setSnapshot(null);
        }
        attachMimeType = mimetype;
        setString(text, file);
    }

    public String getString() {
        return textField == null ? null : textField.getString();
    }

    public void setMaxSize(int statusMaxLength) {
        if (textField != null) {
            textField.setMaxSize(statusMaxLength);
        }
    }

    public void itemStateChanged(Item item) {
        if (item instanceof TextField) {
            appendStat();
        }
    }

    public String getAttachFile() {
        return attachFile;
    }

    public String getAttachFileMimeType() {
        return attachMimeType;
    }

    public void fileSnapshot() {
        try {
            final FileScreen fs = new FileScreen(controller, "tsurv.jpg");
            fs.start(true);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        synchronized (fs) {
                            fs.wait();
                        }
                        setString(getString());
                    } catch (InterruptedException e) {
                        Log.error(e.toString());
                    }
                }
            }).start();
        } catch (Exception e) {
            controller.showError(e);
        }
    }

    public void attachFile() {
        try {
            final FileScreen fs = new FileScreen(controller);
            fs.start(true);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        synchronized (fs) {
                            fs.wait();
                        }
                        String fn = controller.getFileName();
                        if (fn != null) {
                            setString(null, fn, "application/octet-stream");
                        } else {
                            setString(getString());
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.toString());
                    }
                }
            }).start();
        } catch (Exception e) {
            controller.showError(e);
        }
    }

    public void showMedia(final int type) {
        try {
            String ext = "." + mediaFileExtension(type);
            final String[] fn = {ext};
            final FileScreen fs = new FileScreen(controller, controller.getCurrentScreen(), "Save media to file..", fn);
            fs.start(true);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        synchronized (fs) {
                            fs.wait();
                        }
                        if (fn[0] != null) {
                            try {
                                SnapshotScreen ss = new SnapshotScreen(controller,
                                        getString(), type == 2 ? "audio:" + fn[0] : fn[0]);
                                controller.setCurrent(ss);
                                ss.start(true);
                            } catch (Exception e) {
                                Log.error(e.getMessage());
                            }
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.toString());
                    }
                }
            }).start();
        } catch (Exception e) {
            controller.showError(e);
        }
    }

    private void saveText() {
        if (textField != null) {
            String t = textField.getString();
            if (t != null && t.length() > 0) {
                controller.getInsert().addWord(t, 0);
            }
        }
    }

    private void textFieldEmpty() {
        if (textField != null) {
            saveText();
            textField.setString("");
        }
    }

    private static String mediaFileExtension(int type) {
        String[] ext = Device.mediaFileExtensions(type);
        if (ext != null && ext.length > 0) {
            return ext[0];
        }
        return "";
    }
}
