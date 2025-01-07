package supermemnon.pixelmonautobattle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Level;

public class AutoBattleCommands {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> commandStructure = Commands.literal("autobattle");
	    commandStructure = appendCommand(commandStructure);
        dispatcher.register(commandStructure);
    }

    private static boolean validatePermission(CommandSource source, String permission) {
        if (source.hasPermission(2)) {return true;}
        ServerPlayerEntity player = (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) ? (ServerPlayerEntity) source.getEntity() : null;
        if (player == null) { return false; }
        return PermissionAPI.hasPermission(player, permission);
    }

    private static LiteralArgumentBuilder<CommandSource> appendCommand(LiteralArgumentBuilder<CommandSource> command) {
           return command
                   .then(Commands.literal("toggle")
                           .requires(source -> validatePermission(source, PixelmonAutobattle.TOGGLE_PERMISSION))
                           .executes(context -> runToggleAutoBattle(context.getSource()))
                   )
                   .then(Commands.literal("set")
                           .requires(source -> validatePermission(source, PixelmonAutobattle.SET_PERMISSION))
                           .then(Commands.argument("player", EntityArgument.player())
                               .then(Commands.literal("off")
                                       .executes(context -> runSetAutoBattle(context.getSource(), EntityArgument.getPlayer(context, "player"), false, false))
                               )
                               .then(Commands.literal("on")
                                       .then(Commands.argument("forced", BoolArgumentType.bool())
                                               .executes(context -> runSetAutoBattle(context.getSource(), EntityArgument.getPlayer(context, "player"), true, BoolArgumentType.getBool(context, "forced")))
                                       )
                               )
                           )
                   );
    }

    private static int runSetAutoBattle(CommandSource source, ServerPlayerEntity target, boolean value, boolean forceOut) {
        if (target == null) {
            return 0;
        }
        if (value) {
            AutoBattleHandler.BattleHandler.enableAutoBattle(target, -1, forceOut);
        }
        else {
            AutoBattleHandler.BattleHandler.disableAutoBattle(target);
        }
        return 1;
    }

    private static int runToggleAutoBattle(CommandSource source) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendFailure(new StringTextComponent("Error: Command must be executed by player!"));
            return 0;
        }
        AutoBattleHandler.BattleHandler.toggleAutoBattle((ServerPlayerEntity) source.getEntity());
        return 1;
    }
}
