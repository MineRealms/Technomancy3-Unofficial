package theflogat.technomancy.common.platform;

/** Compile-time platform identity; no runtime loader detection is needed for generated code. */
public final class PlatformVersion {
    //? if forge {
    public static final boolean FORGE = true;
    //?} else {
    /*public static final boolean FORGE = false;
    *///?}

    private PlatformVersion() {
    }
}
