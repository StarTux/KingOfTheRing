package com.cavetale.kingofthering;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class KingOfTheRingCommand implements TabExecutor {
    private final KingOfTheRingPlugin plugin;

    public void enable() {
        plugin.getCommand("kingofthering").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (args.length == 0) {
            player.teleport(plugin.save.spawn.toBlock(plugin.getWorld()).getLocation().add(0.5, 1.0, 0.5));
            return true;
        }
        switch (args[0]) {
        case "debug": {
            plugin.save.debug = !plugin.save.debug;
            plugin.save();
            player.sendMessage("Debug = " + plugin.save.debug);
            return true;
        }
        case "area": {
            plugin.save.world = player.getWorld().getName();
            plugin.save.area = Cuboid.selectionOf(player);
            player.sendMessage("Area updated: " + plugin.save.world + ", " + plugin.save.area);
            plugin.save();
            return true;
        }
        case "death": {
            plugin.save.death = Cuboid.selectionOf(player);
            player.sendMessage("Death area updated: " + plugin.save.death);
            plugin.save();
            return true;
        }
        case "perimeter": {
            plugin.save.perimeter = Cuboid.selectionOf(player);
            player.sendMessage("Perimieter updated: " + plugin.save.perimeter);
            plugin.save();
            return true;
        }
        case "spawn": {
            plugin.save.spawn = Vec3i.of(player.getLocation().getBlock());
            player.sendMessage("Spawn updated: " + plugin.save.world + ", " + plugin.save.spawn);
            plugin.save();
            return true;
        }
        case "start": {
            plugin.start();
            sender.sendMessage("starting");
            return true;
        }
        case "stop": {
            plugin.stop();
            sender.sendMessage("stopping");
            return true;
        }
        case "platform": {
            Cuboid sel = Cuboid.selectionOf(player);
            plugin.save.platforms.add(sel);
            plugin.save();
            player.sendMessage("Platform added: " + sel);
            return true;
        }
        case "rmplatform": {
            Cuboid sel = Cuboid.selectionOf(player);
            List<Cuboid> platformsToRemove = new ArrayList<>();
            for (Cuboid platform : plugin.save.platforms) {
                if (sel.contains(platform)) {
                    platformsToRemove.add(platform);
                }
            }
            plugin.save.platforms.removeAll(platformsToRemove);
            plugin.save();
            player.sendMessage(platformsToRemove.size() + " platforms removed: " + platformsToRemove);
            return true;
        }
        case "hl": {
            for (Cuboid cuboid : plugin.save.platforms) {
                cuboid.highlight(plugin.getWorld(), 0.0, location -> {
                        player.spawnParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0);
                    });
            }
            plugin.save.area.highlight(plugin.getWorld(), 0.0, location -> {
                    player.spawnParticle(Particle.WAX_ON, location, 1, 0, 0, 0, 0);
                });
            player.sendMessage("Highlighting");
            return true;
        }
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>(List.of("area", "death", "perimeter", "spawn", "start", "stop",
                                                          "platform", "rmplatform", "debug", "hl"));
            result.removeIf(i -> !i.contains(args[0]));
            return result;
        }
        return null;
    }
}
