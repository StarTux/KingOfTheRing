package com.cavetale.kingofthering;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Save {
    protected State state = State.IDLE;
    protected Map<UUID, String> players = new HashMap<>();
    protected String world = "";
    protected Cuboid area = Cuboid.ZERO;
    protected Cuboid perimeter = Cuboid.ZERO;
    protected Cuboid death = Cuboid.ZERO;
    protected Vec3i spawn = Vec3i.ZERO;
    protected List<Cuboid> platforms = new ArrayList<>();
    protected boolean debug;
    protected int loopTicks;
    protected int loopCount;
    protected boolean event;
}
