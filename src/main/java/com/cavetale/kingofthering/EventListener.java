package com.cavetale.kingofthering;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final KingOfTheRingPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.applyGameAt(event.getPlayer().getLocation(), game -> {
                if (!game.isRunning()) return;
                if (game.isPlayer(event.getPlayer())) {
                    event.setCancelled(true);
                }
            });
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        plugin.applyGameAt(event.getEntity().getLocation(), game -> {
                if (!game.isRunning()) return;
                if (event.getEntity() instanceof Player) {
                    if (event.getDamager() instanceof Player) {
                        event.setCancelled(true);
                    }
                }
            });
    }

    @EventHandler
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        plugin.applyGameAt(event.getEntity().getLocation(), game -> {
                if (!(event.getEntity() instanceof Player player)) return;
                if (game.isPlayer(player) && event.getFoodLevel() < player.getFoodLevel()) {
                    event.setCancelled(true);
                }
            });
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (plugin.teleporting) return;
        plugin.applyGameAt(event.getPlayer().getLocation(), game -> {
                if (!game.isRunning()) return;
                if (game.isPlayer(event.getPlayer())) {
                    event.setCancelled(true);
                }
            });
        plugin.applyGameAt(event.getTo(), game -> {
                if (!game.isRunning()) return;
                event.setCancelled(true);
            });
    }

    @EventHandler
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        plugin.applyGameAt(event.getPlayer().getLocation(), game -> {
                if (!game.isRunning()) return;
                if (!game.isPlayer(event.getPlayer())) return;
                switch (event.getAction()) {
                case FLY:
                    event.setCancelled(true);
                    break;
                default: break;
                }
            });
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onEntityToggleGlideEvent(EntityToggleGlideEvent event) {
        plugin.applyGameAt(event.getEntity().getLocation(), game -> {
                if (!game.isRunning()) return;
                if (!(event.getEntity() instanceof Player)) return;
                Player player = (Player) event.getEntity();
                if (game.isPlayer(player) && event.isGliding()) {
                    event.setCancelled(true);
                    player.sendMessage(text("No flying!", RED));
                    game.removePlayer(player);
                    game.spawnPlayer(player);
                }
            });
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.applyGameAt(event.getPlayer().getLocation(), game -> {
                event.setRespawnLocation(game.getRandomSpawnLocation());
            });
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        plugin.applyGameAt(event.getPlayer().getLocation(), game -> {
                if (!game.isRunning()) {
                    if (plugin.save.event) {
                        event.sidebar(PlayerHudPriority.HIGH, plugin.highscoreLines);
                    }
                    return;
                }
                List<Component> lines = List.of(text("Pit of Doom", GOLD),
                                                text("Round ", GOLD)
                                                .append(text(game.save.loopCount + 1, WHITE)),
                                                text("Alive ", GOLD)
                                                .append(text(game.save.players.size(), WHITE)),
                                                text("You are ", GOLD)
                                                .append(game.isPlayer(event.getPlayer())
                                                        ? text("Alive", GREEN)
                                                        : text("Dead", DARK_RED)));
                event.sidebar(PlayerHudPriority.HIGH, lines);
            });
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        plugin.onLoadWorld(event.getWorld());
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        plugin.onUnloadWorld(event.getWorld());
    }
}
