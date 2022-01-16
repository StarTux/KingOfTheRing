package com.cavetale.kingofthering;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

public final class KingOfTheRingPlugin extends JavaPlugin {
    KingOfTheRingCommand kingoftheringCommand = new KingOfTheRingCommand(this);
    EventListener eventListener = new EventListener(this);
    Save save;
    boolean teleporting;
    BukkitTask task;
    protected Random random = new Random();
    protected final List<Platform> platforms = new ArrayList<>();
    protected final List<Creeper> creepers = new ArrayList<>();
    protected static final List<String> WINNER_TITLES = List.of("Doom",
                                                                "Stygian",
                                                                "Balrog",
                                                                "LavaBucket");

    @Override
    public void onEnable() {
        kingoftheringCommand.enable();
        eventListener.enable();
        load();
        if (isRunning()) {
            task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        }
        getCommand("pitofdoom").setExecutor(new PitOfDoomCommand(this));
    }

    @Override
    public void onDisable() {
        save();
        cleanUp();
    }

    void save() {
        getDataFolder().mkdirs();
        Json.save(new File(getDataFolder(), "save.json"), save, true);
    }

    void load() {
        save = Json.load(new File(getDataFolder(), "save.json"), Save.class, Save::new);
    }

    World getWorld() {
        return Bukkit.getWorld(save.world);
    }

    List<Player> getActivePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : save.players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) result.add(player);
        }
        return result;
    }

    List<Player> playersInPerimeter() {
        List<Player> result = new ArrayList<>();
        for (Player player : getWorld().getPlayers()) {
            if (!save.perimeter.contains(player.getLocation())) continue;
            result.add(player);
        }
        return result;
    }

    void tick() {
        World world = Bukkit.getWorld(save.world);
        if (world == null) return;
        save.players.keySet().removeIf(u -> Bukkit.getPlayer(u) == null);
        List<Player> players = getActivePlayers();
        if (!save.debug && players.size() == 1) {
            win(players.get(0));
            return;
        } else if (players.size() == 0) {
            draw();
            return;
        }
        for (Player player : players) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            if (!save.area.contains(player.getLocation())) {
                player.sendMessage(ChatColor.RED + "You left the area!");
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
                Location location = randomSpawnLocation();
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
            List<Cuboid> allPlatforms = new ArrayList<>(save.platforms);
            int max = Math.min(allPlatforms.size() - 1, (2 * save.loopCount + 3));
            Collections.shuffle(allPlatforms);
            for (int i = 0; i < max; i += 1) {
                Cuboid cuboid = allPlatforms.get(i);
                Platform platform = new Platform();
                for (Vec3i vec : cuboid.enumerate()) {
                    Block block = vec.toBlock(world);
                    platform.blocks.add(block);
                    platform.blockData.add(block.getBlockData());
                }
                platform.ticks = i * -5;
                platforms.add(platform);
            }
        } else if (save.loopTicks == 20 * 25) {
            cleanUp();
            save.loopTicks = 0;
            save.loopCount += 1;
        }
        for (Platform platform : platforms) {
            platform.tick();
        }
        for (Player player : playersInPerimeter()) {
            if (!isPlayer(player) && save.area.contains(player.getLocation())) {
                spawnPlayer(player);
            }
        }
    }

    void start() {
        World world = Bukkit.getWorld(save.world);
        if (world == null) return;
        List<String> names = new ArrayList<>();
        teleporting = true;
        for (Player player : playersInPerimeter()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            player.teleport(randomSpawnLocation());
            player.sendMessage(ChatColor.DARK_RED + "Get ready!");
            player.showTitle(Title.title(Component.text("Get ready!", NamedTextColor.GREEN),
                                         Component.text("The game begins", NamedTextColor.GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 0.5f, 2.0f);
            names.add(player.getName());
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(potionEffect.getType());
            }
            player.setFlying(false);
            player.setGliding(false);
            save.players.put(player.getUniqueId(), player.getName());
        }
        teleporting = false;
        save.state = State.PLAY;
        save.loopTicks = 0;
        save.loopCount = 0;
        save();
        if (names.isEmpty()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + String.join(" ", names));
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
    }

    void stop() {
        task.cancel();
        save.state = State.IDLE;
        save.players.clear();
        save();
        cleanUp();
    }

    boolean isRunning() {
        return save.state != State.IDLE;
    }

    Location randomSpawnLocation() {
        List<Vec3i> list = new ArrayList<>();
        for (Cuboid it : save.platforms) {
            list.addAll(it.enumerate());
        }
        Vec3i vec = list.get(random.nextInt(list.size()));
        return vec.toBlock(getWorld()).getLocation().add(0.5, 1.0, 0.5);
    }

    public boolean isPlayer(Player player) {
        return isRunning() && save.players.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        save.players.remove(player.getUniqueId());
    }

    public void spawnPlayer(Player player) {
        teleporting = true;
        player.teleport(save.spawn.toBlock(getWorld()).getLocation().add(0.5, 1.0, 0.5));
        teleporting = false;
    }

    void win(Player winner) {
        for (Player player : playersInPerimeter()) {
            player.sendMessage(Component.text()
                               .append(winner.displayName())
                               .append(Component.text(" wins the game!", NamedTextColor.GREEN)));
            player.showTitle(Title.title(winner.displayName(),
                                         Component.text("wins the game", NamedTextColor.GREEN)));
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + winner.getName()
                               + " " + String.join(" ", WINNER_TITLES));
        stop();
    }

    void draw() {
        for (Player player : playersInPerimeter()) {
            player.sendMessage(ChatColor.RED + "DRAW! Nobody wins");
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
