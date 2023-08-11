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
    protected Map<UUID, String> players = new HashMap<>();
}
