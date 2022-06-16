package com.extensions.remote_notifications_client;

public class AsyncRunner {
    private AsyncRunner() {}

    public static void runAsynchronously(final Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
