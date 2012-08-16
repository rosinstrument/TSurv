package com.sugree.twitter.tasks;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.twitter.model.Status;
import com.sugree.twitter.TwitterController;

public class RequestObjectTask extends AbstractTask {

    private TwitterController controller;
    private TwitterApi api;
    private int objectType;
    private String id;
    private Status status = null;
    private int index;
    public final static int FAVORITE_CREATE = 0;
    public final static int FAVORITE_DESTROY = 1;
    //public final static int TEST = 2;
    //public final static int SCHEDULE_DOWNTIME = 3;
    public final static int FOLLOW = 4;
    public final static int UNFOLLOW = 5;
    public final static int DELETE = 6;

    public RequestObjectTask(TwitterController controller, TwitterApi api, int objectType, String id) {
        this.controller = controller;
        this.api = api;
        this.objectType = objectType;
        this.id = id;
    }

    public RequestObjectTask(TwitterController controller, TwitterApi api, int objectType, Status status, int list_index) {
        this.controller = controller;
        this.api = api;
        this.objectType = objectType;
        this.status = status;
        this.index = list_index;
    }

    synchronized public void doTask() {
        String resStr = "", result = "Success!";
        try {
            switch (objectType) {
                case FOLLOW:
                    resStr = "Follow";
                    api.followName(id);
                    break;
                case UNFOLLOW:
                    resStr = "UnFollow";
                    api.unFollowName(id);
                    break;
                case DELETE:
                    resStr = "Delete";
                    if (status == null) {
                        break;
                    }
                    api.Delete(status.getId());
                    controller.timeline.deleteStatus(status, index);
                    break;
                case FAVORITE_CREATE:
                    resStr = "Create Favorite";
                    status = api.createFavorite(id);
                    status.setFavorited(true);
                    controller.updateTimeline(status);
                    //controller.showTimeline();
                    //controller.showStatus(status);
                    break;
                case FAVORITE_DESTROY:
                    resStr = "Destroy Favorite";
                    status = api.destroyFavorite(id);
                    status.setFavorited(false);
                    controller.updateTimeline(status);
                    //controller.showTimeline();
                    break;
            }
        } catch (Exception e) {
            result = e.getMessage();
        }
        controller.showAlert(resStr, result);
    }
}
