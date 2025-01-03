package supermemnon.pixelmonaddon;

import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.api.enums.ExperienceGainType;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigHandler {
    public static ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec configSpec = null;
    enum DROP_METHOD {
        GROUND,
        MAGNET,
        GIVE,
        NONE
    }
    enum TARGET_TYPE {
        NONE,
        BLACKLIST,
        WHITELIST
    }
    public static String messageAlreadyActive = "You already have a pokemon autobattling!";
    public static String messageSelectedNotActive = "The selected pokemon isn't sent out!";
    public static String messageAutoBattleEnable = "Auto Battling ON";
    public static String messageAutoBattleDisable = "Auto Battling OFF";
    public static String messageAutoBattleFail = "You can't auto battle right now!";
    public static String messageAutoBattleResultWin = "Your %p defeated a %w!";
    public static String messageAutoBattleResultWinHurt = "Your %p defeated a %w, but was hurt!";
    public static String messageAutoBattleResultDraw = "Your %p fought a %w to a draw, and retreated!";
    public static String messageAutoBattleResultDefeat = "Your %p was defeated by a %w!";
    public static String messageLowHealthReturn = "Your %p tired itself out and returned!";
    public static String messageNoPermissionToggle = "You don't have permission to Autobattle.";

    public static ForgeConfigSpec.ConfigValue<String> itemDropMethodConf;
    public static DROP_METHOD itemDropMethod;
    public static ForgeConfigSpec.ConfigValue<String> expDropMethodConf;
    public static DROP_METHOD expDropMethod;
    public static ForgeConfigSpec.ConfigValue<Double> exp_multiplier;
    public static ExperienceGainType exp_gain_type = ExperienceGainType.UNKNOWN;
    public static ForgeConfigSpec.ConfigValue<Integer> autoBattleSeekRange;
//    public static ForgeConfigSpec.ConfigValue<Boolean> allowMultiplePartyMons;
    public static ForgeConfigSpec.ConfigValue<Integer> landslideLevelDifference;
    public static ForgeConfigSpec.ConfigValue<Double> baseDamageHealthPercent;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowFaintFromAutoBattle;
    public static ForgeConfigSpec.ConfigValue<Boolean> stopXpBeforeLevelUp;
    public static ForgeConfigSpec.ConfigValue<Boolean> useHappinessTimer;
    public static ForgeConfigSpec.ConfigValue<Integer> minimumHappinessRequired;

    public static ForgeConfigSpec.ConfigValue<String> spawnerTargetTypeConf;
    public static TARGET_TYPE spawnerTargetType = TARGET_TYPE.NONE;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> specTargetListConf;
    public static ForgeConfigSpec.ConfigValue<String> specTargetTypeConf;
    public static TARGET_TYPE specTargetType = TARGET_TYPE.NONE;

    /*
    Here's the actual config handling lol
     */
    public static void initConfig() {
        configBuilder.push("AutoBattle Config");

        exp_gain_type = ExperienceGainType.UNKNOWN;
        itemDropMethodConf = configBuilder
                .comment("\nHow to handle items dropped by autobattled pokemon." +
                        "\nValid values are GROUND (drops on the ground at defeated mon), MAGNET (teleports item drop to trainer), GIVE (puts items in inventory), and NONE (cancels drops)." +
                        "\nDefault value is GROUND")
                .define("item-drop-method", DROP_METHOD.MAGNET.toString());
        expDropMethodConf = configBuilder
                .comment("\nHow to handle regular minecraft XP dropped by autobattled pokemon." +
                        "\nValid values are GROUND (drops on the ground at defeated mon), MAGNET (teleports orbs to trainer), GIVE (adds to xp bar), and NONE (cancels xp)." +
                        "\nDefault value is GROUND.")
                .define("exp-drop-method", DROP_METHOD.MAGNET.toString());
        exp_multiplier = configBuilder
                .comment("\nPokemon XP multiplier based on defeated pokemon's dropped XP." +
                        "\nDefault value is 0.5")
                .define("exp-multiplier", 0.5);
        autoBattleSeekRange = configBuilder
                .comment("\nRange in blocks that a trainer's pokemon can search for wild pokemon to autobattle. Higher values may lag." +
                        "\nDefault value is 32")
                .define("auto-battle-seek-range", 32);
//        allowMultiplePartyMons = configBuilder
//                .comment("\nWhether players can have multiple sent-out pokemon autobattle." +
//                        "\nDefault value is false")
//                .define("allow-multi-party", false);
        landslideLevelDifference = configBuilder
                .comment("\nLevel difference before landslide victories can occur, guaranteeing a win." +
                        "\nAccepts ranges 0-100" +
                        "\nDefault value is 32")
                .defineInRange("landslide-level-difference", 25, 0, 100);
        baseDamageHealthPercent = configBuilder
                .comment("\nBase Percentage of health received as damage received from a defeat or draw. Calculated as smaller or higher based on level difference." +
                        "\nAccepts ranges 0.0-100.0" +
                        "\nDefault value is 5.0 (5 percent)")
                .defineInRange("base-damage-percent", 5.0f, 0.0f, 100.0f);
        allowFaintFromAutoBattle = configBuilder
                .comment("\nWhether autobattling pokemon can faint from receiving damage after defeat or draw. If false, they will retreat with 1% health." +
                        "\nDefault value is false")
                .define("allow-faint", false);
        stopXpBeforeLevelUp = configBuilder
                .comment("\nWhether levelup is prevented by XP received from autobattling. If false, XP gain is stopped at 5 points before levelup." +
                        "\nDefault value is false (pokemon can levelup)")
                .define("no-levelup", false);
        useHappinessTimer = configBuilder
                .comment("\nWhether autobattling pokemon slowly lose happiness as they autobattle (1 per minute)." +
                        "\nDefault value is false (pokemon do not lose happiness)")
                .define("enable-happiness-timer", false);
        minimumHappinessRequired = configBuilder
                .comment("\nAn autobattling pokemon will refuse to autobattle, or stop autobattling, if happiness is at or below this number." +
                        "\nDefault value is -1 (no minimum)")
                .define("minimum-happiness", -1);
        spawnerTargetTypeConf = configBuilder
                .comment("\nWhether there is special targeting of Pokemon created by Spawners." +
                        "\nValid values are NONE (no special targeting), BLACKLIST (will not target spawner pokemon), and WHITELIST (only targets spawner pokemon)." +
                        "\nDefault value is NONE.")
                .define("spawner-target-type", TARGET_TYPE.NONE.toString());
        specTargetTypeConf = configBuilder
                .comment("\nWhether there is special targeting of Pokemon based on a specification list." +
                        "\nValid values are NONE (will not use the list), BLACKLIST (will avoid matches with the list), and WHITELIST (only matches with the list)." +
                        "\nDefault value is NONE.")
                .define("spec-target-type", TARGET_TYPE.NONE.toString());
        specTargetListConf = configBuilder
                .comment("\nList of string specifications to validate, based on written pokemon spec (same as used in /pokespawn and /pokeedit)." +
                        "\nFollows the rules set by 'spec-target-type' for how to handle spec validation." +
                        "\nIf left empty, it will not be used.")
                .defineList("spec-target-list", Arrays.asList("species:bulbasaur", "otn:safari"), entry -> true);

//        configBuilder = configBuilder.pop();
        configSpec = configBuilder.build();
    }

    public static void postInitConfig() {
        itemDropMethod = DROP_METHOD.valueOf(itemDropMethodConf.get());
        expDropMethod = DROP_METHOD.valueOf(expDropMethodConf.get());
        spawnerTargetType = TARGET_TYPE.valueOf(spawnerTargetTypeConf.get());
        specTargetType = TARGET_TYPE.valueOf(specTargetTypeConf.get());

        if (specTargetListConf.get().isEmpty())
        {
            specTargetType = TARGET_TYPE.NONE;
        }
    }

    public static boolean validateSafariTarget(PixelmonEntity mon) {
        return true;
    }

    public static boolean validateSpawnerTarget(PixelmonEntity mon) {
        switch (spawnerTargetType) {
            case NONE: return true;
            case WHITELIST: {
                return mon.spawner != null;
            }
            case BLACKLIST: {
                return mon.spawner == null;
            }
        }
        return false;
    }

    public static boolean validateSpecTarget(Pokemon mon) {
        switch (specTargetType) {
            case NONE: return true;
            case BLACKLIST: {
                for (String specString : specTargetListConf.get()) {
                    PokemonSpecification spec = PokemonSpecificationProxy.create(specString);
                    if (spec.matches(mon)) {
                        return false;
                    }
                }
                return true;
            }
            case WHITELIST: {
                for (String specString : specTargetListConf.get()) {
                    PokemonSpecification spec = PokemonSpecificationProxy.create(specString);
                    if (!spec.matches(mon)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    //            name = query.substring(0, query.indexOf(" "));
    //            PokemonSpecification spec = PokemonSpecificationProxy.create(new String[]{query.substring(query.indexOf(" ") + 1) + " !egg"});
    //            results = storage.findAll(spec);
    //            ((List)results).removeIf(PokemonBase::isEgg);

}
