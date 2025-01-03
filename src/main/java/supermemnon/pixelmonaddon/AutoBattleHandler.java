package supermemnon.pixelmonaddon;

import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.Evolution;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.types.LevelingEvolution;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.DropItemHelper;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.Effectiveness;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import com.pixelmonmod.pixelmon.items.ExpAllItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class AutoBattleHandler {
    enum BattleResultType {
        WIN,
        WIN_HURT,
        DRAW,
        DEFEAT
    }
    public static float partyDistributeXPFactor = 0.5f;
    public static final int autoBattlePriority = -5; //Goal priority, 'higher' than seekwild so it can override it.
    public static final int seekWildPriority = 0; //Goal priority, 0 for general 'highest'.
    public static void sendActionBarNotif(ServerPlayerEntity player, ITextComponent text) {
        player.playSound(SoundEvents.UI_TOAST_IN, 0.25f, 1.88f);
        STitlePacket packet = new STitlePacket(STitlePacket.Type.ACTIONBAR, text);
        player.connection.send(packet);
    }
    public static String formatBattleMessage(String message, PixelmonEntity trainer, @Nullable PixelmonEntity wild) {
        message = message.replaceAll("%p", trainer.getPokemon().getFormattedDisplayName().getString());
        if (wild != null) {
            message = message.replaceAll("%w", wild.getLocalizedName());
        }
        return message;
    }
    public static class AutoBattleInstance {
        //The purpose of this class is to provide connections to AI Goals and pixelmon entities.
        //This is especially important because AI Goals are shoved into a private collection after being added to an entity.
        //So... We keep a reference here.
        public static Map<UUID, AutoBattleInstance> active_instances = new HashMap<>();
        PixelmonEntity trainerMon;
        AutoBattleAI.TrainerAutoBattleGoal trainerGoal;
        PixelmonEntity wildMon;
        AutoBattleAI.WildAutoBattleGoal wildGoal;

        //We only need the trainer mon because an instance only needs to handle logic for the mon's seeking behaviour until it finds a wild mon combatant.
        //Instances are also indexed into the hashmap by their mons' UUID, which we can just get from the entity.
        //Also this functions as the trainer-mon equivalent of initWildMon(), because the trainer-mon needs to be initialized right away!
        //And also, thusly, it must set the autoBattleEnable tag since it functionally starts the autobattling behaviour!
        public AutoBattleInstance(PixelmonEntity tmon) {
            if (active_instances.containsKey(tmon.getUUID())) {
                return;
            }
            trainerMon = tmon;
            trainerGoal = new AutoBattleAI.TrainerAutoBattleGoal(this, tmon);
            //Unfortunately we have to re-add this every time, because without mixins the pixelmon will totally reset its AI goals.
            tmon.goalSelector.addGoal(seekWildPriority, trainerGoal);
            AutoBattleHandler.NBTHandler.setTag(tmon, AutoBattleHandler.NBTHandler.autoBattleEnableTag, true);
        }

        //This'll be used when a valid target is found for the trainer mon
        //In the TrainerAutoBattleGoal, the pixelmon seeks out a target, approaches, and once in-range, the trainer-mon ensnares the wild-mon into the AutoBattleInstance using this function.
        public void initWildMon(PixelmonEntity wmon) {
//            wild_map.put(wmon.getUUID(), trainerMon.getUUID());
            wildMon = wmon;
            wildGoal = new AutoBattleAI.WildAutoBattleGoal(trainerMon, wmon);
            wmon.goalSelector.addGoal(autoBattlePriority, wildGoal);
        }

        public static void addAutoBattleInstance(UUID uuid, AutoBattleInstance instance) {
            //Remove the instance if it already exists
            if (active_instances.containsKey(uuid)) {
                removeAutoBattleInstance(uuid, true);
            }
            active_instances.put(uuid, instance);
        }

        //Removes either or of the mon goals.
        //Could be two separate methods, but, well...
        public void removeGoals(boolean trainer, boolean wild) {
            if (trainer && trainerMon != null && trainerGoal != null) {
                trainerMon.goalSelector.removeGoal(trainerGoal);
            }
            if (wild && wildMon != null && wildGoal != null) {
                wildMon.goalSelector.removeGoal(wildGoal);
            }
        }

        //We rely heavily on GC to remove the instances themselves...
        //Importantly, we also try and remove goals here, since this should only be accessed outside of the goal logic.
        public static void removeAutoBattleInstance(UUID monUUID, boolean isTrainerMon) {
            AutoBattleInstance instance = active_instances.get(monUUID);
            if (instance == null) {
                return;
            }
            instance.removeGoals(true, true);
//            if (isTrainerMon && (instance.wildMon != null)) {
//                wild_map.remove(instance.wildMon.getUUID());
////                active_instances.remove(instance.wildMon.getUUID());
//            }
            active_instances.remove(monUUID);
        }

        public static AutoBattleInstance getAutoBattleInstance(UUID uuid) {
            if (!active_instances.containsKey(uuid)) {
                return null;
            }
            return active_instances.get(uuid);
        }

        //Gets or creates new autobattle instance for the given UUID
        public static AutoBattleInstance getAutoBattleInstance(PixelmonEntity mon, boolean shouldInit) {
            AutoBattleInstance instance = getAutoBattleInstance(mon.getUUID());
            if (instance == null && shouldInit) {
                 instance = new AutoBattleInstance(mon);
                 addAutoBattleInstance(mon.getUUID(), instance);
            }
            return instance;
        }

        //Sets up the nbt tags for proofing and inits the wild mon's auto battle goals.
        public  void startAutoBattle(PixelmonEntity wildMon) {
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Starting auto battle between %s and %s", trainerMon.getName().getString(), wildMon.getName().getString()));
            AutoBattleHandler.NBTHandler.setTag(trainerMon, AutoBattleHandler.NBTHandler.autoBattlingTag, true);
            AutoBattleHandler.NBTHandler.setTag(wildMon, AutoBattleHandler.NBTHandler.autoBattlingTag, true);
            // These things predicate on their nbt boolean tag and will eat themselves once they are invalid
            initWildMon(wildMon);
            trainerMon.setTarget(wildMon);
            wildMon.setTarget(trainerMon);
        }

        //Handles ending the auto battle, removing tags, goals, etc...
        //Should be called from within the trainerGoal instance here
        public  void endAutoBattle() {
            trainerGoal.target = null;

            trainerGoal.checkFatigued();

            BattleResultType result = BattleHandler.calculateBattleResult(trainerMon, wildMon);

            //Since this is called by the trainerGoal, NEVER have it try and remove itself!
            //This will cause a huge concurrent access exception!
            //Anyways, we remove the wild mon's goal just in case it survives
            removeGoals(false, true);
            ServerPlayerEntity trainer = trainerMon.getOwner() instanceof ServerPlayerEntity ? (ServerPlayerEntity) trainerMon.getOwner() : null;
            //If no trainer is valid here, we just cancel early. No need to handle drops, messages, etc...
            //And luckily the goal-removal has already been handled! At least for the wild mon.
            //The trainer mon's goals
            if (trainer == null) {
                return;
            }
            switch(result) {
                case WIN: {
                    sendActionBarNotif(trainer, new StringTextComponent(formatBattleMessage(ConfigHandler.messageAutoBattleResultWin, trainerMon, wildMon)));
//                    trainer.sendMessage(new StringTextComponent(ConfigHandler.messageAutoBattleResultWin), trainer.getUUID());
                    break;
                }
                case WIN_HURT: {
                    sendActionBarNotif(trainer, new StringTextComponent(formatBattleMessage(ConfigHandler.messageAutoBattleResultWinHurt, trainerMon, wildMon)));
//                    trainer.sendMessage(new StringTextComponent(ConfigHandler.messageAutoBattleResultWinHurt), trainer.getUUID());
                    break;
                }
                case DRAW: {
                    sendActionBarNotif(trainer, new StringTextComponent(formatBattleMessage(ConfigHandler.messageAutoBattleResultDraw, trainerMon, wildMon)));
//                    trainer.sendMessage(new StringTextComponent(ConfigHandler.messageAutoBattleResultDraw), trainer.getUUID());
                    break;
                }
                case DEFEAT: {
                    sendActionBarNotif(trainer, new StringTextComponent(formatBattleMessage(ConfigHandler.messageAutoBattleResultDefeat, trainerMon, wildMon)));
//                    trainer.sendMessage(new StringTextComponent(ConfigHandler.messageAutoBattleResultDefeat), trainer.getUUID());
                    break;
                }
            }





            //Successful battle. Has drops and exp.
            if (result == BattleResultType.WIN || result == BattleResultType.WIN_HURT) {
                //We have to hurt the mon in order to proc the drops and XP
                EXPHandler.handleExpCalc(wildMon.getPokemon(), trainerMon.getPokemon(), (ServerPlayerEntity) trainerMon.getOwner());
                EXPHandler.handlePlayerExp((ServerPlayerEntity) trainerMon.getOwner(), wildMon);
                BattleHandler.handleDrops(this);
                wildMon.kill();
            }
            // Didn't win but damaged the opponent
            else if (result == BattleResultType.DRAW) {
            }

            if (trainerMon.getPokemon().getHealthPercentage() <= 5.0f) {
                trainer.sendMessage(new StringTextComponent(formatBattleMessage(ConfigHandler.messageLowHealthReturn, trainerMon, wildMon)), trainer.getUUID());
            }
        }

    }

    public static class BattleHandler {

        //The primary arbitrator for AutoBattleInstances and such
        public static void toggleAutoBattle(ServerPlayerEntity player) {
            if (BattleRegistry.getBattle(player) != null) {
                //The player is in a battle, cancel
//                PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Can't auto battle...!\n\t%s", StorageProxy.getParty(player).getSelectedSlot()));
                player.sendMessage(new StringTextComponent(ConfigHandler.messageAutoBattleFail), player.getUUID());
                return;
            }
            Pokemon pokemon =  StorageProxy.getParty(player).getSelectedPokemon();
            PixelmonEntity pixelmonEntity = pokemon.getPixelmonEntity().orElse(null);
            if (pixelmonEntity == null) {
//                PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("No pokemon present...!\n\t%s\n\t%s", pokemon, pixelmonEntity));
                player.sendMessage(new StringTextComponent(ConfigHandler.messageSelectedNotActive), player.getUUID());
                return;
            }
//            if (!ConfigHandler.allowMultiplePartyMons.get() && hasAutoBattlingMon(player, StorageProxy.getParty(player).getSelectedSlot())) {
            if (hasAutoBattlingMon(player, StorageProxy.getParty(player).getSelectedSlot())) {
                //Player has multiple mons but we only allow one
                player.sendMessage(new StringTextComponent(ConfigHandler.messageAlreadyActive), player.getUUID());
                return;
            }
            boolean isEnabled = NBTHandler.getTag(pixelmonEntity, NBTHandler.autoBattleEnableTag);
            setAutoBattle(player, pixelmonEntity, !isEnabled);
        }

        public static void setAutoBattle(ServerPlayerEntity player, PixelmonEntity pixelmonEntity, boolean value) {
            if (value) {
                //We can handle making a new instance here because we can also destroy it
                AutoBattleInstance instance = new AutoBattleInstance(pixelmonEntity);
                AutoBattleInstance.addAutoBattleInstance(pixelmonEntity.getUUID(), instance);
                sendActionBarNotif(player, new StringTextComponent(ConfigHandler.messageAutoBattleEnable));
                player.playSound(SoundEvents.ARROW_HIT, 0.5f, 1.99f);
            }
            else {
                AutoBattleInstance.removeAutoBattleInstance(pixelmonEntity.getUUID(), true);
                sendActionBarNotif(player, new StringTextComponent(ConfigHandler.messageAutoBattleDisable));
                player.playSound(SoundEvents.ARROW_HIT, 0.5f, 0.49f);
            }
            NBTHandler.setTag(pixelmonEntity, NBTHandler.autoBattleEnableTag, value);
        }

        //Doesn't validate an auto battler but checks if they have the tags
        public static boolean isWildAutoBattler(LivingEntity entity) {
            if (entity == null) {
                return false;
            }
            if (!(entity instanceof PixelmonEntity)) {
                return false;
            }
            if (((PixelmonEntity) entity).getOwner() != null) {
                return false;
            }
            boolean battling =  (NBTHandler.getTag(entity, NBTHandler.autoBattlingTag));
            //Most recent change is adding isWildPokemon()... it might not work for detecting random wild pixelmonentities
            return  (NBTHandler.getTag(entity, NBTHandler.autoBattlingTag));
        }

        //Checks the player's whole party for an autobattling mon
        //Offers to ignore the currently selected slot since we only care about extraneous mons
        public static boolean hasAutoBattlingMon(ServerPlayerEntity player, int ignoreSlot) {
            PlayerPartyStorage party = StorageProxy.getParty(player);
            for (int i = 0; i < 6; i++) {
                Pokemon mon = party.get(i);
                if (mon == null || ignoreSlot == i) {
                    continue;
                }
                Optional<PixelmonEntity> pixelmon = mon.getPixelmonEntity();
                if (!pixelmon.isPresent()) {
                    continue;
                }
                if (NBTHandler.getTag(pixelmon.get(), NBTHandler.autoBattleEnableTag)) {
                    return true;
                }
            }
            return false;
        }

        // Handles damage from autobattle and gets the result of the battle.
        public static BattleResultType calculateBattleResult(PixelmonEntity trainerMon, PixelmonEntity wildMon) {
            BattleResultType result;
            Pokemon tmon = trainerMon.getPokemon();
            Pokemon wmon = wildMon.getPokemon();
            int levelDifference = (tmon.getPokemonLevel() - wmon.getPokemonLevel());
            float sendEffectiveness = 0.0f;
            float receiveEffectiveness = 0.0f;
            for (Element type : tmon.getForm().getTypes()) {
                sendEffectiveness += Element.getTotalEffectiveness(wmon.getForm().getTypes(), type);
            }
            for (Element type : wmon.getForm().getTypes()) {
                receiveEffectiveness += Element.getTotalEffectiveness(tmon.getForm().getTypes(), type);
            }
            // Landslide level difference in favor
            if (levelDifference >= ConfigHandler.landslideLevelDifference.get()) {
                if (sendEffectiveness >= Effectiveness.Normal.value) {
                    result = BattleResultType.WIN;
                }
                else {
                    result = BattleResultType.WIN_HURT;
                }
            }
            // Landslide difference in defeat
            else if (levelDifference <= -ConfigHandler.landslideLevelDifference.get()) {
                if (sendEffectiveness > Effectiveness.Normal.value) {
                    result = BattleResultType.DRAW;
                }
                else {
                    result = BattleResultType.DEFEAT;
                }
            }
            // Match or higher than opponent
            else if (levelDifference >= 0) {
                if (sendEffectiveness > Effectiveness.Super.value) {
                    result = BattleResultType.WIN;
                }
                else if (sendEffectiveness > Effectiveness.Normal.value) {
                    result = RandomHelper.getRandomChance(4) ? BattleResultType.WIN_HURT : BattleResultType.WIN;
                }
                else {
                    result = RandomHelper.getRandomChance(4) ? BattleResultType.DRAW : BattleResultType.WIN_HURT;
                }
            }
            // Lower than opponent
            else {
                if (sendEffectiveness > Effectiveness.Super.value) {
                    result = RandomHelper.getRandomChance(4) ? BattleResultType.DRAW : BattleResultType.WIN_HURT;
                }
                else if (sendEffectiveness >= Effectiveness.Normal.value) {
                    result = RandomHelper.getRandomChance(4) ? BattleResultType.DEFEAT : BattleResultType.DRAW;
                }
                else {
                    result = RandomHelper.getRandomChance(2) ? BattleResultType.DRAW : BattleResultType.WIN_HURT;
                }
            }

            boolean isLandslide = levelDifference >= ConfigHandler.landslideLevelDifference.get();
    //        PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Battle Results:\n\tLandslide: %s\n\tSend Effectiveness: %s\n\tRec. Effectiveness: %s", isLandslide, sendEffectiveness, receiveEffectiveness));

            if (result == BattleResultType.WIN) {
                return result;
            }
            float minHealth = ConfigHandler.allowFaintFromAutoBattle.get() ? 0.0f : 1.0f;
            double tMonDamage = ConfigHandler.baseDamageHealthPercent.get() * receiveEffectiveness * (Math.abs((double)(100 - levelDifference)) / 100);
            tmon.setHealthPercentage((float) Math.max(minHealth, tmon.getHealthPercentage() - tMonDamage));
            //In a draw the opponent is still damaged
            if (result == BattleResultType.DRAW) {
                double wMonDamage = ConfigHandler.baseDamageHealthPercent.get() * sendEffectiveness * (Math.abs((double)(100 - levelDifference)) / 100);
                wmon.setHealthPercentage((float) Math.max(minHealth, wmon.getHealthPercentage() - wMonDamage));
            }
            return result;
        }

        public static boolean isValidAutoBattle(PixelmonEntity pixelmon) {
            return !pixelmon.isDeadOrDying() && (pixelmon.battleController == null) && !pixelmon.isFlying() &&
                    !pixelmon.isSwimming() && !pixelmon.isBossPokemon() && !pixelmon.getPokemon().isShiny() &&
                    !pixelmon.getPokemon().isLegendary() && !NBTHandler.getTag(pixelmon, NBTHandler.autoBattlingTag) &&
                    ConfigHandler.validateSpawnerTarget(pixelmon) && ConfigHandler.validateSpecTarget(pixelmon.getPokemon());
        }

        //This handles the item drops, it's meant to be called in the itemdropsevent.
        //Why? Because it's easier to act on the existing drops than to cancel it and simulate our own.
        public static void handleDrops(AutoBattleInstance instance) {
            if (ConfigHandler.itemDropMethod == ConfigHandler.DROP_METHOD.NONE) {
                return;
            }
            List<ItemStack> items = DropItemRegistry.getDropsForPokemon(instance.wildMon);
            ServerPlayerEntity playerOwner = (ServerPlayerEntity) instance.trainerMon.getOwner();
            if (playerOwner == null) {
                return;
            }
            switch(ConfigHandler.itemDropMethod) {
                case MAGNET: {
                    for (ItemStack item : items) {
                        DropItemHelper.dropItemOnGround(playerOwner.position(), playerOwner, item, true, false);
                    }
                    break;
                }
                case GIVE: {
                    for (ItemStack item : items) {
                        boolean added = playerOwner.inventory.add(item);
                        if (!added & !item.isEmpty()) {
                            DropItemHelper.dropItemOnGround(playerOwner.position(), playerOwner, item, true, true);
                        }
                    }
                    break;
                }
                case GROUND: {
                    for (ItemStack item : items) {
                        DropItemHelper.dropItemOnGround(instance.wildMon.position(), playerOwner, item, false, false);
                    }
                    break;
                }
            }
        }
    }

    public static class EXPHandler {

        //Basically just a copy from the actual player EXP code lol
        public static int calcPlayerExp(PixelmonEntity pixelmon) {
            if (pixelmon == null) {
                return 0;
            }
            int opponentPixelmonLevel = pixelmon.getPokemon().getPokemonLevel();
            int expAmount = 1;
            int divisor = 5;
            if (opponentPixelmonLevel >= 75) {
                expAmount = opponentPixelmonLevel / (divisor * 5);
            } else if (opponentPixelmonLevel >= 50) {
                expAmount = opponentPixelmonLevel / (divisor * 4);
            } else if (opponentPixelmonLevel >= 35) {
                expAmount = opponentPixelmonLevel / (divisor * 2);
            } else if (opponentPixelmonLevel > divisor) {
                expAmount = opponentPixelmonLevel / divisor;
            }
            return expAmount;
        }

        public static void handlePlayerExp(ServerPlayerEntity player, PixelmonEntity wild) {
            if (ConfigHandler.expDropMethod == ConfigHandler.DROP_METHOD.NONE || ConfigHandler.expDropMethod == ConfigHandler.DROP_METHOD.GROUND) {
                return;
            }
            int xp = calcPlayerExp(wild);
            switch(ConfigHandler.expDropMethod) {
                case GIVE: {
                    player.giveExperiencePoints(xp);
                    return;
                }
                case MAGNET: {
                    ExperienceOrbEntity xpOrb = new ExperienceOrbEntity(player.level, player.getX(), player.getY(), player.getZ(), xp);
                    wild.level.addFreshEntity(xpOrb);
                    return;
                }
                case GROUND: {
                    ExperienceOrbEntity xpOrb = new ExperienceOrbEntity(wild.level, wild.getX(), wild.getY(), wild.getZ(), xp);
                    wild.level.addFreshEntity(xpOrb);
                    return;
                }
            }
        }

        public static boolean enabledExpAll(ServerPlayerEntity player) {
            for (ItemStack item : player.inventory.items) {
                if (item != null && item.getItem() instanceof ExpAllItem && ExpAllItem.isActivated(item)) {
                    return true;
                }
            }
            return false;
        }

        public static double applyLevelEvoMult(Pokemon mon, double exp) {
            for (Evolution e : mon.getForm().getEvolutions()) {
                if (e instanceof LevelingEvolution && mon.getPokemonLevelContainer().getPokemonLevel() >= ((LevelingEvolution) e).getPokemonLevel()) {
                    exp *= 1.2;
                }
            }
            return exp;
        }

        public static void handleExpCalc(Pokemon defeated, Pokemon trainerMon, ServerPlayerEntity player) {
            boolean hasExpAll = enabledExpAll(player);

            applyExp(defeated, trainerMon, 1.0);

            PlayerPartyStorage party = StorageProxy.getParty(player);

            for (int i = 0; i < 6; i++) {
                Pokemon mon = party.get(i);
                if (mon == null || mon.getUUID() == trainerMon.getUUID() || !(hasExpAll || mon.getHeldItemAsItemHeld().getHeldItemType() == EnumHeldItems.expShare)) {
                    continue;
                }
                applyExp(defeated, mon, partyDistributeXPFactor);
            }
        }

        public static void applyExp(Pokemon defeated, Pokemon trainerMon, double factor) {
            if (ConfigHandler.stopXpBeforeLevelUp.get() && (trainerMon.getExperienceToLevelUp() <= 5)) {
                trainerMon.getPokemonLevelContainer().awardEXP(0, ConfigHandler.exp_gain_type);
                return;
            }
            //Starting xp
            double baseEXP = defeated.getForm().getSpawn().getBaseExp();
            double trainerMultiplier = trainerMon.getOriginalTrainer().equals(trainerMon.getDisplayName()) ? 1.0 : 1.5;
            double eggMultiplier = trainerMon.getHeldItemAsItemHeld().getHeldItemType() == EnumHeldItems.luckyEgg ? 1.5 : 1.0;
            int faintedLevel = defeated.getPokemonLevel();
            //Calculated xp
//            double exp = a * t * baseExp * eggMultiplier * (double)captureCombo * faintedLevel * scaleFactor / 7.0;
            int xp = (int)(baseEXP * factor * ConfigHandler.exp_multiplier.get() * faintedLevel * trainerMultiplier * eggMultiplier / 7.0);
            xp = ConfigHandler.stopXpBeforeLevelUp.get() ? Math.min(trainerMon.getExperienceToLevelUp()-5, xp) : xp;
            trainerMon.getPokemonLevelContainer().awardEXP(xp, ConfigHandler.exp_gain_type);
        }
    }

    public static class NBTHandler {
        public static final int BYTE_TAG = 1;
        public static final String autoBattleEnableTag = "autoBattleEnabled";
        public static final String autoBattlingTag = "autoBattling";

        public static void setTag(LivingEntity entity, String tag, boolean value) {
            CompoundNBT nbt = entity.getPersistentData();
            nbt.putBoolean(tag, value);
        }
        public static boolean getTag(LivingEntity entity, String tag) {
            CompoundNBT nbt = entity.getPersistentData();
//            if (!nbt.contains(tag)) {
//                return false;
//            }
            return nbt.getBoolean(tag);
        }

    }
}
