package com.sugree.twitter.views;

import com.substanceofcode.twitter.Settings;
import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.Log;
import com.substanceofcode.utils.StringUtil;
import com.substanceofcode.utils.TimeUtil;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.TwitterException;
import com.sugree.twitter.tCoResolver;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

public class StatusScreen extends Form implements CommandListener, ItemCommandListener {

    private TwitterController controller;
    private Status status;
    private Command replyCommand, directCommand, exitCommand, bookmarksCommand;
    private Command retweetCommand, nextStatusCommand, prevStatusCommand;
    private Command favoriteCommand, followCommand, unfollowCommand;
    private Command backCommand, linkItemCommand, forceImgCommand;
    private Command aboutUserCommand, userTimelineCommand,
            deleteCommand = new Command("Delete", Command.SCREEN, 10);
    private Thread imgThread = null;
    private Hashtable commandToImage = new Hashtable();

    public StatusScreen(TwitterController controller) {
        super("");
        this.controller = controller;
        addCommand(backCommand = new Command("Back", Command.BACK, 1));
        addCommand(replyCommand = new Command("Reply", Command.SCREEN, 4));
        addCommand(nextStatusCommand = new Command("UP", Command.SCREEN, 3));
        addCommand(prevStatusCommand = new Command("DOWN", Command.SCREEN, 3));
        addCommand(retweetCommand = new Command("Retweet", Command.SCREEN, 5));
        addCommand(favoriteCommand = new Command("ToggleFavorite", Command.SCREEN, 6));
        addCommand(aboutUserCommand = new Command("About user", Command.SCREEN, 7));
        addCommand(userTimelineCommand = new Command("UserLine", Command.SCREEN, 7));
        addCommand(followCommand = new Command("Follow", Command.SCREEN, 7));
        addCommand(unfollowCommand = new Command("Unfollow", Command.SCREEN, 7));
        addCommand(directCommand = new Command("DirectMessage", Command.SCREEN, 8));
        addCommand(exitCommand = new Command("Exit", Command.SCREEN, 19));
        addCommand(bookmarksCommand = new Command("Bookmarks", Command.SCREEN, 20));
        addCommand(deleteCommand);
        linkItemCommand = new Command("Go", Command.SCREEN, 6);
        forceImgCommand = new Command("LoadPics", Command.SCREEN, 2);
        setCommandListener(this);
    }

    public void redraw() {
        setStatus(status, false);
    }

    public void setStatus(Status status, boolean forceImg) {
        boolean resize = controller.getSettings().getBooleanProperty(Settings.RESIZE_THUMBNAIL, false);
        this.status = status;
        setTitle(status.getScreenName());
        Date now = new Date();
        now.setTime(now.getTime() - controller.getServerTimeOffset());
        Date date = status.getDate();
        String interval;
        if (now.getTime() > date.getTime()) {
            interval = TimeUtil.getTimeInterval(date, now) + " ago";
        } else {
            interval = TimeUtil.getTimeInterval(now, date) + " future";
        }
        deleteAll();
        commandToImage.clear();
        Vector mediaEntities = status.getMediaUrl();
        String chunks_separators[][] = StringUtil.splitSpace(status.getText());
        String[] chunks = chunks_separators[0], separators = chunks_separators[1];
        StringItem si = null;
        Hashtable mediaUrls = new Hashtable();
        Vector items2append = new Vector();
        for (int i = 0; i < chunks.length; i++) {
            String chunk = chunks[i], separator = separators[i];
            if (StringUtil.isUrl(chunk) || StringUtil.isHash(chunk)) {
                if (tCoResolver.isTCo(chunk)) {
                    chunk = tCoResolver.resolve(chunk);
                }
                if (StringUtil.isImgUrl(chunk)) {
                    mediaUrls.put(chunk, "");
                }
                si = new StringItem("", chunk, Item.HYPERLINK);
                si.addCommand(linkItemCommand);
                si.setItemCommandListener(this);
                items2append.addElement(si);
                if (separator != null) {
                    items2append.addElement(new StringItem("", separator, Item.PLAIN));
                }
            } else {
                if (separator == null) {
                    separator = "";
                }
                if (si == null || (si.getAppearanceMode() & Item.HYPERLINK) != 0) {
                    si = new StringItem("", chunk + separator, Item.PLAIN);
                    items2append.addElement(si);
                } else {
                    si.setText(si.getText() + chunk + separator);
                }
            }
        }
        if (mediaEntities != null) {
            for (int i = 0; i < mediaEntities.size(); i++) {
                mediaUrls.put(mediaEntities.elementAt(i), "");
            }
        }
        final Enumeration mue = mediaUrls.keys();
        si = new StringItem("",
                String.valueOf(controller.timeline.getSelectedIndex() + 1)
                + "/" + controller.timeline.size()
                + "/" + interval + "/" + status.getSource()
                + (status.getFavorited() ? "/fav" : "")
                + (status.getFollowing() ? "/fol" : "")
                + (mue.hasMoreElements() ? "/I" : ""));
        si.setDefaultCommand(nextStatusCommand);
        si.setItemCommandListener(this);
        append(si);
        for (int i = 0; i < items2append.size(); i++) {
            append((Item) items2append.elementAt(i));
        }
        items2append.removeAllElements();
        if (mue.hasMoreElements()) {
            if (resize || forceImg) {
                if (imgThread != null) {
                    imgThread.interrupt();
                }
                removeCommand(forceImgCommand);
                final long start_sid = StatusScreen.this.status.getId();
                imgThread = new Thread(
                        new Runnable() {
                            public void run() {
                                int id = append("\nloading pic(s)..");
                                while (mue.hasMoreElements()) {
                                    String u = (String) mue.nextElement();
                                    Command cm = new Command("Full size", Command.SCREEN, 9);
                                    try {
                                        byte[] bim = HttpUtil.getImageBytes(u,
                                                controller.maxPicSize());
                                        long sid = StatusScreen.this.status.getId();
                                        if (Thread.currentThread() != imgThread
                                                || sid != start_sid) {
                                            return;
                                        }
                                        commandToImage.put(cm, new ImgURL(bim, u));
                                        Image im = Image.createImage(bim, 0, bim.length);
                                        try {
                                            im = SnapshotScreen.createThumbnail(im, StatusScreen.this);
                                        } catch (OutOfMemoryError e) {
                                            Log.error("resize error");
                                        }
                                        delete(id);
                                        ImageItem ii = new ImageItem(null, im, Item.LAYOUT_CENTER, "", Item.HYPERLINK);
                                        ii.setDefaultCommand(cm);
                                        ii.setItemCommandListener(StatusScreen.this);
                                        append(ii);
                                    } catch (OutOfMemoryError ofm) {
                                        Log.error(ofm.toString());
                                        append(" Failed: out of mem");
                                    } catch (IOException ioe) {
                                        Log.error(ioe.toString());
                                        append(" Failed: " + ioe.getMessage());
                                    } catch (Exception e) {
                                        Log.error(e.toString());
                                        append(" Failed: " + e.toString());
                                    } finally {
                                        final String cmdDesc = "Save to File";
                                        Command cms = new Command(cmdDesc, Command.SCREEN, 9);
                                        commandToImage.put(cms, new ImgURL(null, u));
                                        StringItem si = new StringItem(null, cmdDesc, Item.HYPERLINK);
                                        si.addCommand(cms);
                                        si.setItemCommandListener(StatusScreen.this);
                                        append(si);
                                    }

                                }
                            }
                        });
                imgThread.start();
            } else {
                addCommand(forceImgCommand);
            }
        } else {
            removeCommand(forceImgCommand);
        }
    }

    public void commandAction(Command cmd, Item item) {
        if (cmd == nextStatusCommand) {
            controller.timeline.nextSelected();
        } else if (cmd == prevStatusCommand) {
            controller.timeline.prevSelected();
        } else if (cmd == linkItemCommand) {
            final String url = ((StringItem) item).getText();
            Log.info(cmd.toString() + ": " + url);
            if (StringUtil.isHash(url)) {
                controller.newSearch(url);
            } else if (StringUtil.isImgUrl(url)) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            final CanvasScreen cs = new CanvasScreen(url, url, StatusScreen.this, controller, true);
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        synchronized (cs) {
                                            cs.wait();
                                        }
                                        if (!cs.okReturn) {
                                            controller.openUrl(url);
                                        }
                                    } catch (InterruptedException e) {
                                        Log.error(e.toString());
                                    }
                                }
                            }).start();
                        } catch (TwitterException te) {
                            controller.alert("Error", "Showing picture", te);
                            controller.openUrl(url);
                        }
                    }
                }).start();
            } else {
                controller.openUrl(url);
            }
        } else {
            ImgURL iu = (ImgURL) commandToImage.get(cmd);
            if (iu.img != null) {
                try {
                    CanvasScreen lps = new CanvasScreen(iu.img, iu.url, this, controller, false);
                } catch (TwitterException te) {
                    controller.alert("Error", "Loading image URL", te);
                }
            } else if (iu.url != null) {
                try {
                    FileScreen fs = new FileScreen(controller, this, "Save pic", iu.url);
                    fs.start(true);
                } catch (Exception e) {
                    Log.error(e.toString());
                } catch (OutOfMemoryError ofm) {
                    Log.error(ofm.toString());
                }
            }
        }
    }

    public Status getStatus() {
        return status;
    }

    public void commandAction(Command cmd, Displayable display) {
        if (cmd == backCommand) {
            controller.showTimeline();
        } else if (cmd == replyCommand) {
            controller.showReply(status);
        } else if (cmd == directCommand) {
            controller.showUpdate("d " + status.getScreenName() + " ");
        } else if (cmd == aboutUserCommand) {
            AboutUserScreen about = new AboutUserScreen(status, this, controller);
        } else if (cmd == userTimelineCommand) {
            controller.newSearch("@" + status.getScreenName());
        } else if (cmd == followCommand) {
            controller.Follow(status);
        } else if (cmd == unfollowCommand) {
            controller.unFollow(status);
        } else if (cmd == deleteCommand) {
            controller.timeline.Delete(status, -1);
        } else if (cmd == nextStatusCommand) {
            controller.timeline.nextSelected();
        } else if (cmd == prevStatusCommand) {
            controller.timeline.prevSelected();
        } else if (cmd == retweetCommand) {
            controller.showUpdate(status.getRetweet(controller.getStatusMaxLength()));
        } else if (cmd == favoriteCommand) {
            controller.toggleFavorited(status);
        } else if (cmd == forceImgCommand) {
            setStatus(status, true);
        } else if (cmd == exitCommand) {
            controller.exit();
        } else if (cmd == bookmarksCommand) {
            controller.goToBookmarks();


        }
    }

    private static class ImgURL {

        public byte[] img;
        public String url;

        public ImgURL(byte[] im, String u) {
            img = im;
            url = u;
        }
    }
}
