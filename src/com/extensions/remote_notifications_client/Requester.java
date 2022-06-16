package com.extensions.remote_notifications_client;

import android.app.Activity;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("Convert2Lambda")
public class Requester {
    public enum RequestError {
        BAD_URL,
        CONNECTION_ERROR,
        TIMEOUT
    }

    private static class URLComponentEncoder {
        public static String encode(String source) {
            String result;

            try {
                result = URLEncoder.encode(source, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException exception) {
                result = source;
            }

            return result;
        }
    }

    public static class OnRequestDataCallback {
        public void onData(String data, int responseCode) { /*...*/ }
    }

    public static class OnRequestFailCallback {
        public void onError(RequestError reason) { /*...*/ }
    }

    private static class ConnectionConfigurator {
        public void configure(HttpURLConnection connection) { /*...*/ }
    }

    public static class Payload {
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        public static final String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String APPLICATION_JSON = "application/json";
        public static final String TEXT_PLAIN = "text/plain";
        private final String contentType;
        private final String body;

        public Payload(String contentType, String body) {
            this.contentType = contentType;
            this.body = body;
        }

        public static Payload xWwwFormUrlencoded(String... entries) {
            if (entries.length == 0) {
                return new Payload(X_WWW_FORM_URLENCODED, "");
            }

            Map<String, String> data = new HashMap<>();

            for (int i = 0; i < entries.length; i += 2) {
                try {
                    data.put(entries[i], entries[i + 1]);
                } catch (ArrayIndexOutOfBoundsException exception) {
                    // If the last argument doesn't have a pair, it will be ignored
                }
            }

            StringBuilder payloadBuilder = new StringBuilder();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                payloadBuilder.append(
                    String.format(
                        "%s=%s&",
                        URLComponentEncoder.encode(entry.getKey()),
                        URLComponentEncoder.encode(entry.getValue())
                    )
                );
            }

            payloadBuilder.deleteCharAt(payloadBuilder.length() - 1);

            return new Payload(X_WWW_FORM_URLENCODED, payloadBuilder.toString());
        }

        public static Payload json(Map<String, Object> source) {
            return new Payload(APPLICATION_JSON, JSON.stringify(source));
        }

        public static Payload plain(String text) {
            return new Payload(TEXT_PLAIN, text);
        }
    }

    public String baseUrl;
    private final Activity frontend;

    public Requester(Activity frontend) {
        this.frontend = frontend;
    }

    public static String errorToString(RequestError error) {
        //noinspection EnhancedSwitchMigration
        switch (error) {
            case BAD_URL: return "bad url";
            case CONNECTION_ERROR: return "connection error";
            case TIMEOUT: return "timeout";
            default: return "unknown";
        }
    }

    private void runCallbackOnUiThread(final OnRequestDataCallback callback, final String data, final int responseCode) {
        frontend.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onData(data, responseCode);
            }
        });
    }

    private void runCallbackOnUiThread(final OnRequestFailCallback callback, final RequestError reason) {
        frontend.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onError(reason);
            }
        });
    }

    private void asyncRequest(
            final String path, final String method, final OnRequestDataCallback onData, final OnRequestFailCallback onError,
            final ConnectionConfigurator connectionConfigurator
    ) {
        AsyncRunner.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                URL url;

                try {
                    url = new URL(baseUrl + path);
                } catch (MalformedURLException exception) {
                    runCallbackOnUiThread(onError, RequestError.BAD_URL);
                    return;
                }

                String data;
                int code;

                try {
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod(method);
                    connectionConfigurator.configure(connection);

                    code = connection.getResponseCode();
                    InputStream responseStream = connection.getInputStream();
                    BufferedReader input = new BufferedReader(new InputStreamReader(responseStream));
                    List<String> lines = new ArrayList<>();

                    String line;
                    while ((line = input.readLine()) != null) {
                        lines.add(line);
                    }
                    data = String.join(System.lineSeparator(), lines);

                    connection.disconnect();
                } catch (SocketTimeoutException exception) {
                    runCallbackOnUiThread(onError, RequestError.TIMEOUT);
                    return;
                } catch (IOException exception) {
                    runCallbackOnUiThread(onError, RequestError.CONNECTION_ERROR);
                    return;
                }

                runCallbackOnUiThread(onData, data, code);
            }
        });
    }

    private void asyncRequest(final String path, final String method, final OnRequestDataCallback onData, final OnRequestFailCallback onError) {
        AsyncRunner.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                URL url;

                try {
                    url = new URL(baseUrl + path);
                } catch (MalformedURLException exception) {
                    runCallbackOnUiThread(onError, RequestError.BAD_URL);
                    return;
                }

                StringBuilder responseContentBuilder = new StringBuilder();
                int code;

                try {
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod(method);

                    code = connection.getResponseCode();
                    InputStream responseStream = connection.getInputStream();
                    BufferedReader input = new BufferedReader(new InputStreamReader(responseStream));
                    String line;

                    while ((line = input.readLine()) != null) {
                        responseContentBuilder.append(line).append(System.lineSeparator());
                    }

                    connection.disconnect();
                } catch (IOException exception) {
                    runCallbackOnUiThread(onError, RequestError.CONNECTION_ERROR);
                    return;
                }

                runCallbackOnUiThread(onData, responseContentBuilder.toString(), code);
            }
        });
    }

    public void get(String path, OnRequestDataCallback onData, OnRequestFailCallback onError) {
        this.asyncRequest(path, "GET", onData, onError);
    }

    public void post(String path, final Payload payload, OnRequestDataCallback onData, OnRequestFailCallback onError) {
        this.asyncRequest(path, "POST", onData, onError, new ConnectionConfigurator() {
            @Override
            public void configure(HttpURLConnection connection) {
                connection.setRequestProperty("Content-Type", payload.contentType);
                connection.setDoOutput(true);
                final byte[] payloadBytes = payload.body.getBytes();

                try {
                    connection.getOutputStream().write(payloadBytes, 0, payloadBytes.length);
                } catch (IOException ignored) {}
            }
        });
    }

    public void post(String path, OnRequestDataCallback onData, OnRequestFailCallback onError) {
        this.asyncRequest(path, "POST", onData, onError);
    }
}
