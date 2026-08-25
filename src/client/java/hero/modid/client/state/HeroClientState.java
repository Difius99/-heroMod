package hero.modid.client.state;

import hero.modid.hero.HeroType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeroClientState {
    private static final Map<UUID, HeroType> HERO_BY_UUID = new ConcurrentHashMap<>();

    public static HeroType get(UUID uuid) {
        return HERO_BY_UUID.getOrDefault(uuid, HeroType.DEFAULT);
    }

    public static void set(UUID uuid, HeroType type) {
        HERO_BY_UUID.put(uuid, type);
    }

    public static void clear() {
        HERO_BY_UUID.clear();
    }

    private HeroClientState() {}
}
