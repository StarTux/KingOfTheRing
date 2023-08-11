package com.cavetale.kingofthering;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.util.Json;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingOfTheRingPlugin extends JavaPlugin {
    protected KingOfTheRingCommand kingoftheringCommand = new KingOfTheRingCommand(this);
    protected EventListener eventListener = new EventListener(this);
    protected Save save;
    protected boolean teleporting;
    protected static final List<String> WINNER_TITLES = List.of("Doom",
                                                                "Stygian",
                                                                "Balrog",
                                                                "LavaBucket");
    protected File saveFolder;
    protected final Map<String, Game> games = new HashMap<>();

    @Override
    public void onEnable() {
        kingoftheringCommand.enable();
        eventListener.enable();
        saveFolder = new File(getDataFolder(), "games");
        saveFolder.mkdirs();
        load();
        getCommand("pitofdoom").setExecutor(new PitOfDoomCommand(this));
        for (World world : Bukkit.getWorlds()) {
            onLoadWorld(world);
        }
    }

    @Override
    public void onDisable() {
        save();
        for (Game game : games.values()) {
            game.save();
            try {
                game.disable();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Disable game " + game.name, e);
            }
        }
        games.clear();
    }

    protected void save() {
        getDataFolder().mkdirs();
        Json.save(new File(getDataFolder(), "save.json"), save, true);
    }

    protected void load() {
        save = Json.load(new File(getDataFolder(), "save.json"), Save.class, Save::new);
    }

    public Game getGameAt(Location location) {
        for (Game game : games.values()) {
            if (!game.worldName.equals(location.getWorld().getName())) {
                continue;
            }
            if (!game.isInPerimeter(location)) {
                continue;
            }
            return game;
        }
        return null;
    }

    public boolean applyGameAt(Location location, Consumer<Game> callback) {
        Game game = getGameAt(location);
        if (game == null) return false;
        callback.accept(game);
        return true;
    }

    protected void onLoadWorld(World world) {
        AreasFile areasFile = AreasFile.load(world, "PitOfDoom");
        if (areasFile == null) return;
        for (Map.Entry<String, List<Area>> entry : areasFile.getAreas().entrySet()) {
            String name = entry.getKey();
            if (games.containsKey(name)) {
                getLogger().severe("Duplicate game " + name + " in world " + world.getName());
                continue;
            }
            List<Area> areas = entry.getValue();
            Game game = new Game(this, world, name, areas);
            games.put(name, game);
            try {
                game.enable();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Enable game " + name, e);
            }
        }
    }

    protected void onUnloadWorld(World world) {
        for (Game game : List.copyOf(games.values())) {
            if (!world.getName().equals(game.worldName)) continue;
            game.save();
            try {
                game.disable();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Disable game " + game.name, e);
            }
            games.remove(game.name);
        }
    }
}
