package com.cavetale.kingofthering;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class PitOfDoomCommand implements TabExecutor {
    private final KingOfTheRingPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("[kotr:pitofdoom] player expected");
            return true;
        }
        for (Game game : plugin.games.values()) {
            if (game.isRunning()) {
                game.spawnPlayer(player);
                break;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
