package supermemnon.pixelmonautobattle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

public class AutoBattleCommands {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> commandStructure = Commands.literal("autobattle").requires(source -> source.hasPermission(0));
        
	    commandStructure = appendCommand(commandStructure);

        dispatcher.register(commandStructure);
    }

    private static LiteralArgumentBuilder<CommandSource> appendCommand(LiteralArgumentBuilder<CommandSource> command) {
           return command
           .executes(context -> runAutoBattle(context.getSource()))
                   .then(Commands.literal("set")
                           .then(Commands.argument("player", EntityArgument.player())
                                   .then(Commands.literal("off")
                                           .executes(context -> setAutoBattle(context.getSource(), EntityArgument.getPlayer(context, "player"), false, false))
                                   )
                                   .then(Commands.literal("on")
                                           .then(Commands.argument("forced", BoolArgumentType.bool())
                                                   .executes(context -> setAutoBattle(context.getSource(), EntityArgument.getPlayer(context, "player"), true, BoolArgumentType.getBool(context, "forced")))
                                           )
                                   )
                           )
           );
    }

    private static int setAutoBattle(CommandSource source, ServerPlayerEntity target, boolean value, boolean forceOut) {
        if (value) {
            AutoBattleHandler.BattleHandler.enableAutoBattle(target, -1, forceOut);
        }
        else {
            AutoBattleHandler.BattleHandler.disableAutoBattle(target);
        }
        return 1;
    }

    private static int runAutoBattle(CommandSource source) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendFailure(new StringTextComponent("Error: Command must be executed by player!"));
        }
        AutoBattleHandler.BattleHandler.toggleAutoBattle((ServerPlayerEntity) source.getEntity());
        return 1;
    }
//    private static LiteralArgumentBuilder<CommandSource> appendSetCommand(LiteralArgumentBuilder<CommandSource> command) {
//           return command.then(Commands.literal("set")
//                .then(Commands.literal("requireditem")
//                        .executes(context -> runSetRequiredItem(context.getSource()))
//                )
//               .then(Commands.literal("pokelootcommand")
//                       .then(Commands.argument("blockpos", BlockPosArgument.blockPos())
//                               .then(Commands.argument("command", StringArgumentType.greedyString())
//                                       .executes(
//                                               context -> runSetPokeLootCommand(context.getSource(), BlockPosArgument.getOrLoadBlockPos(context, "blockpos"), StringArgumentType.getString(context, "command"))
//                                       )
//                               )
//                       )
//               )
//               .then(Commands.literal("dialogue")
//                       .then(Commands.argument("dialogue", StringArgumentType.greedyString())
//                               .executes(context -> runSetCustomDialogue(context.getSource(), StringArgumentType.getString(context, "dialogue"))
//                               )
//                       )
//               )
//               .then(Commands.literal("npcstare")
//                       .then(Commands.argument("entity", EntityArgument.entity())
//                               .then(Commands.argument("blockpos", BlockPosArgument.blockPos())
//                                       .executes(context -> runSetNpcStare(context.getSource(), EntityArgument.getEntity(context, "entity"),BlockPosArgument.getOrLoadBlockPos(context, "blockpos"))
//                                       )
//                               )
//                       )
//               )
//           );
//    }
}
