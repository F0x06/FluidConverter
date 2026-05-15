package me.f0x.fluidconverter.blockentity;

public enum RedstoneMode {
    IGNORED,
    ACTIVE_WITH_SIGNAL,
    ACTIVE_WITHOUT_SIGNAL;

    public RedstoneMode next() {
        RedstoneMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static RedstoneMode fromName(String name) {
        if (name == null) return IGNORED;
        try {
            return RedstoneMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return IGNORED;
        }
    }

    public boolean shouldRun(boolean hasSignal) {
        return switch (this) {
            case IGNORED -> true;
            case ACTIVE_WITH_SIGNAL -> hasSignal;
            case ACTIVE_WITHOUT_SIGNAL -> !hasSignal;
        };
    }

    public String translationKey() {
        return "gui.fluidconverter.admin.redstone." + name().toLowerCase();
    }
}
