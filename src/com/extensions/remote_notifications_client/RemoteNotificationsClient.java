package com.extensions.remote_notifications_client;


import android.app.Activity;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;

import static android.Manifest.permission.INTERNET;

@DesignerComponent(
        version = 1,
        category = ComponentCategory.EXTENSION,
        description = "RNS client",
        nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions({ INTERNET })
public class RemoteNotificationsClient extends AndroidNonvisibleComponent {
    private final Requester requester;

    private String target = "";

    public RemoteNotificationsClient(Form form) {
        super(form);
        Activity context = form.$context();
        requester = new Requester(context);
    }

    @SimpleProperty(description = "Target device IP. Empty string if not set.")
    public String Target() {
        return target;
    }

    @SimpleProperty
    public void Target(String ip) {
        target = ip;
        requester.baseUrl = "http://" + ip;
    }

    @SimpleFunction
    public void ResetTarget() {
        target = "";
        requester.baseUrl = null;
    }

    private boolean targetNotDefined() {
        return Objects.equals(target, "");
    }

    @SimpleFunction
    public void GetProtocolVersion() {
        if (targetNotDefined()) return;

        requester.get(
                "/ver",
                new Requester.OnRequestDataCallback() {
                    @Override
                    public void onData(String data, int responseCode) {
                        if (responseCode == 200) {
                            try {
                                int version = Integer.parseInt(data.trim());
                                OnProtocolVersionFetched(version);
                            } catch (NumberFormatException e) {
                                OnProtocolVersionRequestFailed("invalid response");
                            }
                        } else {
                            OnProtocolVersionRequestFailed("invalid response");
                        }
                    }
                },
                new Requester.OnRequestFailCallback() {
                    @Override
                    public void onError(Requester.RequestError reason) {
                        OnProtocolVersionRequestFailed(Requester.errorToString(reason));
                    }
                }
        );
    }

    @SimpleEvent
    public void OnProtocolVersionFetched(int version) {
        EventDispatcher.dispatchEvent(this, "OnProtocolVersionFetched", version);
    }

    @SimpleEvent
    public void OnProtocolVersionRequestFailed(String reason) {
        EventDispatcher.dispatchEvent(this, "OnProtocolVersionRequestFailed", reason);
    }

    @SimpleFunction
    public void SyncTime() {
        if (targetNotDefined()) return;

        long currentTime = new GregorianCalendar().getTimeInMillis();

        requester.post(
                "/time?format=ms",
                Requester.Payload.plain(Long.toString(currentTime)),
                new Requester.OnRequestDataCallback() {
                    @Override
                    public void onData(String data, int responseCode) {
                        if (Objects.equals(data, "ok") && responseCode == 200) {
                            OnTimeSynced();
                        } else {
                            OnTimeSyncFailure("invalid response");
                        }
                    }
                },
                new Requester.OnRequestFailCallback() {
                    @Override
                    public void onError(Requester.RequestError reason) {
                        OnTimeSyncFailure(Requester.errorToString(reason));
                    }
                }
        );
    }

    @SimpleEvent
    public void OnTimeSynced() {
        EventDispatcher.dispatchEvent(this, "OnTimeSynced");
    }

    @SimpleEvent
    public void OnTimeSyncFailure(String reason) {
        EventDispatcher.dispatchEvent(this, "OnTimeSyncFailure", reason);
    }

    @SimpleFunction
    public void Notify(final String iconData, final String appName, final String title, final String content) {
        if (targetNotDefined()) return;

        requester.post(
                "/notify",
                Requester.Payload.xWwwFormUrlencoded(
                        "icon", iconData,
                        "appName", appName,
                        "title", title,
                        "content", content
                ),
                new Requester.OnRequestDataCallback() {
                    @Override
                    public void onData(String data, int responseCode) {
                        if (Objects.equals(data, "ok") && responseCode == 200) return;

                        OnNotifyFailure("invalid response");
                    }
                },
                new Requester.OnRequestFailCallback() {
                    @Override
                    public void onError(Requester.RequestError reason) {
                        OnNotifyFailure(Requester.errorToString(reason));
                    }
                }
        );
    }

    @SimpleEvent
    public void OnNotifyFailure(String reason) {
        EventDispatcher.dispatchEvent(this, "OnNotifyFailure", reason);
    }
}
