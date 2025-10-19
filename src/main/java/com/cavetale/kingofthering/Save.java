package com.cavetale.kingofthering;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class Save {
    protected boolean debug;
    protected boolean event;
    protected Map<UUID, Integer> scores = new HashMap<>();
    protected String gameName; // default game

    public void addScore(UUID uuid, int value) {
        int old = scores.getOrDefault(uuid, 0);
        scores.put(uuid, Math.max(0, old + value));
    }

    public int getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }
}
