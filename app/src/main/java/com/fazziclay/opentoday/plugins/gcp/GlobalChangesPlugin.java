package com.fazziclay.opentoday.plugins.gcp;

import com.fazziclay.opentoday.api.EventHandler;
import com.fazziclay.opentoday.api.OpenTodayPlugin;
import com.fazziclay.opentoday.util.Logger;

public class GlobalChangesPlugin extends OpenTodayPlugin {
    private static final String TAG = "plugin://GlobalChangesPlugin";

    private final EventHandler[] handlers = new EventHandler[]{new GcpEventHandler(this)};

    @Override
    public void onEnable() {
        super.onEnable();
        Logger.d(TAG, "onEnable");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Logger.d(TAG, "onDisable");
    }

    @Override
    public EventHandler[] getEventHandlers() {
        return handlers;
    }
}
