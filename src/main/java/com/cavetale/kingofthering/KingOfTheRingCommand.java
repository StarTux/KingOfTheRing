package com.cavetale.kingofthering;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import java.util.List;
import org.bukkit.command.CommandSender;
import static com.cavetale.core.command.CommandArgCompleter.supplyIgnoreCaseList;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class KingOfTheRingCommand extends AbstractCommand<KingOfTheRingPlugin> {
    protected KingOfTheRingCommand(final KingOfTheRingPlugin plugin) {
        super(plugin, "kingofthering");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("debug").arguments("[debug]")
            .completers(CommandArgCompleter.BOOLEAN)
            .description("Debug spam")
            .senderCaller(this::debug);
        rootNode.addChild("event").arguments("[event]")
            .completers(CommandArgCompleter.BOOLEAN)
            .description("Set event mode")
            .senderCaller(this::event);
        rootNode.addChild("start").arguments("<game>")
            .completers(supplyIgnoreCaseList(() -> List.copyOf(plugin.games.keySet())))
            .description("Start a game")
            .senderCaller(this::start);
        rootNode.addChild("stop").arguments("<game>")
            .completers(supplyIgnoreCaseList(() -> List.copyOf(plugin.games.keySet())))
            .description("Stop a game")
            .senderCaller(this::stop);
        // Score
        CommandNode scoreNode = rootNode.addChild("score")
            .description("Score commands");
        scoreNode.addChild("add")
            .description("Manipulate score")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i != 0))
            .senderCaller(this::scoreAdd);
        scoreNode.addChild("clear").denyTabCompletion()
            .description("Clear all scores")
            .senderCaller(this::scoreClear);
        scoreNode.addChild("reward").denyTabCompletion()
            .description("Reward players")
            .senderCaller(this::scoreReward);
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (args.length > 1) {
            return false;
        } else if (args.length == 1) {
            plugin.save.debug = CommandArgCompleter.requireBoolean(args[0]);
            plugin.save();
        }
        sender.sendMessage(text("Debug = " + plugin.save.debug, YELLOW));
        return true;
    }

    private boolean event(CommandSender sender, String[] args) {
        if (args.length > 1) {
            return false;
        } else if (args.length == 1) {
            plugin.save.event = CommandArgCompleter.requireBoolean(args[0]);
            plugin.save();
        }
        sender.sendMessage(text("Event = " + plugin.save.event, YELLOW));
        return true;
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Game game = plugin.games.get(args[0]);
        if (game == null) throw new CommandWarn("Game not found: " + args[0]);
        game.start();
        sender.sendMessage(text("Starting game: " + game.name, AQUA));
        return true;
    }

    private boolean stop(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Game game = plugin.games.get(args[0]);
        if (game == null) throw new CommandWarn("Game not found: " + args[0]);
        game.stop();
        sender.sendMessage(text("Stopping game: " + game.name, YELLOW));
        return true;
    }

    private boolean scoreClear(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.save.scores.clear();
        plugin.computeHighscore();
        sender.sendMessage(text("All scores cleared", AQUA));
        return true;
    }

    private boolean scoreAdd(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        int value = CommandArgCompleter.requireInt(args[1], i -> i != 0);
        plugin.save.addScore(target.uuid, value);
        plugin.computeHighscore();
        sender.sendMessage(text("Score of " + target.name + " manipulated by " + value, AQUA));
        return true;
    }

    private void scoreReward(CommandSender sender) {
        int count = plugin.rewardHighscore();
        sender.sendMessage(text("Rewarded " + count + " players", AQUA));
    }
}
