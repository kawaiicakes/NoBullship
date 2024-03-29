package io.github.kawaiicakes.nobullship;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    public static ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec.DoubleValue COOLDOWN_MULTIPLIER, MINIMUM_COOLDOWN, MAXIMUM_COOLDOWN, DROP_RAW_PERCENT;
    public static ForgeConfigSpec.BooleanValue DISABLE_DROP, DROP_RAW, DISABLE_GLOBAL_COOLDOWN;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> DROP_WHITELIST, DROP_BLACKLIST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("No Bullship!");

        COOLDOWN_MULTIPLIER = builder
                .comment("The multiplier on cooldown times for schematics.")
                .translation("config.nobullship.cooldown")
                .comment("Default 1.0")
                .defineInRange("cooldown", 1.0, 0.01, 30);

        MINIMUM_COOLDOWN = builder
                .comment("The minimum cooldown time possible in seconds.")
                .translation("config.nobullship.min_cooldown")
                .comment("Default 1.0")
                .defineInRange("min_cooldown", 1.0, 0.50, 395);

        MAXIMUM_COOLDOWN = builder
                .comment("The maximum cooldown time possible in seconds.")
                .translation("config.nobullship.max_cooldown")
                .comment("Default 405")
                .defineInRange("max_cooldown", 405, 0.51, 3000);

        DROP_RAW_PERCENT = builder
                .comment("The percentage of blocks that will be dropped if an entity is destroyed. CURRENTLY DOES NOTHING")
                .comment("Ignored if raw_drops is disabled.")
                .translation("config.nobullship.raw_drops_percent")
                .comment("Default 67%.")
                .defineInRange("raw_drops_percent", 0.67, 0, 1.00);

        DISABLE_DROP = builder
                .comment("If enabled, will attempt to stop entities which have a multiblock recipe from dropping as items.")
                .translation("config.nobullship.default_drops")
                .comment("Default false.")
                .define("default_drops", false);

        DROP_RAW = builder
                .comment("Whether entities with a multiblock recipe drop a portion of the raw ingredients needed to make them. CURRENTLY DOES NOTHING")
                .translation("config.nobullship.raw_drops")
                .comment("Default true.")
                .define("raw_drops", true);

        DISABLE_GLOBAL_COOLDOWN = builder
                .comment("Disables global cooldown. Not recommended.")
                .translation("config.nobullship.disable_global_cooldown")
                .comment("Default true.")
                .define("disable_global_cooldown", false);

        DROP_WHITELIST = builder
                .comment("Acts as a whitelist for drops if an item you needed was removed by default_drops.")
                .translation("config.nobullship.drop_whitelist")
                .defineList("drop_whitelist", ObjectArrayList.wrap(new String[]{}), (obj) ->
                        obj instanceof String string && ResourceLocation.isValidResourceLocation(string));

        DROP_BLACKLIST = builder
                .comment("Acts as a blacklist for drops if an item was missed by default_drops.")
                .translation("config.nobullship.drop_blacklist")
                .defineList("drop_blacklist", ObjectArrayList.wrap(new String[]{}), (obj) ->
                        obj instanceof String string && ResourceLocation.isValidResourceLocation(string));

        CONFIG = builder.build();
    }
}
