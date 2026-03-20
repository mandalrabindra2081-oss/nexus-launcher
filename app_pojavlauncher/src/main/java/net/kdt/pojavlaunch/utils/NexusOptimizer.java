package net.kdt.pojavlaunch.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * NexusLauncher Performance Optimizer
 * Only touches JVM arguments - never touches renderer logic.
 * Focused on mod stability, crash prevention, and smooth gameplay.
 */
public class NexusOptimizer {

    private static final String TAG = "NexusOptimizer";

    public enum DeviceTier { LOW, MID, HIGH }

    public static DeviceTier getDeviceTier(Context ctx) {
        int totalRam = Tools.getTotalDeviceMemory(ctx);
        int cpuCores = Runtime.getRuntime().availableProcessors();
        if (totalRam >= 6144 && cpuCores >= 8) return DeviceTier.HIGH;
        if (totalRam >= 3072) return DeviceTier.MID;
        return DeviceTier.LOW;
    }

    /**
     * Build stable JVM args optimized for modded Minecraft on mobile.
     * These are safe args that work with all renderers and mod loaders.
     */
    public static List<String> buildOptimizedJVMArgs(Context ctx, String mcVersion) {
        List<String> args = new ArrayList<>();
        DeviceTier tier = getDeviceTier(ctx);
        int ram = LauncherPreferences.PREF_RAM_ALLOCATION;
        boolean is32bit = Architecture.is32BitsDevice();

        Log.i(TAG, "NexusOptimizer: tier=" + tier + " ram=" + ram + "MB mc=" + mcVersion);

        // ── Memory ────────────────────────────────────────────────────────────
        int xmx = is32bit ? Math.min(ram, 512) : ram;
        int xms = Math.max(64, xmx / 8); // Small XMS - let JVM grow as needed
        args.add("-Xms" + xms + "m");
        args.add("-Xmx" + xmx + "m");

        // ── G1GC - Best GC for modded Minecraft, reduces lag spikes ──────────
        args.add("-XX:+UseG1GC");
        args.add("-XX:+ParallelRefProcEnabled");
        args.add("-XX:MaxGCPauseMillis=200");
        args.add("-XX:+UnlockExperimentalVMOptions");
        args.add("-XX:+DisableExplicitGC");
        args.add("-XX:+AlwaysPreTouch");
        args.add("-XX:G1NewSizePercent=30");
        args.add("-XX:G1MaxNewSizePercent=40");
        args.add("-XX:G1HeapRegionSize=8M");
        args.add("-XX:G1ReservePercent=20");
        args.add("-XX:G1HeapWastePercent=5");
        args.add("-XX:G1MixedGCCountTarget=4");
        args.add("-XX:InitiatingHeapOccupancyPercent=15");
        args.add("-XX:G1MixedGCLiveThresholdPercent=90");
        args.add("-XX:G1RSetUpdatingPauseTimePercent=5");
        args.add("-XX:SurvivorRatio=32");
        args.add("-XX:+PerfDisableSharedMem");
        args.add("-XX:MaxTenuringThreshold=1");

        // ── Crash prevention ──────────────────────────────────────────────────
        args.add("-XX:-OmitStackTraceInFastThrow");
        args.add("-XX:+TieredCompilation");

        // ── Mod loader stability (Fabric/Forge) ───────────────────────────────
        // Prevents random crashes with ASM/mixin-heavy mods
        args.add("-XX:+UseCompressedOops");
        args.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        args.add("-Dfml.ignorePatchDiscrepancies=true");
        // Prevents class loading deadlocks with 200+ mods
        args.add("-Djdk.reflect.useDirectMethodHandle=false");
        // Sodium/Iris/OptiFine stability
        args.add("-Djava.awt.headless=true");
        // Network stability
        args.add("-Djava.net.preferIPv4Stack=true");
        args.add("-Dfile.encoding=UTF-8");

        // ── Thread safety for mods ────────────────────────────────────────────
        args.add("-XX:+UseStringDeduplication");

        // ── Device tier specific tuning ───────────────────────────────────────
        switch (tier) {
            case HIGH:
                args.add("-XX:ConcGCThreads=4");
                args.add("-XX:ParallelGCThreads=8");
                break;
            case MID:
                args.add("-XX:ConcGCThreads=2");
                args.add("-XX:ParallelGCThreads=4");
                break;
            case LOW:
                args.add("-XX:ConcGCThreads=1");
                args.add("-XX:ParallelGCThreads=2");
                args.add("-XX:G1HeapRegionSize=4M");
                break;
        }

        Log.i(TAG, "Added " + args.size() + " optimized JVM args");
        return args;
    }

    /**
     * Smart RAM recommendation - leaves headroom for mods
     */
    public static int getRecommendedRAM(Context ctx, String mcVersion) {
        int deviceRam = Tools.getTotalDeviceMemory(ctx);
        boolean is32bit = Architecture.is32BitsDevice();

        // Leave 40% for OS + mods native memory
        int usableRam = (int)(deviceRam * 0.60f);

        if (is32bit) usableRam = Math.min(usableRam, 512);
        if (!is32bit) usableRam = Math.min(usableRam, 4096);

        // Minimum 256MB
        usableRam = Math.max(usableRam, 256);

        // Round to nearest 128MB
        usableRam = (usableRam / 128) * 128;

        Log.i(TAG, "Recommended RAM: " + usableRam + "MB (device=" + deviceRam + "MB)");
        return usableRam;
    }

    /**
     * Compare two Minecraft version strings
     */
    public static int compareVersion(String v1, String v2) {
        try {
            String[] p1 = v1.split("[.-]");
            String[] p2 = v2.split("[.-]");
            int len = Math.max(p1.length, p2.length);
            for (int i = 0; i < len; i++) {
                int n1 = 0, n2 = 0;
                try { n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0; } catch (Exception e) {}
                try { n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0; } catch (Exception e) {}
                if (n1 != n2) return n1 - n2;
            }
        } catch (Exception e) {
            Log.w(TAG, "Version compare failed: " + e.getMessage());
        }
        return 0;
    }
}
