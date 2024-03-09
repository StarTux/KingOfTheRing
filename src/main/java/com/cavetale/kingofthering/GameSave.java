package com.cavetale.kingofthering;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GameSave {
    protected String worldName = "";
    protected String areaName = "";
    protected State state = State.IDLE;
    protected int loopTicks;
    protected int loopCount;
    protected int countdownTicks;
    protected int allPlatformsDoneTicks;
    protected Map<UUID, Integer> playerRounds = new HashMap<>();
    protected Map<UUID, String> players = new HashMap<>();

    public void addRound(UUID uuid, int amount) {
        final int old = playerRounds.getOrDefault(uuid, 0);
        playerRounds.put(uuid, old + 1);
    }
}
