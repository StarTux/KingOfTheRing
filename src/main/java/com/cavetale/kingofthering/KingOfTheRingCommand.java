package com.cavetale.kingofthering;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
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
}
