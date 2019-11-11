package zeroneye.enemyz;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final Config CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    public final ForgeConfigSpec.DoubleValue iconSize;
    public final ForgeConfigSpec.DoubleValue yOffset;
    public final ForgeConfigSpec.DoubleValue distance;

    public Config(ForgeConfigSpec.Builder builder) {
        this.iconSize = builder.comment("Change the icon size.")
                .defineInRange("icon_size", 1.0D, 0.0D, 1.0D);
        this.yOffset = builder.comment("Move the icon up and down.")
                .defineInRange("icon_y_offset", 0.0D, -1.0D, 1.0D);
        this.distance = builder.comment("Max distance to render the skull icon.")
                .defineInRange("max_render_distance", 32.0D, 1.0D, 10000.0D);
    }

    static {
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        CONFIG_SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }
}
