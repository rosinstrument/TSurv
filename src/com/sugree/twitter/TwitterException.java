package com.sugree.twitter;

public class TwitterException extends Exception {

    public TwitterException(Throwable e) {
        super(
                e.getMessage() != null
                ? e.toString() + ": " + e.getMessage()
                : e.toString());
    }

    public TwitterException(String text) {
        super(text);
    }
}
