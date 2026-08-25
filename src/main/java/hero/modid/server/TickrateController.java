package hero.modid.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Util;

public final class TickrateController {
    private static boolean active = false;
    private static long endAtMs = 0;
    private static int previousRate = 20;

    public static void init(){
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!active) return;

            if (Util.getMeasuringTimeMs() >= endAtMs){
                setTickRate(server, previousRate);
                active = false;
            }

        });
    }

    public static void activate(MinecraftServer server, int newRate, long durationMs){
        long now = Util.getMeasuringTimeMs();

        if (!active) {
            previousRate = 20;
            setTickRate(server, newRate);
            active = true;
        }

        endAtMs = Math.max(endAtMs, now + durationMs);
    }

    private static void setTickRate(MinecraftServer server, int rate){
        ServerCommandSource src = server.getCommandSource();

        try {
            server.getCommandManager()
                    .getDispatcher()
                    .execute("tick rate " + rate, src);
        } catch (CommandSyntaxException e) {
            System.err.println("Failed to execute tick rate: " + e.getMessage());
        };
    }
    private TickrateController() {}
}
