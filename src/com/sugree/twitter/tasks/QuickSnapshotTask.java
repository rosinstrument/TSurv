package com.sugree.twitter.tasks;

import com.substanceofcode.tasks.AbstractTask;
import com.sugree.twitter.TwitterController;
import com.sugree.twitter.views.SnapshotScreen;

public class QuickSnapshotTask extends AbstractTask {

    private TwitterController controller;
    private SnapshotScreen snapshot;

    public QuickSnapshotTask(TwitterController controller, SnapshotScreen snapshot) {
        this.controller = controller;
        this.snapshot = snapshot;
    }

    public void doTask() {
        try {
            snapshot.start(false);
            snapshot.quickSnapshot();
        } catch (Exception e) {
            controller.showError(e);
        }
        snapshot = null;
    }
}
