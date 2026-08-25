package hero.modid.hero;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeroManager {
    private   HeroManager() {}

    private static final Map<UUID, HeroType> HERO_BY_PLAYER = new ConcurrentHashMap<>();

    public static HeroType getHeroType(ServerPlayerEntity player) {
        return HERO_BY_PLAYER.getOrDefault(player.getUuid(), HeroType.DEFAULT);
    }

    public static void setHeroType (ServerPlayerEntity player, HeroType heroType) {
        HERO_BY_PLAYER.put(player.getUuid(), heroType);
    }

    public static void removeHeroType (ServerPlayerEntity player) {
        HERO_BY_PLAYER.remove(player.getUuid());
    }
}
