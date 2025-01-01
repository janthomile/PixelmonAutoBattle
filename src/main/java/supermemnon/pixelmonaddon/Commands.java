package supermemnon.pixelmonaddon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

public class Commands {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> commandStructure = net.minecraft.command.Commands.literal("autobattle").requires(source -> source.hasPermission(0));
        
	    commandStructure = appendCommand(commandStructure);

        dispatcher.register(commandStructure);
    }

    private static LiteralArgumentBuilder<CommandSource> appendCommand(LiteralArgumentBuilder<CommandSource> command) {
           return command
           .executes(context -> runAutoBattle(context.getSource()));
    }

    private static int runAutoBattle(CommandSource source) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendFailure(new StringTextComponent("Error: Command must be executed by player!"));
        }
        AutoBattleHandler.BattleHandler.toggleAutoBattle((ServerPlayerEntity) source.getEntity());
        return 1;
    }

}
