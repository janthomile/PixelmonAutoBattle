package supermemnon.pixelmonaddon;

import com.pixelmonmod.pixelmon.Pixelmon;
import jdk.nashorn.internal.runtime.regexp.joni.Config;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PixelmonAutobattle.MODID)
public class PixelmonAutobattle
{
    public static final String MODID = "pixelmonautobattle";
    private static final Logger LOGGER = LogManager.getLogger();
    private static PixelmonAutobattle instance;
    public static PixelmonAutobattle getInstance() {
        return instance;
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public PixelmonAutobattle() {
        ConfigHandler.initConfig();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        Pixelmon.EVENT_BUS.register(EventHandler.ModEvents.class);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.configSpec, "pixelmon-autobattle.toml");

    }

    private void setup(final FMLCommonSetupEvent event)
    {
        ConfigHandler.postInitConfig();
//        PermissionAPI.registerNode("pixelmonautobattle.autobattle.current", DefaultPermissionLevel.NONE, "");
//        PermissionAPI.registerNode("pixelmonautobattle.autobattle.multi", DefaultPermissionLevel.NONE, "");
    }
}
