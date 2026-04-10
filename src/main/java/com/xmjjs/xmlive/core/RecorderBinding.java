package com.xmjjs.xmlive.core;

import org.bukkit.GameMode;

import java.util.UUID;

public class RecorderBinding {

    public static final int MODE_VELOCITY = 0;
    public static final int MODE_PACKET = 1;

    private final UUID recorderUuid;
    private UUID targetUuid;
    private boolean autoMode;
    private int interval;
    private double customDistance = -1.0;
    private double customPitch = -1.0;
    private boolean spectatorMode = false;
    private GameMode previousGameMode;
    private int cameraMode = MODE_VELOCITY;

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

    public double getCustomDistance() {
        return customDistance;
    }

    public void setCustomDistance(double customDistance) {
        this.customDistance = customDistance;
    }

    public double getCustomPitch() {
        return customPitch;
    }

    public void setCustomPitch(double customPitch) {
        this.customPitch = customPitch;
    }

    public double getEffectiveDistance(double globalDistance) {
        return customDistance > 0 ? customDistance : globalDistance;
    }

    public double getEffectivePitch(double globalPitch) {
        return customPitch > 0 ? customPitch : globalPitch;
    }

    public boolean isSpectatorMode() {
        return spectatorMode;
    }

    public void setSpectatorMode(boolean spectatorMode) {
        this.spectatorMode = spectatorMode;
    }

    public GameMode getPreviousGameMode() {
        return previousGameMode;
    }

    public void setPreviousGameMode(GameMode previousGameMode) {
        this.previousGameMode = previousGameMode;
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int cameraMode) {
        this.cameraMode = cameraMode;
    }
}
