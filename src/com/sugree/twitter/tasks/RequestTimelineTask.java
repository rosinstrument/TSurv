package com.sugree.twitter.tasks;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.TwitterException;
import java.util.Hashtable;
import java.util.Vector;

public class RequestTimelineTask extends AbstractTask {

    private TwitterController controller;
    private TwitterApi api;
    private int feedType;
    private boolean nonBlock;
    private boolean retro = false;
    private static String lastReqId = "";
    public final static int FEED_HOME = 1;
    public final static int FEED_REPLIES = 2;
    public final static int FEED_USER = 3;
    public final static int FEED_PUBLIC = 4;
    public final static int FEED_DIRECT = 5;
    public final static int FEED_FAVORITES = 6;
    public final static int SEARCH = 7;
    private static Hashtable lastRequestedId = new Hashtable();
    private static final long DID = 100;

    public RequestTimelineTask(TwitterController controller, TwitterApi api, int feedType, boolean nonBlock) {
        this.controller = controller;
        this.api = api;
        this.feedType = feedType;
        this.nonBlock = nonBlock;
    }

    public RequestTimelineTask(TwitterController controller, TwitterApi api, int feedType, boolean nonBlock, boolean retroFlag) {
        this.controller = controller;
        this.api = api;
        this.feedType = feedType;
        this.nonBlock = nonBlock;
        retro = retroFlag;
    }

    public static String refreshLine(int feedtype, String searchTerm) {
        String FT = FT(feedtype, searchTerm);
        String prev = (String) lastRequestedId.get(FT);
        lastRequestedId.remove(FT);
        return prev;
    }

    public String refreshLine(int feedtype) {
        String FT = FT(feedtype);
        String prev = (String) lastRequestedId.get(FT);
        lastRequestedId.remove(FT);
        return prev;
    }

    private String FT(int feedtype) {
        return FT(feedtype, controller.searchTerm);
    }

    private static String FT(int feedtype, String searchTerm) {
        return feedtype + (feedtype == SEARCH ? searchTerm : "");
    }

    private String FT() {
        return FT(feedType);
    }

    public void doTask() {
        Vector timeline = new Vector();
        String FT = FT();
        String lastid = controller.getLastId();
        if (lastid.length() != 0) {
            try {
                if (lastReqId.length() == 0
                        || (Long.parseLong(lastid)
                        > Long.parseLong(lastReqId))) {
                    lastReqId = lastid;
                }
            } catch (NumberFormatException nf) {
                Log.error(nf.toString());
            }
        } else {
            String sli = (String) lastRequestedId.get(FT);
            if (sli != null) {
                lastid = Long.toString(Long.parseLong(sli) - DID);
            }
        }
        String lastId = retro ? "-" + controller.getFirstId() : lastid;
        try {
            if (feedType == FEED_HOME) {
                timeline = api.requestHomeTimeline(lastId);
            } else if (feedType == FEED_REPLIES) {
                timeline = api.requestMentionsTimeline(lastId);
            } else if (feedType == FEED_USER) {
                timeline = api.requestUserTimeline(lastId);
            } else if (feedType == FEED_PUBLIC) {
                timeline = api.requestPublicTimeline(lastId);
            } else if (feedType == SEARCH) {
                String st = controller.searchTerm;
                if (st != null) {
                    if (st.startsWith("@")) {
                        controller.timeline.setTitleName("timeline: " + st);
                    } else {
                        controller.timeline.setTitleName("search: " + st);
                    }
                    timeline = api.requestSearchTimeline(lastId, st);
                }
            } else if (feedType == FEED_DIRECT) {
                timeline = api.requestDirectMessagesTimeline(lastId);
            } else if (feedType == FEED_FAVORITES) {
                timeline = api.requestFavoritesTimeline();
            }
            if (!timeline.isEmpty()) {
                controller.addTimeline(timeline, nonBlock, retro);
                String li = controller.getLastId();
                String sli = (String) lastRequestedId.get(FT);
                try {
                    if (li != null && li.length() > 0) {
                        if (sli == null || sli.length() < 1) {
                            // || (Long.parseLong(sli) < Long.parseLong(li)
                            lastRequestedId.put(FT, li);
                        }
                    }
                } catch (NumberFormatException nf) {
                }
            } else {
                Log.error("empty timeline");
            }
            controller.showTimeline();
        } catch (TwitterException e) {
            Log.error(e.toString());
            controller.showAlert(e, nonBlock);
        }
    }
}
