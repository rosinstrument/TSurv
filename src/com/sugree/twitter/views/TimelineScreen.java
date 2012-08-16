package com.sugree.twitter.views;

import com.rosinstrument.twi.Comparator;
import com.rosinstrument.twi.QuickSort;
import com.substanceofcode.twitter.Settings;
import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.tasks.RequestTimelineTask;
import com.sugree.utils.DateUtil;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

public class TimelineScreen extends List implements CommandListener {

    private final int TIMER_INTERVAL = 10;
    private TwitterController controller;
    private Command readCommand, replyCommand, directCommand, updateCommand,
            quickSnapshotCommand, startSnapshotSeriesCommand, allCommand, refreshCommand, homeTimelineCommand, publicTimelineCommand,
            userTimelineCommand, repliesTimelineCommand, directTimelineCommand,
            favoritesTimelineCommand, setupCommand, logCommand, aboutCommand,
            minimizeCommand, exitCommand, bookmarksCommand, saveBookmarkCommand,
            deleteCommand;
    private int refreshInterval;
    private int timeLeft;
    private Timer refreshTimer;
    private TimerTask refreshTask;
    private boolean autoUpdate;
    private Vector statuses;
    private long selectedStatus;
    private int length;
    private String title;
    private boolean showCounter;
    private boolean restoreStatusFlag = false;

    public TimelineScreen(TwitterController controller) {
        super("TSurv", Choice.IMPLICIT);
        this.controller = controller;
        Settings settings = controller.getSettings();
        boolean swapMinimizeRefresh = settings.getBooleanProperty(Settings.SWAP_MINIMIZE_REFRESH, false);
        selectedStatus = 0;
        statuses = new Vector();
        title = getTitle();
        showCounter = false;
        refreshInterval = 0;
        if (swapMinimizeRefresh) {
            refreshCommand = new Command("RenewLine", Command.EXIT, 20);
        } else {
            refreshCommand = new Command("RenewLine", Command.ITEM, 2);
        }
        addCommand(refreshCommand);
        addCommand(allCommand = new Command("RefreshLine", Command.ITEM, 2));
        setSelectCommand(readCommand = new Command("Read", Command.ITEM, 7));
        replyCommand = new Command("Reply", Command.ITEM, 2);
        deleteCommand = new Command("Delete", Command.ITEM, 3);
        directCommand = new Command("DirectMessage", Command.ITEM, 2);
        addCommand(updateCommand = new Command("Tweet", Command.SCREEN, 6));
        addCommand(homeTimelineCommand = new Command("HomeLine", Command.SCREEN, 8));
        addCommand(repliesTimelineCommand = new Command("RepliesLine", Command.SCREEN, 9));
        addCommand(directTimelineCommand = new Command("DirectLine", Command.SCREEN, 10));
        addCommand(favoritesTimelineCommand = new Command("FavoritesLine", Command.SCREEN, 11));
        addCommand(userTimelineCommand = new Command("MyLine", Command.SCREEN, 12));
        addCommand(publicTimelineCommand = new Command("EveryoneLine", Command.SCREEN, 13));
        addCommand(quickSnapshotCommand = new Command("QShot", Command.ITEM, 1));
        addCommand(startSnapshotSeriesCommand = new Command("TweetShotSeries", Command.SCREEN, 22));
        addCommand(setupCommand = new Command("Setup", Command.SCREEN, 16));
        addCommand(logCommand = new Command("Log", Command.SCREEN, 17));
        addCommand(aboutCommand = new Command("About", Command.SCREEN, 18));
        addCommand(exitCommand = new Command("Exit", Command.SCREEN, 19));
        addCommand(bookmarksCommand = new Command("Bookmarks", Command.SCREEN, 25));
        saveBookmarkCommand = new Command("SaveBookmark", Command.SCREEN, 26);
        if (!swapMinimizeRefresh) {
            minimizeCommand = new Command("Minimize", Command.EXIT, 30);
            addCommand(minimizeCommand);
        }
        setCommandListener(this);
    }

    private void saveSelected() {
        int index = getSelectedIndex();
        if (index >= 0 && index < statuses.size()) {
            selectedStatus = ((Status) statuses.elementAt(index)).getId();
        }
    }

    private void restoreSelected() {
        if (size() <= 0 || statuses.size() <= 0) {
            return;
        }
        int lastIndex = findStatus(selectedStatus);
        if (lastIndex >= size() || lastIndex < 0) {
            lastIndex = size() - 1;
        }
        setSelectedIndex(lastIndex, true);
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setShowCounter(boolean showCounter) {
        this.showCounter = showCounter;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.timeLeft = this.refreshInterval = refreshInterval;
    }

    public void setRefresh(boolean enable) {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        if (enable) {
            refreshTimer = new Timer();
            refreshTask = new RefreshTask();
            refreshTimer.scheduleAtFixedRate(refreshTask, TIMER_INTERVAL * 1000, TIMER_INTERVAL * 1000);
        } else {
            refreshTask = null;
            refreshTimer = null;
            setTitle(title);
        }
    }

    public void setAutoUpdate(boolean enable) {
        autoUpdate = enable;
    }

    public void clearTimeline() {
        statuses.removeAllElements();
        updateTimeline();
    }

    public void addTimeline(Vector timeline, boolean retro) {
        saveSelected();
        final Hashtable ht = new Hashtable();
        for (int i = 0; i < statuses.size(); i++) {
            Status s = (Status) statuses.elementAt(i);
            ht.put(new Long(s.getId()).toString(), s);
        }
        for (int i = 0; i < timeline.size(); i++) {
            Status s = (Status) timeline.elementAt(i);
            ht.put(new Long(s.getId()).toString(), s);
        }
        Vector v = new Vector();
        Enumeration e = ht.keys();
        while (e.hasMoreElements()) {
            v.addElement(e.nextElement());
        }
        QuickSort.sort(v, new Comparator() {
            public int compareTo(String s1, String s2) {
                long l1 = ((Status) ht.get(s1)).getDate().getTime(),
                        l2 = ((Status) ht.get(s2)).getDate().getTime();
                if (l1 == l2) {
                    return s1.compareTo(s2);
                } else if (l1 < l2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        statuses.removeAllElements();
        int start = 0, end = v.size() - 1;
        if (end >= length) {
            if (retro) {
                end = start + length - 1;
            } else {
                start = end - length + 1;
            }
        }
        if (start <= end) {
            for (int i = end; i >= start; i--) {
                statuses.addElement(ht.get(v.elementAt(i)));
            }
        }
        statuses.trimToSize();
        updateTimeline();
        restoreSelected();
    }

    public void update(Status status) {
        long id = status.getId();
        int index = findStatus(id);
        if (index >= 0) {
            statuses.setElementAt(status, index);
            Displayable dsp = controller.getScreen(TwitterController.SCREEN_STATUS);
            if (dsp instanceof StatusScreen) {
                if (((StatusScreen) dsp).getStatus().getId() == id) {
                    ((StatusScreen) dsp).setStatus(status, false);
                }
            }
            saveSelected();
            updateTimeline();
            restoreSelected();
        }
    }

    public String getLastDate() {
        if (statuses != null && statuses.size() > 0) {
            Status status = (Status) statuses.elementAt(0);
            return DateUtil.formatHTTPDate(status.getDate());
        }
        return "";
    }

    public String getLastId() {
        if (statuses != null && statuses.size() > 0) {
            Status status = (Status) statuses.elementAt(0);
            return String.valueOf(status.getId());
        }
        return "";
    }

    public String getFirstId() {
        if (statuses != null && statuses.size() > 0) {
            Status status = (Status) statuses.elementAt(statuses.size() - 1);
            return String.valueOf(status.getId());
        }
        return "";
    }

    public Vector getTimeline() {
        return statuses;
    }

    private int findStatus(long status) {
        int iless = -1;
        long lless = -1;
        for (int i = 0; i < statuses.size(); i++) {
            long lsid = ((Status) statuses.elementAt(i)).getId();
            if (lsid == status) {
                return i;
            } else if (lsid < status) {
                if (iless == -1 || lless < lsid) {
                    lless = lsid;
                    iless = i;
                }
            }
        }
        return iless;
    }

    private int findStatus(Status status) {
        long id = status.getId();
        for (int i = 0; i < statuses.size(); i++) {
            long lsid = ((Status) statuses.elementAt(i)).getId();
            if (lsid == id) {
                return i;
            }
        }
        return -1;
    }

    private void updateTimeline() {
        deleteAll();
        Enumeration statusEnum = statuses.elements();
        while (statusEnum.hasMoreElements()) {
            Status status = (Status) statusEnum.nextElement();
            Vector mu = status.getMediaUrl();
            append(((mu != null && !mu.isEmpty()) ? "I/" : "")
                    + status.getScreenName() + ": " + status.getText(), null);
        }
        timeLeft = refreshInterval;
    }

    public void restoreStatus() {
        if (getSelectedIndex() >= 0) {
            addCommand(replyCommand);
        } else {
            removeCommand(replyCommand);
            removeCommand(deleteCommand);
        }
        if (restoreStatusFlag) {
            restoreStatusFlag = false;
        } else {
            return;
        }
        if (statuses != null && !statuses.isEmpty()) {
            restoreSelected();
            int index = getSelectedIndex();
            controller.showStatus((Status) statuses.elementAt(index));
        }
    }

    public int nextSelected() {
        int index = getSelectedIndex();
        if (index == 0) {
            saveSelected();
            restoreStatusFlag = true;
            controller.fetchTimeline(controller.getCurrentFeedType());
        } else if (index > 0) {
            Status status = (Status) statuses.elementAt(--index);
            if (index >= 0 && index < statuses.size()) {
                selectedStatus = ((Status) statuses.elementAt(index)).getId();
                if (index >= 0 && index < size()) {
                    setSelectedIndex(index, true);
                    saveSelected();
                } else {
                    Log.error("setSelectedIndex: " + index + "/" + size());
                }
            }
            if (index == 0) {
                controller.vibrate(100);
            }
            controller.showStatus(status);
        }
        return index;
    }

    public int prevSelected() {
        int index = getSelectedIndex();
        if (index >= size() - 1) {
            saveSelected();
            restoreStatusFlag = true;
            controller.fetchPrevTimeline(controller.getCurrentFeedType());
        } else if (index >= 0) {
            Status status = (Status) statuses.elementAt(++index);
            if (index < statuses.size()) {
                selectedStatus = ((Status) statuses.elementAt(index)).getId();
                if (index < size()) {
                    setSelectedIndex(index, true);
                    saveSelected();
                } else {
                    Log.error("setSelectedIndex: " + index + "/" + size());
                }
            }
            if (index >= size() - 1) {
                controller.vibrate(100);
            }
            controller.showStatus(status);
        }
        return index;
    }

    public void Delete(Status stat, int index) {
        if (index < 0) {
            index = findStatus(stat);
        }
        controller.Delete(stat, index);
    }

    public void commandAction(Command cmd, Displayable display) {
        int index = getSelectedIndex();
        if (cmd == readCommand && index >= 0) {
            Status status = (Status) statuses.elementAt(index);
            controller.showStatus(status);
        }
        if (cmd == replyCommand && index >= 0) {
            controller.showReply((Status) statuses.elementAt(index));
        }
        if (cmd == deleteCommand && index >= 0) {
            Status status = (Status) statuses.elementAt(index);
            Delete(status, index);
        } else if (cmd == directCommand && index >= 0) {
            Status status = (Status) statuses.elementAt(index);
            controller.showUpdate("d " + status.getScreenName() + " ");
        } else if (cmd == updateCommand) {
            controller.showUpdate();
        } else if (cmd == quickSnapshotCommand) {
            controller.quickSnapshot2("");
        } else if (cmd == startSnapshotSeriesCommand) {
            controller.startSnapshotSeries();
        } else if (cmd == refreshCommand) {
            controller.fetchTimeline(controller.getCurrentFeedType());
        } else if (cmd == allCommand) {
            int ft = controller.getCurrentFeedType();
            RequestTimelineTask.refreshLine(ft, controller.searchTerm);
            controller.clearTimeline();
            controller.fetchTimeline(ft);
        } else if (cmd == homeTimelineCommand) {
            setTitleName(cmd.getLabel());
            removeCommand(directCommand);
            addCommand(replyCommand);
            removeCommand(deleteCommand);
            controller.fetchTimeline(RequestTimelineTask.FEED_HOME);
        } else if (cmd == publicTimelineCommand) {
            setTitleName(cmd.getLabel());
            removeCommand(directCommand);
            addCommand(replyCommand);
            removeCommand(deleteCommand);
            controller.fetchTimeline(RequestTimelineTask.FEED_PUBLIC);
        } else if (cmd == userTimelineCommand) {
            setTitleName(cmd.getLabel());
            removeCommand(directCommand);
            addCommand(replyCommand);
            addCommand(deleteCommand);
            controller.fetchTimeline(RequestTimelineTask.FEED_USER);
        } else if (cmd == repliesTimelineCommand) {
            setTitleName(cmd.getLabel());
            removeCommand(directCommand);
            addCommand(replyCommand);
            removeCommand(deleteCommand);
            controller.fetchTimeline(RequestTimelineTask.FEED_REPLIES);
        } else if (cmd == directTimelineCommand) {
            setTitleName(cmd.getLabel());
            removeCommand(replyCommand);
            removeCommand(deleteCommand);
            addCommand(directCommand);
            controller.fetchTimeline(RequestTimelineTask.FEED_DIRECT);
        } else if (cmd == favoritesTimelineCommand) {
            setTitleName(cmd.getLabel());
            removeCommand(deleteCommand);
            removeCommand(directCommand);
            addCommand(replyCommand);
            controller.fetchTimeline(RequestTimelineTask.FEED_FAVORITES);
        } else if (cmd == setupCommand) {
            controller.showSetup();
        } else if (cmd == logCommand) {
            controller.showLog();
        } else if (cmd == aboutCommand) {
            controller.showAbout();
        } else if (cmd == minimizeCommand) {
            controller.minimize();
        } else if (cmd == exitCommand) {
            controller.exit();
        } else if (cmd == saveBookmarkCommand) {
            controller.saveBookmark(controller.searchTerm);
        } else if (cmd == bookmarksCommand) {
            controller.goToBookmarks();
        }
    }

    public void setTitleName(String s) {
        setTitle(s);
        title = s;
    }

    public void addBookmarkCommand(String searchTerm) {
        if (searchTerm != null) {
            addCommand(saveBookmarkCommand);
        } else {
            removeCommand(saveBookmarkCommand);
        }
    }

    public void deleteStatus(Status status, int index) {
        if (statuses.size() - 1 >= index && status.getId() == ((Status) statuses.elementAt(index)).getId()) {
            delete(index);
            statuses.removeElementAt(index);
        }
    }

    private class RefreshTask extends TimerTask {

        public final void run() {
            if (refreshInterval > 0
                    && controller.getScreen(controller.SCREEN_CURRENT)
                    == controller.getScreen(controller.SCREEN_TIMELINE)) {
                timeLeft -= TIMER_INTERVAL;
                if (timeLeft <= 0) {
                    timeLeft = refreshInterval;
                    if (autoUpdate) {
                        controller.updateStatus();
                    } else {
                        controller.fetchTimeline(controller.getCurrentFeedType(), true);
                    }
                } else if (showCounter && timeLeft % 10 == 0) {
                    setTitle(title + " " + timeLeft);
                }
            }
        }
    }
}
