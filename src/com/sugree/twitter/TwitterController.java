package com.sugree.twitter;

import com.substanceofcode.twitter.Settings;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.Log;
import com.substanceofcode.utils.StringUtil;
import com.sugree.twitter.tasks.OAuthTask;
import com.sugree.twitter.tasks.QuickSnapshotTask;
import com.sugree.twitter.tasks.RequestObjectTask;
import com.sugree.twitter.tasks.RequestTimelineTask;
import com.sugree.twitter.tasks.UpdateStatusTask;
import com.sugree.twitter.views.BookmarksScreen;
import com.sugree.twitter.views.InsertScreen;
import com.sugree.twitter.views.LinkScreen;
import com.sugree.twitter.views.LogScreen;
import com.sugree.twitter.views.OAuthScreen;
import com.sugree.twitter.views.SetupScreen;
import com.sugree.twitter.views.SnapshotScreen;
import com.sugree.twitter.views.StatusScreen;
import com.sugree.twitter.views.TimelineScreen;
import com.sugree.twitter.views.UpdateStatusScreen;
import com.sugree.twitter.views.WaitScreen;
import com.sugree.utils.DateUtil;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.rms.RecordStoreException;

public class TwitterController {

    public static final int SCREEN_CURRENT = 0;
    public static final int SCREEN_TIMELINE = 1;
    public static final int SCREEN_UPDATE = 2;
    public static final int SCREEN_STATUS = 3;
    public static final int START_EMPTY_TIMELINE = 0;
    public static final int START_UPDATE = 1;
    public static final int START_HOME_TIMELINE = 2;
    public static final int START_REPLIES_TIMELINE = 4;
    private TwitterMIDlet midlet;
    public TwitterApi api;
    private TwitterConsumer oauth;
    private Display display;
    private Settings settings;
    private byte[] snapshot;
    public Image thumbnail = null;
    private String filename = null;
    private long replyTo;
    public TimelineScreen timeline;
    private UpdateStatusScreen update;
    private StatusScreen status;
    private InsertScreen insert;
    private int currentFeedType;
    private long serverTimeOffset;
    private final String[] squeezeFrom = {"   ", " "};
    private final String[] squeezeTo = {" ", " "};
    public Hashtable lastPostHeaders = null;
    private boolean autoSetSnaphotsFrequencyFlag = false;
    public String searchTerm = null;
    private BookmarksScreen bookMarks = null;
    private Displayable screenCurrentSet = null;

    public TwitterController(TwitterMIDlet midlet) {
        try {
            this.midlet = midlet;
            HttpUtil.setUserAgent(
                    TwitterMIDlet.NAME + "/" + TwitterMIDlet.VERSION
                    + " (" + System.getProperty("microedition.platform") + ")"
                    + " Profile/"
                    + System.getProperty("microedition.profiles")
                    + " Configuration/"
                    + System.getProperty("microedition.configuration"));
            display = Display.getDisplay(midlet);
            api = new TwitterApi(TwitterMIDlet.NAME);
            settings = Settings.getInstance(midlet);
            oauth = new TwitterConsumer(
                    PrivateData.OAUTH_CONSUMER_KEY,
                    PrivateData.OAUTH_CONSUMER_SECRET);
            timeline = new TimelineScreen(this);
            update = new UpdateStatusScreen(this, "");
            insert = new InsertScreen(this);
            status = new StatusScreen(this);
            loadSettings();
            currentFeedType = -1;
        } catch (IOException e) {
            Log.error(e.toString());
        } catch (RecordStoreException e) {
            Log.error(e.toString());
        } catch (Error e) {
            Log.error(e.toString());
        }
    }

    public final void loadSettings() {
        Log.setLogOff(settings.getBooleanProperty(Settings.LOG_OFF, false));
        api.setUsername(settings.getStringProperty(Settings.USERNAME, ""));
        api.setPassword(settings.getStringProperty(Settings.PASSWORD, ""));
        api.setGateway(settings.getStringProperty(Settings.GATEWAY, TwitterApi.DEFAULT_GATEWAY));
        api.setPictureGateway(settings.getStringProperty(Settings.PICTURE_GATEWAY, TwitterApi.DEFAULT_PICTURE_GATEWAY));
        api.setOptimizeBandwidth(settings.getBooleanProperty(Settings.OPTIMIZE_BANDWIDTH, true));
        api.setAlternateAuthentication(settings.getBooleanProperty(Settings.ALTERNATE_AUTHEN, false));
        api.setForceNoHost(settings.getBooleanProperty(Settings.FORCE_NO_HOST, false));
        api.setGzip(settings.getBooleanProperty(Settings.ENABLE_GZIP, true));
        api.setCount(settings.getIntProperty(Settings.TIMELINE_LENGTH, 20));
        insert.setCustom(StringUtil.split(settings.getStringProperty(Settings.CUSTOM_WORDS, "#testing,@twidiff"), ","));
        insert.setWords(null);
        timeline.setLength(settings.getIntProperty(Settings.TIMELINE_LENGTH, 20));
        timeline.setShowCounter(settings.getBooleanProperty(Settings.ENABLE_REFRESH_COUNTER, false));
        timeline.setRefreshInterval(settings.getIntProperty(Settings.REFRESH_INTERVAL, 120));
        timeline.setRefresh(settings.getBooleanProperty(Settings.ENABLE_REFRESH, true));
        timeline.setAutoUpdate(settings.getBooleanProperty(Settings.ENABLE_AUTO_UPDATE, false));
        if (settings.getBooleanProperty(Settings.WRAP_TIMELINE, false)) {
            timeline.setFitPolicy(Choice.TEXT_WRAP_ON);
        } else {
            timeline.setFitPolicy(Choice.TEXT_WRAP_OFF);
        }
        update.setMaxSize(getStatusMaxLength());
        oauth.loadRequestToken(settings);
        oauth.loadAccessToken(settings);
        api.setOAuth(oauth);
        processHack();
    }

    public Settings getSettings() {
        return settings;
    }

//	private boolean isStatusLengthMax() {
//		return settings.getBooleanProperty(Settings.STATUS_LENGTH_MAX, false);
//	}
    public int getStatusMaxLength() {
        // statusMaxLength : 160 (hard limit, non-standard)
        //                 : 140 (per spec, default)
        return settings.getBooleanProperty(Settings.STATUS_LENGTH_MAX, false) ? 160 : 140;
    }

    public void processHack() {
        String[] hacks = StringUtil.split(settings.getStringProperty(Settings.HACK, "").toLowerCase(), " ");
        for (int i = 0; i < hacks.length; i++) {
            if (hacks[i].equals("alterauth")) {
                api.setAlternateAuthentication(true);
            } else if (hacks[i].equals("noalterauth")) {
                api.setAlternateAuthentication(false);
            }
        }
    }

    public void minimize() {
        try {
            display.setCurrent(null);
            midlet.pauseApp();
        } catch (Exception e) {
            showTimeline();
        }
    }

    public void openUrl(String url) {
        try {
            midlet.platformRequest(url);
        } catch (ConnectionNotFoundException e) {
            Log.error(e.toString());
        }
    }

    public void exit() {
        try {
            settings.save(false);
        } catch (Exception e) {
        }
        try {
            midlet.destroyApp(true);
        } catch (Exception e) {
        }
    }

    private Vector extractWords(Vector statuses) {
        Vector words = new Vector();
        for (int i = 0; i < statuses.size(); i++) {
            Status s = (Status) statuses.elementAt(i);
            if (!words.contains("@" + s.getScreenName())) {
                words.addElement("@" + s.getScreenName());
            }
            //String[] splited = StringUtil.split(s.getText(), " ");
            String chunks_separators[][] = StringUtil.splitSpace(s.getText());
            String[] splited = chunks_separators[0];
            for (int j = 0; j < splited.length; j++) {
                if (splited[j].length() > 5) {
                    if (splited[j].charAt(0) == '@'
                            || splited[j].charAt(0) == '#') {
                        if (!words.contains(splited[j])) {
                            words.addElement(splited[j]);
                        }
                    }
                }
            }
        }
        return words;
    }

    public String squeezeText(String text) {
        text = text.trim();
        for (int i = 0; i < squeezeFrom.length; i++) {
            text = StringUtil.replace(text, squeezeFrom[i], squeezeTo[i]);
        }
        return text;
    }

    public void setSnapshot(byte[] raw) {
        snapshot = raw;
        thumbnail = null;
        filename = null;
    }

    public void setSnapshot(byte[] raw, String filen) {
        snapshot = raw;
        thumbnail = null;
        filename = filen;
    }

    public void setSnapshot(byte[] raw, Image thumb) {
        snapshot = raw;
        thumbnail = thumb;
        if (thumb != null) {
            update.refresh();
        }
    }

    public byte[] getSnapshot() {
        return snapshot;
    }

    public String getSnapshotFilename() {
        return filename;
    }

    public void setReplyTo(long id) {
        replyTo = id;
    }

    public void setServerTimeOffset(long offset) {
        serverTimeOffset = offset;
    }

    public long getServerTimeOffset() {
        return serverTimeOffset;
    }

    public int getCurrentFeedType() {
        return currentFeedType;
    }

    public void updateTimeline(Status s) {
        timeline.update(s);
    }

    public void addTimeline(Vector statuses, boolean alert, boolean retro) {
        timeline.addTimeline(statuses, retro);
        Vector words = extractWords(timeline.getTimeline());
        insert.setWords(words);
        if (statuses.size() > 0 && alert) {
            if (settings.getBooleanProperty(Settings.ENABLE_REFRESH_ALERT, true)) {
                AlertType.ALARM.playSound(display);
            }
            if (settings.getBooleanProperty(Settings.ENABLE_REFRESH_VIBRATE, true)) {
                vibrate(100);
            }
        }
    }

    public void vibrate(int i) {
        Display d = getCurrent();
        if (d != null) {
            d.vibrate(i);
        }
    }

    public String getLastStatus() {
        return timeline.getLastDate();
    }

    public String getLastId() {
        return timeline.getLastId();
    }

    public String getFirstId() {
        return timeline.getFirstId();
    }

    public void refresh() {
        display.setCurrent(display.getCurrent());
    }

    public void setCurrent(Displayable dspl) {
        if (dspl instanceof Alert) {
            setCurrent((Alert) dspl, getCurrentScreen());
        } else {
            screenCurrentSet = dspl;
            display.setCurrent(dspl);
        }
    }

    public void setCurrent(Alert alert, Displayable nextDispl) {
        screenCurrentSet = nextDispl;
        display.setCurrent(alert, nextDispl);
    }

    public void setCurrent(int id) {
        setCurrent(getScreen(id));
    }

    public String getSnapshotMimeType() {
        String encoding =
                filename != null
                ? filename.toLowerCase()
                : settings.getStringProperty(Settings.SNAPSHOT_ENCODING, "").toLowerCase();
        String mimeType = "image/jpeg";
        if (encoding.indexOf("jpeg") >= 0 || encoding.indexOf("jpg") >= 0) {
            mimeType = "image/jpeg";
        } else if (encoding.indexOf("png") >= 0) {
            mimeType = "image/png";
        } else if (encoding.indexOf("gif") >= 0) {
            mimeType = "image/gif";
        }
        return mimeType;
    }

    public void toggleFavorited(Status status) {
        int objectType;
        if (status.getFavorited()) {
            objectType = RequestObjectTask.FAVORITE_DESTROY;
        } else {
            objectType = RequestObjectTask.FAVORITE_CREATE;
        }
        RequestObjectTask task = new RequestObjectTask(this, api, objectType, String.valueOf(status.getId()));
        WaitScreen wait = new WaitScreen(this, task, SCREEN_STATUS);
        wait.start();
    }

    public void updateStatus() {
        String text = settings.getStringProperty(Settings.AUTO_UPDATE_TEXT, "%H:%M");
        text = DateUtil.strftime(text, null);
        SnapshotScreen ss = null;
        if (settings.getBooleanProperty(Settings.ENABLE_AUTO_UPDATE_PICTURE, false)) {
            try {
                ss = new SnapshotScreen(this, "", null);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
        UpdateStatusTask task = new UpdateStatusTask(this, api, text, replyTo, ss);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
        wait.println("updating...");
        wait.start();
        setCurrent(wait);
    }

    public WaitScreen updateStatus(String text) {
        String mimeType = getSnapshotMimeType();
        String suffix = settings.getStringProperty(Settings.SUFFIX_TEXT, "");
        UpdateStatusTask task = new UpdateStatusTask(this, api, text + suffix, replyTo, snapshot, mimeType);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_UPDATE);
        wait.println("updating...");
        wait.start();
        setCurrent(wait);
        return wait;
    }

    public WaitScreen updateStatus(String text, String attachFile, String mimeType) {
        String suffix = settings.getStringProperty(Settings.SUFFIX_TEXT, "");
        UpdateStatusTask task = new UpdateStatusTask(this, api, text + suffix, replyTo, attachFile, mimeType);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_UPDATE);
        wait.println("updating...");
        wait.start();
        setCurrent(wait);
        return wait;
    }

    public void fetchTimeline(int feedType) {
        fetchTimeline(feedType, false);
    }

    public void fetchTimeline(int feedType, boolean nonBlock) {
        if (feedType == -1) {
            feedType = RequestTimelineTask.FEED_HOME;
        }
        RequestTimelineTask task = new RequestTimelineTask(this, api, feedType, nonBlock);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
        wait.println("fetching...");
        setCurrent(wait);
        if (feedType != currentFeedType) {
            clearTimeline();
        }
        currentFeedType = feedType;
        wait.start();
    }

    public void fetchPrevTimeline(int feedType) {
        fetchPrevTimeline(feedType, false);
    }

    public void fetchPrevTimeline(int feedType, boolean nonBlock) {
        if (feedType == -1) {
            feedType = RequestTimelineTask.FEED_HOME;
        }
        RequestTimelineTask task = new RequestTimelineTask(this, api, feedType, nonBlock, true);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
        wait.println("fetching old...");
        setCurrent(wait);
        if (feedType != currentFeedType) {
            clearTimeline();
        }
        currentFeedType = feedType;
        wait.start();
    }

    /*public void fetchTest() {
     RequestObjectTask task = new RequestObjectTask(this, api, RequestObjectTask.TEST, "");
     WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
     wait.println("fetching...");
     wait.start();
     setCurrent(wait);
     }*/
    public void resetOAuth() {
        settings.setBooleanProperty(Settings.OAUTH_AUTHORIZED, false);
        settings.setStringProperty(Settings.OAUTH_REQUEST_TOKEN, "");
        settings.setStringProperty(Settings.OAUTH_REQUEST_SECRET, "");
        settings.setStringProperty(Settings.OAUTH_ACCESS_TOKEN, "");
        settings.setStringProperty(Settings.OAUTH_ACCESS_SECRET, "");
        try {
            settings.save(true);
        } catch (Exception e) {
            Log.error(e.toString());
        }
    }

    public void showOAuth(String url) {
        if (url == null) {
            url = oauth.getAuthorizeUrl();
        }
        OAuthScreen oa = new OAuthScreen(this, url, timeline);
        setCurrent(oa);
    }

    public void startOAuthRequestToken() {
        OAuthTask task = new OAuthTask(this, oauth, OAuthTask.REQUEST_TOKEN, "");
        WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
        wait.println("request token...");
        wait.start();
        setCurrent(wait);
    }

    public void startOAuthAccessToken(String pin) {
        OAuthTask task = new OAuthTask(this, oauth, OAuthTask.ACCESS_TOKEN, pin);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
        wait.println("access token...");
        wait.start();
        setCurrent(wait);
    }

    public String oauthRequestToken() {
        String url = null;
        try {
            oauth.fetchNewRequestToken();
            oauth.saveRequestToken(settings);
            url = oauth.getAuthorizeUrl();
            System.out.println("oauth authorize " + url);
        } catch (Exception e) {
            Log.error("oauthRequestToken " + e.toString());
        }
        if (url != null) {
            openUrl(url);
        }
        return url;
    }

    public void oauthAccessToken(String pin) {
        try {
            oauth.fetchNewAccessToken(pin);
            oauth.saveAccessToken(settings);
        } catch (Exception e) {
            Log.error(e.toString());
        }
    }

    /*public void fetchScheduleDowntime() {
     RequestObjectTask task = new RequestObjectTask(this, api, RequestObjectTask.SCHEDULE_DOWNTIME, "");
     WaitScreen wait = new WaitScreen(this, task, SCREEN_TIMELINE);
     wait.println("fetching...");
     wait.start();
     setCurrent(wait);
     }*/
    public void showStart() {
        int id = settings.getIntProperty(Settings.START_SCREEN, 0);
        switch (id) {
            case START_EMPTY_TIMELINE:
                showTimeline();
                break;
            case START_UPDATE:
                showUpdate();
                break;
            case START_HOME_TIMELINE:
                timeline.setTitle("Home");
                fetchTimeline(RequestTimelineTask.FEED_HOME);
                break;
            case START_REPLIES_TIMELINE:
                timeline.setTitle("@Replies");
                fetchTimeline(RequestTimelineTask.FEED_REPLIES);
                break;
        }
    }

    private String gt() {
        return getCurrentScreen() == null ? "null" : getCurrentScreen().getTitle();
    }

    public void showTimeline() {
//        snapshot = null;
//        thumbnail = null;
//        filename = null;
        setCurrent(timeline);
        timeline.addBookmarkCommand(searchTerm);
        timeline.restoreStatus();
    }

    public void goToBookmarks() {
        setCurrent(bookMarks());
    }

    public BookmarksScreen bookMarks() {
        if (bookMarks == null) {
            bookMarks = new BookmarksScreen(this);
        }
        return bookMarks;
    }

    public void saveBookmark(String searchTerm) {
        bookMarks().saveBookmark(searchTerm);
    }

    public void showSetup() {
        SetupScreen setup = new SetupScreen(this);
        setCurrent(setup);
    }

    public void showLog() {
        LogScreen log = new LogScreen(this);
        setCurrent(log);
    }

    public void showStatus(Status s) {
        if (s != null) {
            status.setStatus(s, false);
        }
        setCurrent(status);
    }

    public void showUpdate() {
        setReplyTo(0);
        setCurrent(update.refresh());
    }

    public void showUpdate(String text) {
        update.setString(text);
        showUpdate();
    }

    public void showUpdate(String text, String file) {
        update.setString(text, file);
        showUpdate();
    }

    public void showUpdate(String text, String file, String mimetype) {
        update.setString(text, file, mimetype);
        showUpdate();
    }

    public void showInsert() {
        setCurrent(insert);
    }

    public void showList(List list) {
        setCurrent(list);
    }

    public void insertUpdate(String text) {
        update.insert(text);
        setCurrent(update.refresh());
    }

    public void showSnapshot() {
        try {
            SnapshotScreen ss = new SnapshotScreen(this, update.getString(), null);
            ss.start(true);
            setCurrent(ss);
        } catch (Exception e) {
            showError(e);
        }
    }

    public void quickSnapshot() {
        try {
            SnapshotScreen ss = new SnapshotScreen(this, update.getString(), null);
            QuickSnapshotTask task = new QuickSnapshotTask(this, ss);
            WaitScreen wait = new WaitScreen(this, task, SCREEN_UPDATE);
            wait.println("taking snapshot...");
            wait.start();
            setCurrent(wait);
        } catch (Exception e) {
            showError(e);
        }
    }

    public WaitScreen quickSnapshot2(String text) {
        WaitScreen wait = null;
        try {
            SnapshotScreen ss = new SnapshotScreen(this, update.getString(), null);
            ss.start(false);
            ss.quickSnapshot(false);
            wait = updateStatus(text);
        } catch (Exception e) {
            Log.error("quickShot: " + e.toString());
        }
        return wait;
    }
    private TimerTask snapshotSeriesTask = null;

    public void startSnapshotSeries() {
        Log.verbose("startSnapshotSeries");
        long timeBetweenShots = settings.getIntProperty(Settings.TIME_BETWEEN_SHOTS, 20) * 1000;
        if (snapshotSeriesTask != null) {
            snapshotSeriesTask.cancel();
        }
        new Timer().schedule(snapshotSeriesTask = new TimerTask() {
            private int counter = 0, errCount = 0;
            private final int numShots = settings.getIntProperty(Settings.TIMELINE_LENGTH, 20);
            private Hashtable waitt = new Hashtable();

            public boolean cancel() {
                Log.verbose("endSnapshotSeries");
                snapshotSeriesTask = null;
                return super.cancel();
            }

            public void run() {
                Enumeration ke = waitt.keys();
                while (ke.hasMoreElements()) {
                    WaitScreen ws = (WaitScreen) ke.nextElement();
                    if (ws.finished) {
                        waitt.remove(ws);
                    }
                }
                if (errCount > 8) {
                    this.cancel();
                } else {
                    if (waitt.size() > 2) {
                        errCount++;
                        return;
                    }
                    if (counter++ >= numShots) {
                        this.cancel();
                        return;
                    }
                    try {
                        Log.verbose("SnapshotSerie #" + counter);
                        Date dt = new Date();
                        WaitScreen wait = quickSnapshot2(counter + ". " + dt.toString());
                        if (wait != null) {
                            waitt.put(wait, dt);
                            errCount = 0;
                        } else {
                            errCount++;
                        }
                    } catch (Exception e) {
                        errCount++;
                        Log.error(counter + ". error taking snapshot: " + e.getMessage());
                    }
                }
            }
        }, 0, timeBetweenShots);
    }

    public void insertLocation(int mode) {
        try {
            com.sugree.utils.Location loc = new com.sugree.utils.Location(settings);
            update.insert(loc.refresh(api, mode));
            setCurrent(update.refresh());
        } catch (Exception e) {
            showError(e);
        }
    }

    public String memStat() {
        return "memory Kb: " + (Runtime.getRuntime().freeMemory() >> 10)
                + "/" + (Runtime.getRuntime().totalMemory() >> 10);
    }

    public void showAbout() {
        String text = TwitterMIDlet.NAME + " " + TwitterMIDlet.VERSION
                + " by twi.rosinstrument.com based on jibjib "
                + "\n\n";
        text += memStat()
                + "\n";
        text += "Platform: " + System.getProperty("microedition.platform") + "\n";
        text += "Platform: " + System.getProperty("microedition.platform") + "\n";
        text += "CLDC: " + System.getProperty("microedition.configuration") + "\n";
        text += "MIDP: " + System.getProperty("microedition.profiles") + "\n";
        text += "LAPI: " + System.getProperty("microedition.location.version") + "\n";
        text += "AMMS: " + System.getProperty("microedition.amms.version") + "\n";
        text += "MMAPI: " + System.getProperty("microedition.media.version") + "\n";
        text += "mixing: " + System.getProperty("supports.mixing") + "\n";
        text += "audio.capture: " + System.getProperty("supports.audio.capture") + "\n";
        text += "video.capture: " + System.getProperty("supports.video.capture") + "\n";
        text += "recording: " + System.getProperty("supports.recording") + "\n";
        text += "audio.encodings: " + System.getProperty("audio.encodings") + "\n";
        text += "video.encodings: " + System.getProperty("video.encodings") + "\n";
        text += "video.snapshot.encodings: " + System.getProperty("video.snapshot.encodings") + "\n";
        text += "streamable.contents: " + System.getProperty("streamable.contents") + "\n";
        String[] properties = {
            "Cell-ID",
            "CellID",
            "LocAreaCode",
            "IMSI",};
        for (int i = 0; i < properties.length; i++) {
            text += properties[i] + ": " + System.getProperty(properties[i]) + "\n";
        }
        showAlert("About", text);
    }

    public void showLink() {
        LinkScreen link = new LinkScreen(this);
        setCurrent(link);
    }

    public void showAlert(String title, String text) {
        alert(title, text, AlertType.INFO);
    }

    public void showError(Error e) {
        showError("Error", e.toString(), AlertType.ERROR);
    }

    public void showError(Exception e) {
        showError("Exception", e.getMessage(), AlertType.ERROR);
    }

    public void showError(Exception e, boolean nonblock) {
        if (nonblock) {
            showError("Exception", e.getMessage(), null);
        } else {
            showError("Exception", e.getMessage(), AlertType.ERROR);
        }
    }

    public void showAlert(Exception e, boolean nonblock) {
        if (nonblock) {
            showError("Exception", e.getMessage(), null);
        } else {
            showError("Exception", e.getMessage(), AlertType.ERROR);
        }
    }

    public void showError(String title, String text, AlertType type) {
        Log.error(text);
        if (type == null) {
            //setCurrent(screen);
        } else {
            alert(title, text, type);
        }
    }

    public Displayable getCurrentScreen() {
        return getScreen(SCREEN_CURRENT);
    }

    public Displayable getScreen(int id) {
        Displayable screen = screenCurrentSet;
        switch (id) {
            case SCREEN_CURRENT:
                if (screen == null) {
                    screenCurrentSet = screen = display.getCurrent();
                }
                break;
            case SCREEN_TIMELINE:
                screen = timeline;
                break;
            case SCREEN_UPDATE:
                screen = update;
                break;
            case SCREEN_STATUS:
                screen = status;
                break;
        }
        return screen;
    }

    public InsertScreen getInsert() {
        return insert;
    }

    public void autoSetSnaphotsFrequency(boolean init) {
        if (init) {
            autoSetSnaphotsFrequencyFlag = init;
        }
        if (lastPostHeaders == null) {
            return;
        }
        if (lastPostHeaders != null && autoSetSnaphotsFrequencyFlag) {
            int daily_limit = 30;
            autoSetSnaphotsFrequencyFlag = false;
            Object orl = lastPostHeaders.get("X-MediaRateLimit-Limit");
            if (orl != null) {
                int rl = Integer.parseInt(orl.toString());
                if (rl > 0) {
                    daily_limit = rl;
                }
            }
            int tbs = 24 * 3600 / daily_limit + 1;
            Log.verbose("set time between shots to " + tbs);
            settings.setIntProperty(Settings.TIME_BETWEEN_SHOTS, tbs);
        }
    }

    public Display getCurrent() {
        return display;
    }

    public void Delete(Status status, int listindex) {
        int objectType = RequestObjectTask.DELETE;
        RequestObjectTask task = new RequestObjectTask(this, api, objectType, status, listindex);
        WaitScreen wait = new WaitScreen(this, task, SCREEN_STATUS);
        //wait.println("request following...");
        wait.start();
        //setCurrent(wait);
    }

    public void Follow(Status status) {
        int objectType = RequestObjectTask.FOLLOW;
        RequestObjectTask task = new RequestObjectTask(this, api, objectType, status.getScreenName());
        WaitScreen wait = new WaitScreen(this, task, SCREEN_STATUS);
        //wait.println("request following...");
        wait.start();
        //setCurrent(wait);
    }

    public void unFollow(Status status) {
        int objectType = RequestObjectTask.UNFOLLOW;
        RequestObjectTask task = new RequestObjectTask(this, api, objectType, status.getScreenName());
        WaitScreen wait = new WaitScreen(this, task, SCREEN_STATUS);
        //wait.println("request following...");
        wait.start();
        //setCurrent(wait);
    }

    public void newSearch(String url) {
        searchTerm = url;
        clearTimeline();
        showTimeline();
        fetchTimeline(RequestTimelineTask.SEARCH, false);
    }

    public int maxPicSize() {
        return getSettings().getIntProperty(Settings.MAX_PIC_SIZE, 512) * 1024;
    }

    public void clearTimeline() {
        timeline.clearTimeline();
    }
    private String xFileName = null;

    public void setFileName(String filename) {
        xFileName = filename;
    }

    public String getFileName() {
        return xFileName;
    }

    public void alert(String title, String description, Throwable e) {
        String msg = "";
        AlertType alertType = AlertType.INFO;
        if (e != null) {
            alertType = AlertType.ERROR;
            String m = e.getMessage();
            if (m != null && m.length() > 0) {
                msg = "\n" + e.toString() + ":\n" + m;
            } else {
                msg = "\n" + e.toString();
            }
        }
        alert(title, description + msg, alertType);
    }

    public void alert(String title, String description) {
        alert(title, description, AlertType.INFO);
    }

    public void alert(String title, String description, AlertType alertType) {
        Alert alert = new Alert(title, description, null, alertType);
        alert.setTimeout(Alert.FOREVER);
        setCurrent(alert);
    }

    public void showReply(Status status) {
        showUpdate("@" + status.getScreenName() + " " + status.getText());
        setReplyTo(status.getId());
    }
}
