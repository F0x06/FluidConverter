package me.f0x.fluidconverter.blockentity;

public enum SideConfig {
    NONE,
    INPUT,
    OUTPUT;

    public SideConfig next() {
        return switch (this) {
            case NONE -> INPUT;
            case INPUT -> OUTPUT;
            case OUTPUT -> NONE;
        };
    }

    public static SideConfig fromName(String name) {
        if (name == null) return NONE;
        try { return SideConfig.valueOf(name); }
        catch (IllegalArgumentException e) { return NONE; }
    }
}
