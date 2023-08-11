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
import org.bukkit.event.player.PlayerTeleportEvent;
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
        if (!plugin.isRunning()) return;
        if (plugin.isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.isRunning()) return;
        if (!event.getEntity().getWorld().getName().equals(plugin.save.world)) return;
        if (event.getEntity() instanceof Player) {
            if (plugin.save.area.contains(event.getEntity().getLocation())) {
                if (event.getDamager() instanceof Player) {
                    event.setDamage(0.0);
                }
            }
        }
    }

    @EventHandler
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!plugin.isRunning()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.isPlayer(player) && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.isRunning()) return;
        if (plugin.teleporting) return;
        if (plugin.isPlayer(event.getPlayer())) {
            event.setCancelled(true);
        } else if (plugin.save.area.contains(event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        if (!plugin.isRunning()) return;
        if (!plugin.isPlayer(event.getPlayer())) return;
        switch (event.getAction()) {
        case FLY:
            event.setCancelled(true);
            break;
        default: break;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onEntityToggleGlideEvent(EntityToggleGlideEvent event) {
        if (!plugin.isRunning()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.isPlayer(player) && event.isGliding()) {
            event.setCancelled(true);
            player.sendMessage(text("No flying!", RED));
            plugin.removePlayer(player);
            plugin.spawnPlayer(player);
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (!plugin.isRunning()) return;
        if (!plugin.save.world.equals(event.getPlayer().getWorld().getName())) return;
        if (!plugin.save.perimeter.contains(event.getPlayer().getLocation())) return;
        List<Component> lines = List.of(text("Pit of Doom", GOLD),
                                        text("Round ", GOLD)
                                        .append(text(plugin.save.loopCount + 1, WHITE)),
                                        text("Alive ", GOLD)
                                        .append(text(plugin.save.players.size(), WHITE)),
                                        text("You are ", GOLD)
                                        .append(plugin.isPlayer(event.getPlayer())
                                                ? text("Alive", GREEN)
                                                : text("Dead", DARK_RED)));
        event.sidebar(PlayerHudPriority.HIGH, lines);
    }
}
