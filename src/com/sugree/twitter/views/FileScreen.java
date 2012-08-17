package com.sugree.twitter.views;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.utils.HttpUtil;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.TwitterException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemListener;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class FileScreen extends Form implements CommandListener, FileSystemListener {

    private TwitterController controller;
    Object url = null;
    private byte[] snapshotRaw;
    private boolean visible;
    private Displayable prevScreen = null;
    String picname = null;
    private static final String[] typeList = {"Regular File", "Directory"};
    private static final String[] monthList = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /* special string denotes upper directory */
    private static final String UP_DIRECTORY = "..";

    /* special string that denotes upper directory accessible by this browser.
     * this virtual directory contains all roots.
     */
    private static final String MEGA_ROOT = "/";

    /* separator string as defined by FC specification */
    private static final String SEP_STR = "/";

    /* separator character as defined by FC specification */
    private static final char SEP = '/';
    private String currDirName, currDirNameRollback = null;
    private Command view;
    private Command load = new Command("Load", Command.ITEM, 1);
    private Command creat;
    //add delete file functionality
    private Command delete = new Command("Delete", Command.ITEM, 3);
    private Command creatOK = new Command("OK", Command.OK, 1);
    private Command prop = new Command("Properties", Command.ITEM, 2);
    private Command back = new Command("Back", Command.BACK, 2);
    private Command exit = new Command("Exit", Command.EXIT, 3);
    private TextField nameInput; // Input field for new file name
    private ChoiceGroup typeInput; // Input field for file type (regular/dir)
    private Image dirIcon, fileIcon, yesIcon, noIcon;
    private Image[] iconList;
    private Hashtable fileSelectedIndex = new Hashtable();

    public FileScreen(TwitterController controller, Displayable screen, String title) throws Exception {
        super(title);
        this.controller = controller;
        this.prevScreen = screen;
        init();
    }

    public FileScreen(TwitterController controller, Displayable screen, String title, Object url) throws Exception {
        super(title);
        this.controller = controller;
        this.prevScreen = screen;
        this.url = url;
        init();
    }

    public FileScreen(TwitterController controller, String picname) throws Exception {
        super("Select File to Load/Save picture");
        this.controller = controller;
        this.picname = picname;
        this.prevScreen = controller.getCurrentScreen();
        init();
    }

    public FileScreen(TwitterController controller) throws Exception {
        super("Select existing file");
        this.controller = controller;
        this.prevScreen = controller.getCurrentScreen();
        controller.setFileName(null);
        init();
    }

    private void init() {
        /*  
         visibleCommand = new Command("Visible/Hide", Command.SCREEN, 5);
         addCommand(visibleCommand);
         setCommandListener(this);
         */
        String label = "View";
        if (url != null) {
            label = "SaveTo";
        }
        if (picname != null) {
            if (controller.getSnapshot() != null) {
                label = "SaveTo";
            } else {
                label = "View";
            }
        }
        view = new Command(label, Command.ITEM, 1);
        creat = new Command("Save/Create", Command.ITEM, 2);
        currDirName = MEGA_ROOT;
        try {
            dirIcon = Image.createImage("/icons/dir.png");
        } catch (IOException e) {
            dirIcon = null;
        }
        try {
            fileIcon = Image.createImage("/icons/file.png");
        } catch (IOException e) {
            fileIcon = null;
        }

        try {
            yesIcon = Image.createImage("/icons/yes.png");
        } catch (IOException e) {
            yesIcon = null;
        }

        try {
            noIcon = Image.createImage("/icons/no.png");
        } catch (IOException e) {
            noIcon = null;
        }

        iconList = new Image[]{fileIcon, dirIcon};
        FileSystemRegistry.addFileSystemListener(this);

    }

    public void start(boolean visibleFlag) {
        try {
            showCurrDirNow();
        } catch (SecurityException e) {
            alert("Error", "You are not authorized to access the restricted API", e);
        }
    }

    synchronized private void destroy() {
        FileSystemRegistry.removeFileSystemListener(this);
        if (prevScreen != null) {
            controller.setCurrent(prevScreen);
        }
        this.notifyAll();
    }

    public boolean imageFile(String fn) {
        return fn.endsWith(".png") || fn.endsWith(".jpg") || fn.endsWith(".jpeg") || fn.endsWith(".gif")
                || fn.endsWith(".PNG") || fn.endsWith(".JPG") || fn.endsWith(".JPEG") || fn.endsWith(".GIF");
    }

    public void commandAction(Command c, Displayable d) {
        try {
            String filename = null;
            if (d instanceof List) {
                List curr = (List) d;
                int sel = curr.getSelectedIndex();
                filename = curr.getString(sel);
                fileSelectedIndex.put(currDirName, new Integer(sel));
            }
            final String currFile = filename;
            final String fullFN = "file:///" + currDirName + currFile;
            if (c == load) {
                new Thread(
                        new Runnable() {
                            public void run() {
                                if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY)) {
                                    traverseDirectory(currFile);
                                } else if (picname == null) {
                                    controller.setFileName(fullFN);
                                    destroy();
                                } else if (readFile(currFile)) {
                                    controller.setSnapshot(snapshotRaw, currFile);
                                    snapshotRaw = null;
                                    destroy();
                                }
                            }
                        }).start();
            } else if (c == view) {
                new Thread(
                        new Runnable() {
                            public void run() {
                                if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY)) {
                                    traverseDirectory(currFile);
                                } else {
                                    if (saveUrl()) {
                                        try {
                                            FileConnection fc =
                                                    (FileConnection) Connector.open(fullFN, Connector.WRITE);
                                            doSaveUrl(fc);
                                        } catch (IOException e) {
                                            String s = "Can not write to file '" + currFile + "'";
                                            if ((e.getMessage() != null) && (e.getMessage().length() > 0)) {
                                                s += ("\n" + e);
                                            }
                                            Alert alert = new Alert("Error!", s, null, AlertType.ERROR);
                                            alert.setTimeout(Alert.FOREVER);
                                            controller.setCurrent(alert);
                                        }
                                    } else if (saveFile()) {
                                        doSaveFile(fullFN);
                                        destroy();
                                    } else if (picname != null) {
                                        if (controller.getSnapshot() != null) {
                                            deleteFile(currFile);
                                            createFile(currFile, false);
                                            destroy();
                                        } else {
                                            showFile(currFile);
                                        }
                                    } else {
                                        showFile(currFile);
                                    }
                                }
                            }
                        }).start();
            } else if (c == prop) {
                new Thread(new Runnable() {
                    public void run() {
                        showProperties(currFile);
                    }
                }).start();
            } else if (c == creat) {
                createFile();
            } else if (c == creatOK) {
                String newName = nameInput.getString();
                if ((newName == null) || newName.equals("")) {
                    Alert alert =
                            new Alert(
                            "Error!", "File Name is empty. Please provide file name", null,
                            AlertType.ERROR);
                    alert.setTimeout(Alert.FOREVER);
                    controller.setCurrent(alert);
                } else {
                    // Create file in a separate thread and disable all commands
                    // except for "exit"
                    executeCreateFile(newName, typeInput.getSelectedIndex() != 0);
                    removeCommand(creatOK);
                    removeCommand(back);
                }
            } else if (c == back) {
                showCurrDir();
            } else if (c == exit) {
                destroy();
            } else if (c == delete) {
                executeDelete(currFile);
            }
        } catch (SecurityException se) {
            alert("Error", "SecurityException", se);
        }
    }

    void delete(String currFile) {
        if (!currFile.equals(UP_DIRECTORY)) {
            if (currFile.endsWith(SEP_STR)) {
                checkDeleteFolder(currFile);
            } else {
                deleteFile(currFile);
                showCurrDir();
            }
        } else {
            Alert cantDeleteFolder =
                    new Alert(
                    "Error!",
                    "Can not delete The up-directory (..) " + "symbol! not a real folder", null,
                    AlertType.ERROR);
            cantDeleteFolder.setTimeout(Alert.FOREVER);
            controller.setCurrent(cantDeleteFolder);
        }
    }

    private void executeDelete(String currFile) {
        final String file = currFile;
        new Thread(
                new Runnable() {
                    public void run() {
                        delete(file);
                    }
                }).start();
    }

    private void checkDeleteFolder(String folderName) {
        try {
            FileConnection fcdir =
                    (FileConnection) Connector.open("file://localhost/" + currDirName + folderName);
            Enumeration content = fcdir.list("*", true);

            //only empty directory can be deleted
            if (!content.hasMoreElements()) {
                fcdir.delete();
                showCurrDir();
            } else {
                Alert cantDeleteFolder =
                        new Alert(
                        "Error!", "Can not delete The non-empty folder: " + folderName, null,
                        AlertType.ERROR);
                cantDeleteFolder.setTimeout(Alert.FOREVER);
                controller.setCurrent(cantDeleteFolder);
            }
        } catch (Exception e) {
            alert("Error", "fail to delete: " + currDirName + folderName, e);
        }
    }

    //Starts creatFile with another Thread
    private void executeCreateFile(final String name, final boolean val) {
        new Thread(
                new Runnable() {
                    public void run() {
                        createFile(name, val);
                    }
                }).start();
    }

    /**
     * Show file list in the current directory. Perform the action in different
     * thread not to block system calls by security questions.
     */
    void showCurrDir() {
        new Thread(new Runnable() {
            public void run() {
                showCurrDirNow();
            }
        }).start();
    }

    /**
     * Show file list in the current directory .
     */
    void showCurrDirNow() {
        Enumeration e;
        FileConnection currDir = null;
        List browser;
        try {
            if (MEGA_ROOT.equals(currDirName)) {
                e = FileSystemRegistry.listRoots();
                browser = new List(currDirName, List.IMPLICIT);
                while (e.hasMoreElements()) {
                    browser.append((String) e.nextElement(), dirIcon);
                }
            } else {
                currDir = (FileConnection) Connector.open("file://localhost/" + currDirName);
                e = currDir.list("*", true);
                browser = new List(currDirName, List.IMPLICIT);
                // not root - draw UP_DIRECTORY
                browser.append(UP_DIRECTORY, dirIcon);
            }
            while (e.hasMoreElements()) {
                String fileName = (String) e.nextElement();
                if (fileName.charAt(fileName.length() - 1) == SEP) {
                    // This is directory
                    browser.append(fileName, dirIcon);
                } else {
                    // this is regular file
                    browser.append(fileName, fileIcon);
                }
            }
            Integer selected = (Integer) fileSelectedIndex.get(currDirName);
            if (selected != null) {
                int sel = selected.intValue();
                if (browser.size() > sel) {
                    browser.setSelectedIndex(sel, true);
                }
            }
            browser.setSelectCommand(view);
            //Do not allow creating files/directories beside root
            if (!MEGA_ROOT.equals(currDirName)) {
                browser.addCommand(prop);
                if (url == null) {
                    browser.addCommand(load);
                    if (picname != null && controller.getSnapshot() != null) {
                        browser.addCommand(creat);
                    }
                } else {
                    browser.addCommand(creat);
                }
                browser.addCommand(delete);
            }
            browser.addCommand(exit);
            browser.setCommandListener(this);
            if (currDir != null) {
                currDir.close();
            }
            controller.setCurrent(browser);
        } catch (Exception ex) {
            if (currDirNameRollback != null) {
                currDirName = currDirNameRollback;
            }
            alert("Error", "showCurrDirNow", ex);
        }
        currDirNameRollback = null;
    }

    void traverseDirectory(String fileName) {
        /* In case of directory just change the current directory
         * and show it
         */
        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {
                // can not go up from MEGA_ROOT
                return;
            }
            currDirNameRollback = currDirName;
            currDirName = fileName;
        } else if (fileName.equals(UP_DIRECTORY)) {
            // Go up one directory
            currDirNameRollback = currDirName;
            int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);
            if (i != -1) {
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = MEGA_ROOT;
            }
        } else {
            currDirNameRollback = currDirName;
            currDirName = currDirName + fileName;
        }
        showCurrDir();
    }

    void showFile(String fileName) {
        String fullFN = "file://localhost/" + currDirName + fileName;
        FileConnection fc = null;
        InputStream fis = null;
        try {
            fc = (FileConnection) Connector.open(fullFN);
            if (!fc.exists()) {
                throw new IOException("File does not exists");
            }
            fis = fc.openInputStream();
            if (imageFile(fileName)) {
                try {
                    final CanvasScreen cs = new CanvasScreen(fis, fullFN, this, controller, false);
                    new Thread(new Runnable() {
                        public void run() {
                            synchronized (cs) {
                                try {
                                    cs.wait();
                                } catch (InterruptedException ie) {
                                }
                                showCurrDirNow();
                            }
                        }
                    }).start();
                } catch (TwitterException e) {
                    showCurrDirNow();
                    Alert alert =
                            new Alert(
                            "Error!",
                            "Can not show file " + fullFN
                            + "\nException: " + e.getMessage(), null, AlertType.ERROR);
                    alert.setTimeout(Alert.FOREVER);
                    controller.setCurrent(alert);
                }
            } else {
                byte[] b = new byte[1024];
                int length = fis.read(b, 0, 1024);
                TextBox viewer = new TextBox(
                        "View File: " + fileName, null, 1024,
                        TextField.ANY | TextField.UNEDITABLE);
                viewer.addCommand(back);
                viewer.addCommand(exit);
                viewer.setCommandListener(this);
                if (length > 0) {
                    viewer.setString(new String(b, 0, length));
                }
                controller.setCurrent(viewer);
            }
        } catch (Exception e) {
            Alert alert =
                    new Alert(
                    "Error!",
                    "Can not access file " + fullFN + "\nException: "
                    + e.toString() + ", " + e.getMessage(), null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            controller.setCurrent(alert);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ioe) {
            }
            try {
                if (fc != null) {
                    fc.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    boolean readFile(String fileName) {
        try {
            FileConnection fc =
                    (FileConnection) Connector.open("file://localhost/" + currDirName + fileName);
            if (!fc.exists()) {
                throw new IOException("File does not exists");
            }
            InputStream fis = fc.openInputStream();
            int fs = (int) fc.fileSize();
            byte[] b = new byte[fs];

            int length = fis.read(b, 0, fs);
            snapshotRaw = b;

            fis.close();
            fc.close();

            TextBox viewer =
                    new TextBox(
                    "View File: " + fileName, null, 1024,
                    TextField.ANY | TextField.UNEDITABLE);

            viewer.addCommand(back);
            viewer.addCommand(exit);
            viewer.setCommandListener(this);

            if (length > 0) {
                viewer.setString(new String(b, 0, length));
            }
            controller.setCurrent(viewer);
        } catch (Exception e) {
            Alert alert =
                    new Alert(
                    "Error!",
                    "Can not access file " + fileName + " in directory " + currDirName
                    + "\nException: " + e.getMessage(), null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            controller.setCurrent(alert);
        }
        return true;
    }

    void deleteFile(String fileName) {
        try {
            FileConnection fc = (FileConnection) Connector.open("file:///" + currDirName + fileName);
            fc.delete();
        } catch (Exception e) {
            Alert alert =
                    new Alert(
                    "Error!",
                    "Can not access/delete file " + fileName + " in directory " + currDirName
                    + "\nException: " + e.getMessage(), null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            controller.setCurrent(alert);
        }
    }

    void showProperties(String fileName) {
        try {
            if (fileName.equals(UP_DIRECTORY)) {
                return;
            }

            FileConnection fc =
                    (FileConnection) Connector.open("file://localhost/" + currDirName + fileName);

            if (!fc.exists()) {
                throw new IOException("File does not exists");
            }

            Form props = new Form("Properties: " + fileName);

            props.append(new StringItem("Location:", currDirName));
            props.append(new StringItem("Type: ", fc.isDirectory() ? "Directory" : "Regular File"));
            props.append(new StringItem("Modified:", myDate(fc.lastModified())));
            props.append(new StringItem("Size:", "" + fc.fileSize()));
            props.append("Attributes:\n");
            props.append(fc.canRead() ? yesIcon : noIcon);
            props.append("Read\n");
            props.append(fc.canWrite() ? yesIcon : noIcon);
            props.append("Write\n");
            props.append(fc.isHidden() ? yesIcon : noIcon);
            props.append("Hidden");

            props.addCommand(back);
            props.addCommand(exit);
            props.setCommandListener(this);
            fc.close();
            controller.setCurrent(props);
        } catch (Exception e) {
            Alert alert =
                    new Alert(
                    "Error!",
                    "Can not access file " + fileName + " in directory " + currDirName
                    + "\nException: " + e.getMessage(), null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            controller.setCurrent(alert);
        }
    }

    private boolean saveUrl() {
        return url != null && url instanceof String;
    }

    private boolean saveFile() {
        return url != null && url instanceof String[];
    }

    void createFile() {
        Form creator = new Form("New File");
        String proposedName = controller.getSnapshotFilename();
        if (picname != null) {
            proposedName = generatedFileName() + "_" + picname;
        } else {
            if (saveUrl()) {
                proposedName = ((String) url).substring(((String) url).lastIndexOf('/') + 1);
            } else if (saveFile()) {
                proposedName = generatedFileName();
                String suffix = ((String[]) url)[0];
                if (suffix != null) {
                    proposedName += suffix;
                }
            }
        }
        nameInput = new TextField("Enter Name", proposedName, 256, TextField.ANY);
        typeInput = new ChoiceGroup("Enter File Type", Choice.EXCLUSIVE, typeList, iconList);
        creator.append(nameInput);
        creator.append(typeInput);
        creator.addCommand(creatOK);
        creator.addCommand(back);
        creator.addCommand(exit);
        creator.setCommandListener(this);
        controller.setCurrent(creator);
    }

    void createFile(String newName, boolean isDirectory) {
        try {
            FileConnection fc = (FileConnection) Connector.open("file:///" + currDirName + newName);
            if (isDirectory) {
                fc.mkdir();
            } else {
                fc.create();
                if (saveUrl()) {
                    doSaveUrl(fc);
                    return;
                } else if (saveFile()) {
                    doSaveFile(fc.getURL());
                    destroy();
                    return;
                } else {
                    byte[] ss = controller.getSnapshot();
                    if (ss != null) {
                        try {
                            OutputStream os = fc.openOutputStream();
                            os.write(ss);
                            os.close();
                            destroy();
                        } catch (IOException ioe) {
                            alert("Error", "Write File Error", ioe);
                        }
                        return;
                    }
                }
            }
            showCurrDir();
        } catch (Exception e) {
            alert("Error", "Can not create file '" + newName + "'", e);
            // Restore the commands that were removed in commandAction()
            addCommand(creatOK);
            addCommand(back);
        }
    }

    private String myDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));
        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        sb.append(cal.get(Calendar.MINUTE));
        sb.append(':');
        sb.append(cal.get(Calendar.SECOND));
        sb.append(',');
        sb.append(' ');
        sb.append(cal.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        sb.append(monthList[cal.get(Calendar.MONTH)]);
        sb.append(' ');
        sb.append(cal.get(Calendar.YEAR));
        return sb.toString();
    }

    private String generatedFileName() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.YEAR));
        sb.append('-');
        sb.append(monthList[cal.get(Calendar.MONTH)]);
        sb.append('-');
        sb.append(cal.get(Calendar.DAY_OF_MONTH));
        sb.append('_');
        sb.append(cal.get(Calendar.HOUR_OF_DAY));
        sb.append('-');
        sb.append(cal.get(Calendar.MINUTE));
//        sb.append('-');
//        sb.append(cal.get(Calendar.SECOND));
        return sb.toString();
    }

    public void rootChanged(int state, String rootName) {
        showCurrDir();
    }

    private void alert(String title, String description, Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.length() > 0) {
            msg = msg.toString() + ":\n" + msg;
        } else {
            msg = msg.toString();
        }
        Alert alert = new Alert(title, description + "\n" + msg, null, AlertType.ERROR);
        alert.setTimeout(Alert.FOREVER);
        controller.setCurrent(alert);
    }

    private void doSaveFile(String urL) {
        ((String[]) url)[0] = urL;
    }

    private void doSaveUrl(final FileConnection fc) throws IOException {
//        Alert alert = new Alert("Wait..", "Saving.. " + url + " -> " + fc.getName(), null, AlertType.INFO);
//        alert.setTimeout(Alert.FOREVER);
//        controller.setCurrent(alert);
        final String act = "Saving " + url + " -> " + fc.getName();
        WaitScreen wait = new WaitScreen(controller, new AbstractTask() {
            public void doTask() {
                try {
                    int count = HttpUtil.saveImageToStream(((String) url), controller.maxPicSize(), fc.openOutputStream());
                    fc.close();
                } catch (IOException io) {
                    controller.alert("Error", act, io);
                } catch (SecurityException se) {
                    controller.alert("Error", act, se);
                } catch (OutOfMemoryError ofm) {
                    controller.alert("Error", act, ofm);
                }
                synchronized (fc) {
                    fc.notifyAll();
                }

            }
        }, this);
        wait.println(act + "..");
        wait.start();
        controller.setCurrent(wait);
        synchronized (fc) {
            try {
                fc.wait();
            } catch (InterruptedException ie) {
            }
        }
        showCurrDir();
    }
}
