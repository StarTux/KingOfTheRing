package com.cavetale.kingofthering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Save {
    State state = State.IDLE;
    Map<UUID, String> players = new HashMap<>();
    String world = "";
    Cuboid area = Cuboid.ZERO;
    Cuboid perimeter = Cuboid.ZERO;
    Cuboid death = Cuboid.ZERO;
    Vec3i spawn = Vec3i.ZERO;
    List<Cuboid> platforms = new ArrayList<>();
    boolean debug;
    int loopTicks;
    int loopCount;
}
