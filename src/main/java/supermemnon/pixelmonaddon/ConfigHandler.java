package supermemnon.pixelmonaddon;

import com.pixelmonmod.pixelmon.api.enums.ExperienceGainType;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.Level;

public class ConfigHandler {
    public static ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec configSpec = null;
    enum DROP_METHOD {
        GROUND,
        MAGNET,
        GIVE,
        NONE;


    }
    public static String messageAlreadyActive = "You already have a pokemon autobattling!";
    public static String messageSelectedNotActive = "The selected pokemon isn't sent out!";
    public static String messageAutoBattleEnable = "Auto Battling ON";
    public static String messageAutoBattleDisable = "Auto Battling OFF";
    public static String messageAutoBattleFail = "You can't auto battle right now!";
    public static String messageAutoBattleResultWin = "Your %p defeated a %w!!";
    public static String messageAutoBattleResultWinHurt = "Your %p defeated a %w, but was hurt!";
    public static String messageAutoBattleResultDraw = "Your %p fought a %w to a draw, and retreated!";
    public static String messageAutoBattleResultDefeat = "Your %p was defeated by a %w!";
    public static String messageLowHealthReturn = "Your %p tired itself out and returned!";

    public static ForgeConfigSpec.ConfigValue<String> itemDropMethodConf;
    public static DROP_METHOD itemDropMethod;
    public static ForgeConfigSpec.ConfigValue<String> expDropMethodConf;
    public static DROP_METHOD expDropMethod;
    public static ForgeConfigSpec.ConfigValue<Double> exp_multiplier;
    public static ExperienceGainType exp_gain_type = ExperienceGainType.UNKNOWN;
    public static ForgeConfigSpec.ConfigValue<Integer> autoBattleSeekRange;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowMultiplePartyMons;
    public static ForgeConfigSpec.ConfigValue<Integer> landslideLevelDifference;
    public static ForgeConfigSpec.ConfigValue<Double> baseDamageHealthPercent;
    public static ForgeConfigSpec.ConfigValue<Boolean> allowFaintFromAutoBattle;
    public static ForgeConfigSpec.ConfigValue<Boolean> stopXpBeforeLevelUp;

    /*
    Here's the actual config handling lol
     */

    public static void initConfig() {
        configBuilder.push("AutoBattle Config");

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
        exp_gain_type = ExperienceGainType.UNKNOWN;
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

//        configBuilder = configBuilder.pop();
        configSpec = configBuilder.build();
    }

    public static void postInitConfig() {
        itemDropMethod = DROP_METHOD.valueOf(itemDropMethodConf.get());
        expDropMethod = DROP_METHOD.valueOf(expDropMethodConf.get());
        PixelmonAutobattle.getLOGGER().log(Level.INFO, String.format("PostInitConfig: Loaded Proper Values:\n\tItem: %s\n\tEXP: %s", itemDropMethod, expDropMethod));
    }

}
