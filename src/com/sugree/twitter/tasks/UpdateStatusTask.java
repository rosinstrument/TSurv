package com.sugree.twitter.tasks;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.twitter.TwitterApi;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.TwitterException;
import com.sugree.twitter.views.SnapshotScreen;

public class UpdateStatusTask extends AbstractTask {

    private TwitterController controller;
    private TwitterApi api;
    private String status;
    private long replyTo;
    private byte[] snapshot = null;
    private SnapshotScreen snapshotScreen = null;
    private String attachment = null;
    private String mimeType;
    private boolean nonBlock;

    public UpdateStatusTask(TwitterController controller, TwitterApi api, String status,
            long replyTo, byte[] snapshot, String mimeType) {
        this.controller = controller;
        this.api = api;
        this.status = status;
        this.replyTo = replyTo;
        this.snapshot = snapshot;
        this.mimeType = mimeType;
        this.nonBlock = false;
    }

    public UpdateStatusTask(TwitterController controller, TwitterApi api, String status,
            long replyTo, String attachment, String mimeType) {
        this.controller = controller;
        this.api = api;
        this.attachment = attachment;
        this.status = status;
        this.replyTo = replyTo;
        this.mimeType = mimeType;
        this.nonBlock = false;
    }

    public UpdateStatusTask(TwitterController controller, TwitterApi api,
            String status, long replyTo, SnapshotScreen snapshotScreen) {
        this.controller = controller;
        this.api = api;
        this.status = status;
        this.replyTo = replyTo;
        this.snapshotScreen = snapshotScreen;
        this.nonBlock = false;
    }

    public void doTask() {
        try {
            if (snapshotScreen != null) {
                try {
                    snapshotScreen.start(false);
                    snapshotScreen.quickSnapshot(true);
                    snapshot = controller.getSnapshot();
                    mimeType = controller.getSnapshotMimeType();
                } catch (Exception e) {
                    snapshot = null;
                }
            }
            if (attachment != null) {
                api.postPicture(status, attachment, mimeType);
            } else if (snapshot != null) {
                api.postPicture(status, snapshot, mimeType, controller.getSnapshotFilename());
            } else {
                api.updateStatus(status, replyTo);
            }
            controller.showTimeline();
        } catch (TwitterException e) {
            controller.showTimeline();
            controller.showAlert(e, nonBlock);
        }
        if (nonBlock) {
            controller.fetchTimeline(controller.getCurrentFeedType(), nonBlock);
        }
    }
}
