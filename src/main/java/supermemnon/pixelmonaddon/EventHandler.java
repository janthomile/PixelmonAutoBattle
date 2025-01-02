package supermemnon.pixelmonaddon;

import com.pixelmonmod.pixelmon.api.events.KeyEvent;
import com.pixelmonmod.pixelmon.comm.packetHandlers.EnumKeyPacketMode;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Level;

import static supermemnon.pixelmonaddon.ConfigHandler.messageNoPermissionToggle;

public class EventHandler {

    @Mod.EventBusSubscriber(modid = PixelmonAutobattle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            Commands.register(event.getDispatcher());
        }

        @SubscribeEvent
        //must be used to handle the drop conditions
        public static void onLivingDrops(LivingDropsEvent event) {
            //Is it a pixelmon, wild, and autobattling?
            if (!AutoBattleHandler.BattleHandler.isWildAutoBattler(event.getEntityLiving())) {
                return;
            }
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onLivingDropsXp(LivingExperienceDropEvent event) {
            if (!AutoBattleHandler.BattleHandler.isWildAutoBattler(event.getEntityLiving())) {
                return;
            }
            event.setCanceled(true);
        }

    }

    public static class ModEvents {
        @SubscribeEvent
        public static void onKeyEvent(KeyEvent event) {
            if (!event.player.isCrouching() || !(event.key == EnumKeyPacketMode.ActionKeyEntity)) {
                return;
            }
            if (!PermissionAPI.hasPermission(event.player, PixelmonAutobattle.TOGGLE_PERMISSION)) {
                event.player.sendMessage(new StringTextComponent(messageNoPermissionToggle).withStyle(TextFormatting.RED), event.player.getUUID());
                return;
            }
            AutoBattleHandler.BattleHandler.toggleAutoBattle(event.player);
        }
    }
}
