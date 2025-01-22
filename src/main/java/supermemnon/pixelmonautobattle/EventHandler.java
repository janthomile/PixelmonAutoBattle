package supermemnon.pixelmonautobattle;

import com.pixelmonmod.pixelmon.api.events.KeyEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.events.spawning.PixelmonSpawnerEvent;
import com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent;
import com.pixelmonmod.pixelmon.api.pokemon.species.aggression.Aggression;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.comm.packetHandlers.EnumKeyPacketMode;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.UUID;

import static supermemnon.pixelmonautobattle.ConfigHandler.messageNoPermissionToggle;

public class EventHandler {

    public static boolean isWildHostile(PixelmonEntity p) {
        return p.getPokemon().getOriginalTrainer() != null && p.getPokemon().getOriginalTrainer().equalsIgnoreCase("dungeon");
    }

    @Mod.EventBusSubscriber(modid = PixelmonAutobattle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            AutoBattleCommands.register(event.getDispatcher());
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

        @SubscribeEvent
        public static void onPunchPokemon(AttackEntityEvent event) {
            if (!ConfigHandler.usePunchToTarget.get() || !(event.getTarget() instanceof PixelmonEntity)) {
                return;
            }
            AutoBattleHandler.BattleHandler.setAutoBattlingTarget((ServerPlayerEntity) event.getPlayer(), (LivingEntity) event.getTarget());
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

        @SubscribeEvent
        public static void onPokemonSpawner(PixelmonSpawnerEvent.Post event) {
            PixelmonEntity p = event.getEntity();
            if (p == null) {return;}

            if (!isWildHostile(p)) {
                return;
            }

            p.goalSelector.addGoal(-1, new AutoBattleAI.WildHostileGoal(p));

        }

        @SubscribeEvent
        public static void onBattleEnd(BattleEndEvent event) {
            for (BattleParticipant bp : event.getResults().keySet()) {
                if (bp instanceof WildPixelmonParticipant && isWildHostile((((WildPixelmonParticipant) bp).asWrapper().entity))) {
                    PixelmonEntity p = ((WildPixelmonParticipant) bp).asWrapper().entity;
                    p.setAggression(Aggression.AGGRESSIVE);
                    p.setAggressionTimer(50);
                }
            }
        }
    }
}
