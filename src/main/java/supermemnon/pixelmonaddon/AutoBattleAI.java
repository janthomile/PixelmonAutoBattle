package supermemnon.pixelmonaddon;

import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.Level;

import java.util.EnumSet;

public class AutoBattleAI {
    public static class TrainerAutoBattleGoal extends Goal {
        //This class should function as a state machine between SeekMode (initial behaviour, seeking mons) and BattleMode (similar to WildBattleGoal ai, but also handles the timer and puppeteers the mons).
        enum GoalState {
            SEEKING,
            BATTLING,
            FOLLOW
        }
        public GoalState currentState = GoalState.SEEKING;
        AutoBattleHandler.AutoBattleInstance instance;
        PixelmonEntity owner;
        PixelmonEntity target = null;
        static public int maxHappinessTimerTicks = 1200;
        int happinessTimerTicks;
        boolean fatigued;
        /*
            Following Behaviour
         */
        static final float maxDistanceFromTrainer = 64.0f;
        static final int pathCooldownMax = 20;
        int pathCooldown = pathCooldownMax;
        /*
         Seeking Behaviour
        */
        static final float navRange = 3.0f;
        static final float navSpeed = 1.0f;
        static final int resetMaxTicks = 200;
        int resetTicks = resetMaxTicks;
        static final float resetMinDistance = 8.0f;
        /*
         Battling Behaviour
        */
        public static final float lookAtSpeed = 0.5f;
        static final int turnMaxTicks = 60;
        int turnTicks = turnMaxTicks;
        static final float jumpStrength = 0.5f;
        public TrainerAutoBattleGoal(AutoBattleHandler.AutoBattleInstance i, PixelmonEntity mon) {
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Added SeekWildMonGoal to %s", mon.getName().getString()));
            this.instance = i;
            this.owner = mon;
            this.happinessTimerTicks = maxHappinessTimerTicks;
            checkFatigued();
            AutoBattleHandler.NBTHandler.setTag(owner, AutoBattleHandler.NBTHandler.autoBattleEnableTag, false);
            setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE,Flag.LOOK,Flag.JUMP));
            swapFollowTrainer();
        }

        public void checkFatigued() {
            this.fatigued = !(owner.getPokemon().getHealthPercentage() >= 5.0f) || !(this.owner.getPokemon().getFriendship() > ConfigHandler.minimumHappinessRequired.get());
        }
        public void warpToTrainer() {
            if (owner.getOwner() == null) {
                return;
            }
            owner.setTarget(null);
            target = null;
            owner.teleportTo(owner.getOwner().getX(),owner.getOwner().getY(),owner.getOwner().getZ());
        }

        public void swapSeeking() {
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Swap to Seeking from" + currentState.toString()));
            resetTicks = resetMaxTicks;
            currentState = GoalState.SEEKING;
        }
        //Move to target within the reset time or warp back to trainer and switch to regular seeking behaviour.
        public void tickSeeking() {
            //Start by decrementing the cooldown
            resetTicks--;
            if (resetTicks < 1) {
                resetTicks = resetMaxTicks;
                if (owner.getOwner() != null && owner.distanceTo(owner.getOwner()) > resetMinDistance) {
                    warpToTrainer();
                }
                swapFollowTrainer();
            }

            //If target isn't null we can move
            PathNavigator nav = owner.getNavigation();
            //We can't path if we're riding something else
            if (owner.isPassenger()) {
                return;
            }
            if (target == null) {
                swapFollowTrainer();
                return;
            }
            //Tell the nav to go to the target
            nav.moveTo(target, navSpeed);
            //If we're in range, start the autobattle!
            //Change the state, init the turnTicks, and run the instance autobattle code
            if (owner.distanceTo(target) <= navRange) {
                swapBattling();
                return;
            }
            //If we're not at the target yet, we continue until we are
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, "Pathing...");
        }
        public void lunge(PixelmonEntity a, PixelmonEntity b) {
            a.knockback(jumpStrength, a.getX() - b.getX(), a.getZ() - b.getZ());
            a.doJump();
        }
        //Should be minor explosion effect, at very high pitch.
        public void startBattleFx() {
            owner.level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, SoundCategory.NEUTRAL,
                    0.5f, 0.5f);
            Vector3d pos = owner.position().add(target.position().subtract(owner.position()).scale(0.5));
            ((ServerWorld) owner.level).sendParticles(
                    ParticleTypes.EXPLOSION,
                    pos.x, pos.y + 1.0, pos.z,
                    1, //Count
                    0.1,0.1,0.1, //Spread
                    0.01); //Speed
        }
        public void swapBattling() {
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Swap to Battle from" + currentState.toString()));
            currentState = GoalState.BATTLING;
            turnTicks = turnMaxTicks;
            instance.startAutoBattle(target);
            startBattleFx();
        }

        public void tickBattling() {
            if (target == null) {
                swapSeeking();
                return;
            }
            owner.lookAt(EntityAnchorArgument.Type.EYES, target.position());
            target.lookAt(EntityAnchorArgument.Type.EYES, owner.position());
            turnTicks--;
            if ((turnTicks % 20) == 0) {
                lunge(owner, target);
                return;
            }
            if ((turnTicks % 30) == 0) {
                lunge(target, owner);
                return;
            }
            //Basic gate to block until battle is over
            if (turnTicks > 0) {
                return;
            }
            //turnTicks are finished, so we end the autobattle and switch to seeking
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, "End Autobattle: Lunge");
            instance.endAutoBattle();
            swapFollowTrainer();
        }
        public void swapFollowTrainer() {
            //Start with cooldown zero to find a target right away
//        PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Swap to Follow from" + currentState.toString()));
            pathCooldown = 0;
            currentState = GoalState.FOLLOW;
        }
        //Basically just follow the trainer until a target is found
        //Since this is the default state, and battle resets here, we try to find a target right away to try and chain battles
        public void tickFollowTrainer() {
            pathCooldown--;
            //If target is null we do stuff regarding finding a target
            if (target == null && !fatigued) {
                if (owner.getTarget() != null && (owner.getTarget() instanceof PixelmonEntity) && AutoBattleHandler.BattleHandler.isValidAutoBattle((PixelmonEntity) owner.getTarget())) {
                    target = (PixelmonEntity) owner.getTarget();
                    owner.setTarget(null);
                    swapSeeking();
                    return;
                }
                if (pathCooldown < 1) {
                    pathCooldown = pathCooldownMax;
                    if (!findNearestTarget()) {
                        return;
                    }
                    swapSeeking();
                    return;
                }
            }
            target = null;

            if (owner.getOwner() == null) {
                return;
            }
            //We can't path if we're riding something else
            if (owner.isPassenger()) {
                return;
            }
            float dist = owner.distanceTo(owner.getOwner());
            if (dist <= resetMinDistance) {
                return;
            }
            if (dist > maxDistanceFromTrainer) {
                warpToTrainer();
            }
            PathNavigator nav = owner.getNavigation();
            nav.moveTo(owner.getOwner(), navSpeed);
        }
        //So here we have to find the nearest available target, and then assign it to the target and instance.wildMon
        public boolean findNearestTarget() {
            PixelmonEntity nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            Vector3d centerPos = owner.position();
            int r = ConfigHandler.autoBattleSeekRange.get();
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("findNearestTarget:\n\tradius: %s\n\t", r));
            for (PixelmonEntity entity : owner.level.getEntitiesOfClass(PixelmonEntity.class, owner.getBoundingBox().expandTowards(r, 4.0, r))) {
                if (!AutoBattleHandler.BattleHandler.isValidAutoBattle(entity) || (entity == owner)) {
//                    PixelmonAutobattle.getLOGGER().log(Level.INFO, "\tfindNearestTarget: skipped self");
                    continue;
                }
                double distance = centerPos.distanceTo(entity.position());
                if (distance < nearestDistance) {
                    nearest = entity;
                    nearestDistance = distance;
                }
            }
            if (nearest == null) {
//                PixelmonAutobattle.getLOGGER().log(Level.INFO, "\tfindNearestTarget: nearest remained null");
                return false;
            }
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Set target to %s",target));
            target = nearest;
            instance.wildMon = nearest;
            return true;
        }
        @Override
        public boolean canUse() {
            return AutoBattleHandler.NBTHandler.getTag(owner, AutoBattleHandler.NBTHandler.autoBattleEnableTag);
        }
        @Override
        //Runs 1 sec timer which pathfinds to target if applicable
        public void tick() {
            switch(currentState) {
                case SEEKING: {
                    tickSeeking();
                    break;
                }
                case BATTLING: {
                    tickBattling();
                    break;
                }
                case FOLLOW: {
                    tickFollowTrainer();
                    return;
                }
            }
            if (ConfigHandler.useHappinessTimer.get()) {
                happinessTimerTicks--;
                if (happinessTimerTicks < 1) {
                    happinessTimerTicks = maxHappinessTimerTicks;
                    owner.getPokemon().decreaseFriendship(ConfigHandler.happinessTimerDecrement.get());
                    checkFatigued();
                }
            }
        }
    }
    //Basically just keeps the wild mon still
    public static class WildAutoBattleGoal extends Goal {
//        public static final int maxBattleTime = 100;
//        public int battleTime = maxBattleTime;
        PixelmonEntity owner;
        //Sets up the wild pokemon for autobattling
        public WildAutoBattleGoal(PixelmonEntity mon, PixelmonEntity wildMon) {
//            PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("Added AutoBattleGoal to %s", mon.getName().getString()));
            this.owner = mon;
            AutoBattleHandler.NBTHandler.setTag(owner, AutoBattleHandler.NBTHandler.autoBattlingTag, true);
            setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE,Flag.LOOK,Flag.JUMP));
        }
        @Override
        public boolean canUse() {
            return true;
        }
    }
}
