package me.f0x.fluidconverter.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue CONVERSION_RATE_MB_PER_TICK;
    private static final ModConfigSpec.IntValue TANK_CAPACITY_MB;
    private static final ModConfigSpec.BooleanValue ADMIN_MENU_ENABLED;
    private static final ModConfigSpec.BooleanValue ENERGY_ENABLED;
    private static final ModConfigSpec.IntValue ENERGY_CAPACITY_FE;
    private static final ModConfigSpec.IntValue ENERGY_COST_PER_MB;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("machine");
        CONVERSION_RATE_MB_PER_TICK = b
                .comment("Millibuckets converted per server tick when input and output are set.")
                .defineInRange("conversion_rate_mb_per_tick", 20, 1, 10_000);
        TANK_CAPACITY_MB = b
                .comment("Capacity of each internal tank, in millibuckets.")
                .defineInRange("tank_capacity_mb", 8_000, 1_000, 1_000_000);
        ADMIN_MENU_ENABLED = b
                .comment("If true, ops and creative players can open the admin panel (delete learned recipes). Set to false to hide the admin button entirely.")
                .define("admin_menu_enabled", true);
        b.pop();

        b.push("energy");
        ENERGY_ENABLED = b
                .comment("If true, the converter requires Forge Energy to operate. If false, energy is not consumed and the GUI bar is hidden.")
                .define("enabled", true);
        ENERGY_CAPACITY_FE = b
                .comment("Internal energy buffer (FE).")
                .defineInRange("capacity_fe", 100_000, 1_000, 1_000_000_000);
        ENERGY_COST_PER_MB = b
                .comment("Energy cost (FE) per millibucket of input fluid converted. Set to 0 to disable energy requirement.")
                .defineInRange("cost_per_mb", 10, 0, 100_000);
        b.pop();

        SPEC = b.build();
    }

    public static int conversionRateMbPerTick() {
        return CONVERSION_RATE_MB_PER_TICK.get();
    }

    public static int tankCapacityMb() {
        return TANK_CAPACITY_MB.get();
    }

    public static boolean adminMenuEnabled() {
        return ADMIN_MENU_ENABLED.get();
    }

    public static boolean energyEnabled() {
        return ENERGY_ENABLED.get();
    }

    public static int energyCapacityFe() {
        return ENERGY_CAPACITY_FE.get();
    }

    public static int energyCostPerMb() {
        return ENERGY_COST_PER_MB.get();
    }

    private Config() {}
}
