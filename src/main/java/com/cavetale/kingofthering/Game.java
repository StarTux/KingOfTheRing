package com.cavetale.kingofthering;

import com.cavetale.area.struct.Area;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Game {
    protected final KingOfTheRingPlugin plugin;
    protected final String worldName;
    protected final String name;
    protected final List<Area> areaList;
    protected final File saveFile;
    protected GameSave save;
    // Setup
    protected List<Cuboid> perimeters = new ArrayList<>();
    protected List<Cuboid> areas = new ArrayList<>();
    protected List<Cuboid> spawns = new ArrayList<>();
    protected List<Cuboid> platformShapes = new ArrayList<>();
    // Runtime
    protected BukkitTask task;
    protected Random random = new Random();
    protected final List<Platform> platforms = new ArrayList<>();
    protected final List<Creeper> creepers = new ArrayList<>();

    public Game(final KingOfTheRingPlugin plugin, final World world, final String name, final List<Area> areaList) {
        this.plugin = plugin;
        this.worldName = world.getName();
        this.name = name;
        this.areaList = areaList;
        this.saveFile = new File(plugin.saveFolder, name + ".json");
    }

    protected void enable() {
        loadAreas();
        load();
        if (isRunning()) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    protected void disable() {
        cleanUp();
    }

    private void loadAreas() {
        for (Area area : areaList) {
            if (area.name == null) {
                plugin.getLogger().severe("[" + name + "] unnamed area: " + area);
                continue;
            }
            switch (area.getName()) {
            case "perimeter": perimeters.add(area.toCuboid()); break;
            case "area": areas.add(area.toCuboid()); break;
            case "spawn": spawns.add(area.toCuboid()); break;
            case "platform": platformShapes.add(area.toCuboid()); break;
            default:
                plugin.getLogger().severe("[" + name + "] Unknown area name: " + area);
            }
        }
    }

    protected void load() {
        save = Json.load(saveFile, GameSave.class, GameSave::new);
    }

    protected void save() {
        Json.save(saveFile, save, true);
    }

    protected World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    protected List<Player> getActivePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : save.players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) result.add(player);
        }
        return result;
    }

    protected List<Player> playersInPerimeter() {
        List<Player> result = new ArrayList<>();
        PLAYERS: for (Player player : getWorld().getPlayers()) {
            Location loc = player.getLocation();
            for (Cuboid perimeter : perimeters) {
                if (perimeter.contains(loc)) {
                    result.add(player);
                    continue PLAYERS;
                }
            }
        }
        return result;
    }

    protected boolean isInArea(Location location) {
        for (Cuboid area : areas) {
            if (area.contains(location)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInPerimeter(Location location) {
        for (Cuboid perimeter : perimeters) {
            if (perimeter.contains(location)) {
                return true;
            }
        }
        return false;
    }

    protected void tick() {
        World world = getWorld();
        if (world == null) return;
        save.players.keySet().removeIf(u -> Bukkit.getPlayer(u) == null);
        final List<Player> players = getActivePlayers();
        if (save.state == State.COUNTDOWN) {
            if (save.countdownTicks > 0) {
                if (save.countdownTicks % 20 == 0) {
                    final int seconds = save.countdownTicks / 20;
                    for (Player player : players) {
                        player.sendActionBar(text(seconds, GOLD));
                    }
                }
                save.countdownTicks -= 1;
            } else {
                save.state = State.PLAY;
            }
            for (Player player : players) {
                if (!isInArea(player.getLocation())) {
                    player.sendMessage(text("Don't fall off!", RED));
                    plugin.teleporting = true;
                    player.teleport(randomPlatformLocation());
                    plugin.teleporting = false;
                }
            }
            return;
        }
        if (!plugin.save.debug && players.size() == 1) {
            win(players.get(0));
            return;
        } else if (players.size() == 0) {
            draw();
            return;
        }
        for (Player player : players) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            if (!isInArea(player.getLocation())) {
                player.sendMessage(text("You left the game area!", RED));
                removePlayer(player);
            }
        }
        save.loopTicks += 1;
        if (save.loopTicks == 20 * 5) {
            for (Player player : players) {
                player.sendActionBar(Component.text("Watch out!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.5f, 1.0f);
            }
        } else if (save.loopTicks == 20 * 6) {
            for (int i = 0; i < (save.loopCount / 2); i += 1) {
                Location location = randomPlatformLocation();
                Creeper creeper = location.getWorld().spawn(location, Creeper.class, e -> {
                        e.setPersistent(false);
                        if (save.loopCount > 5 && random.nextInt(10) == 0) {
                            e.setPowered(true);
                        }
                    });
                world.playSound(location, Sound.ENTITY_CREEPER_HURT, SoundCategory.MASTER, 1.0f, 1.0f);
                creepers.add(creeper);
            }
        } else if (save.loopTicks == 20 * 7) {
            List<Cuboid> allPlatforms = new ArrayList<>(platformShapes);
            final int loopFactor = Math.max(1, allPlatforms.size() / 15);
            final int init = Math.max(1, allPlatforms.size() / 10);
            final int max = Math.min(allPlatforms.size() - 1, (loopFactor * save.loopCount + init));
            Collections.shuffle(allPlatforms);
            for (int i = 0; i < max; i += 1) {
                Cuboid cuboid = allPlatforms.get(i);
                Platform platform = new Platform();
                for (Vec3i vec : cuboid.enumerate()) {
                    Block block = vec.toBlock(world);
                    if (block.isEmpty() || block.getType() == Material.LIGHT) continue;
                    platform.blocks.add(block);
                    platform.blockData.add(block.getBlockData());
                }
                platform.ticks = i * -5;
                platforms.add(platform);
            }
        }
        boolean allPlatformsDone = !platforms.isEmpty();
        for (Platform platform : platforms) {
            if (!platform.done) {
                allPlatformsDone = false;
                break;
            }
        }
        if (allPlatformsDone) {
            save.allPlatformsDoneTicks += 1;
            if (save.allPlatformsDoneTicks >= 20) {
                cleanUp();
                save.allPlatformsDoneTicks = 0;
                save.loopTicks = 0;
                save.loopCount += 1;
                if (plugin.save.event) {
                    for (UUID uuid : save.players.keySet()) {
                        plugin.save.addScore(uuid, 1);
                    }
                    plugin.computeHighscore();
                }
            }
        }
        for (Platform platform : platforms) {
            platform.tick();
        }
        for (Player player : playersInPerimeter()) {
            if (!isPlayer(player) && isInArea(player.getLocation())) {
                spawnPlayer(player);
            }
        }
    }

    protected void start() {
        World world = getWorld();
        if (world == null) return;
        List<String> names = new ArrayList<>();
        plugin.teleporting = true;
        for (Player player : playersInPerimeter()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            player.teleport(randomPlatformLocation());
            player.sendMessage(text("Get ready!", DARK_RED));
            player.showTitle(Title.title(Component.text("Get ready!", NamedTextColor.GREEN),
                                         Component.text("The game begins", NamedTextColor.GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 0.5f, 2.0f);
            names.add(player.getName());
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(potionEffect.getType());
            }
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFlying(false);
            player.setGliding(false);
            save.players.put(player.getUniqueId(), player.getName());
        }
        plugin.teleporting = false;
        save.state = State.COUNTDOWN;
        save.countdownTicks = 20 * 10;
        save.loopTicks = 0;
        save.loopCount = 0;
        save();
        if (names.isEmpty()) return;
        if (plugin.save.event) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + String.join(" ", names));
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    protected void stop() {
        task.cancel();
        save.state = State.IDLE;
        save.players.clear();
        save();
        cleanUp();
    }

    protected boolean isRunning() {
        return save.state != State.IDLE;
    }

    protected Location randomPlatformLocation() {
        List<Block> list = new ArrayList<>();
        for (Cuboid cuboid : platformShapes) {
            for (Vec3i vec : cuboid.enumerate()) {
                Block block = vec.toBlock(getWorld());
                if (block.isEmpty() || block.getType() == Material.LIGHT) continue;
                list.add(block);
            }
        }
        Block block = list.get(random.nextInt(list.size()));
        Location result = block.getLocation().add(0.5, 1.0, 0.5);
        result.setYaw((float) (random.nextDouble() * 360.0));
        return result;
    }

    public boolean isPlayer(Player player) {
        return isRunning() && save.players.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        save.players.remove(player.getUniqueId());
    }

    public Location getRandomSpawnLocation() {
        Set<Vec3i> set = new HashSet<>();
        for (var it : spawns) set.addAll(it.enumerate());
        List<Vec3i> list = List.copyOf(set);
        Vec3i spawn = list.get(random.nextInt(list.size()));
        Location result = spawn.toBlock(getWorld()).getLocation().add(0.5, 1.0, 0.5);
        result.setYaw((float) (random.nextDouble() * 360.0));
        return result;
    }

    public void spawnPlayer(Player player) {
        plugin.teleporting = true;
        player.teleport(getRandomSpawnLocation());
        plugin.teleporting = false;
    }

    protected void win(Player winner) {
        for (Player player : playersInPerimeter()) {
            player.sendMessage(Component.text()
                               .append(winner.displayName())
                               .append(Component.text(" wins the game!", NamedTextColor.GREEN)));
            player.showTitle(Title.title(winner.displayName(),
                                         Component.text("wins the game", NamedTextColor.GREEN)));
        }
        if (plugin.save.event) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + winner.getName()
                                   + " " + String.join(" ", KingOfTheRingPlugin.WINNER_TITLES));
            plugin.save.addScore(winner.getUniqueId(), 10);
            plugin.computeHighscore();
            plugin.save();
        }
        stop();
    }

    protected void draw() {
        for (Player player : playersInPerimeter()) {
            player.sendMessage(text("DRAW! Nobody wins", RED));
            player.showTitle(Title.title(Component.text("Draw!", NamedTextColor.RED),
                                        Component.text("Nobody wins", NamedTextColor.RED)));
        }
        stop();
    }

    protected void cleanUp() {
        for (Platform platform : platforms) {
            platform.resetAll();
        }
        platforms.clear();
        for (Creeper creeper : creepers) {
            creeper.remove();
        }
        creepers.clear();
    }
}
