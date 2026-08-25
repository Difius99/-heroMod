package hero.modid.server;

import hero.modid.HeroMod;
import hero.modid.hero.HeroManager;
import hero.modid.hero.HeroPowers;
import hero.modid.hero.HeroType;
import hero.modid.hero.power.FlashPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HeroPowerHandler {

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                final HeroType heroType = HeroManager.getHeroType(player);
                HeroPowers.getPower(heroType).serverTick(player);
            }
        });
    }
}
