// src/main/java/com/yourpackage/xmlive/core/RecorderBinding.java
package com.xmjjs.xmllive.core;

import java.util.UUID;

public class RecorderBinding {
    private final UUID recorderUuid;
    private UUID targetUuid;
    private boolean autoMode;
    private int interval; // 自动切换间隔（秒）

    public RecorderBinding(UUID recorderUuid, UUID targetUuid, boolean autoMode, int interval) {
        this.recorderUuid = recorderUuid;
        this.targetUuid = targetUuid;
        this.autoMode = autoMode;
        this.interval = interval;
    }

    public UUID getRecorderUuid() {
        return recorderUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}