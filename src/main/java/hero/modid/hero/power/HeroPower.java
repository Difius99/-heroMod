package hero.modid.hero.power;

import net.minecraft.server.network.ServerPlayerEntity;

public interface HeroPower {
   default void serverTick(ServerPlayerEntity player){};

   default boolean use(ServerPlayerEntity player){return false;};

   default void onDeselected(ServerPlayerEntity player) {};

   default void onDisconnect(ServerPlayerEntity player) {};
}
