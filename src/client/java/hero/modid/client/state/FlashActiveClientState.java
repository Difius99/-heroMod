package hero.modid.client.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlashActiveClientState {
    private static final Map<UUID, Long> ACTIVE_UNTIL_MS = new ConcurrentHashMap<>();
    private static  final  Map<UUID, Long> COOLDOWN_UNTIL_MS = new ConcurrentHashMap<>();

    private static final long DURATION_MS = 2000;
    private static final long COOLDOWN = 4000;

    public static void activate(UUID uuid) {
        ACTIVE_UNTIL_MS.put(uuid, System.currentTimeMillis() + DURATION_MS);
        COOLDOWN_UNTIL_MS.put(uuid, System.currentTimeMillis() + COOLDOWN);
    }

    public static boolean isActive(UUID uuid) {
        return System.currentTimeMillis() < ACTIVE_UNTIL_MS.getOrDefault(uuid, 0L);
    }

    public static long getCooldown(UUID uuid) {
        return COOLDOWN_UNTIL_MS.getOrDefault(uuid, 0L) - System.currentTimeMillis();
    }

    public static void clear(){
        ACTIVE_UNTIL_MS.clear();
        COOLDOWN_UNTIL_MS.clear();
    }

    private  FlashActiveClientState() {}
}
