package hero.modid.client.render;

import hero.modid.client.state.FlashActiveClientState;
import hero.modid.client.state.HeroClientState;
import hero.modid.hero.HeroType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.util.math.Vec3d;

import java.util.*;


public final class FlashAfterImage {
    private static final Map<UUID, Deque<Ghost>> GHOSTS_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Vec3d> LAST_SPAWN_POS = new HashMap<>();
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();

    private static final int LIFE_TICKS = 3;

    private static final int MAX_GHOSTS = 8;

    private static final double BACK_OFFSET = 0.7;
    private static final double TICK_CORRECTION = 1.8;
    private static final double MIN_DIST = 0.2;

    public static void init(){
        ClientTickEvents.END_CLIENT_TICK.register(FlashAfterImage :: tick);
        WorldRenderEvents.AFTER_ENTITIES.register(FlashAfterImage :: render);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        Set<UUID> seen = new HashSet<>();
        for (AbstractClientPlayerEntity p : client.world.getPlayers()){
            UUID uuid = p.getUuid();
            seen.add(uuid);

            Deque<Ghost> deque = GHOSTS_BY_PLAYER.computeIfAbsent(uuid, k -> new ArrayDeque<>());
            deque.removeIf(g -> ++g.age > LIFE_TICKS);

            if (HeroClientState.get(uuid) != HeroType.FLASH) {
                GHOSTS_BY_PLAYER.remove(uuid);
                LAST_SPAWN_POS.remove(uuid);
                LAST_POS.remove(uuid);
                continue;
            }

            if (!FlashActiveClientState.isActive(uuid)) {
                GHOSTS_BY_PLAYER.remove(uuid);
                LAST_SPAWN_POS.remove(uuid);
                LAST_POS.remove(uuid);
                continue;
            }

            Vec3d curPos = new Vec3d(p.getX(), p.getY(), p.getZ());
            Vec3d prevPos = LAST_POS.put(uuid, curPos);
            if (prevPos == null) continue;

            Vec3d delta = curPos.subtract(prevPos);
            Vec3d deltaH = new Vec3d(delta.x, 0.0, delta.z);

            if (deltaH.lengthSquared() < 0.0004) {
                LAST_SPAWN_POS.remove(uuid);
                continue;
            }
            Vec3d pos = curPos
                    .subtract(deltaH.multiply(TICK_CORRECTION))
                    .subtract(deltaH.normalize().multiply(BACK_OFFSET));

            Vec3d last = LAST_SPAWN_POS.get(uuid);
            if (last != null && last.squaredDistanceTo(pos) < MIN_DIST * MIN_DIST) continue;
            LAST_SPAWN_POS.put(uuid, pos);

            var dispatcher = client.getEntityRenderDispatcher();
            @SuppressWarnings({"rawtypes", "unchecked"})

            var renderer = (net.minecraft.client.render.entity.EntityRenderer) dispatcher.getRenderer(p);

            var state = (EntityRenderState) renderer.createRenderState();
            renderer.updateRenderState(p, state, 0.0f);

            deque.addFirst(new Ghost(state, pos));
            while (deque.size() > MAX_GHOSTS) deque.removeLast();
        }

        GHOSTS_BY_PLAYER.keySet().removeIf(uuid -> !seen.contains(uuid));
        LAST_SPAWN_POS.keySet().removeIf(uuid -> !seen.contains(uuid));
        LAST_POS.keySet().removeIf(uuid -> !seen.contains(uuid));
    }

    private static void render(WorldRenderContext context) {
        if (GHOSTS_BY_PLAYER.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        var dispatcher = client.getEntityRenderDispatcher();
        var matrices = context.matrices();
        OrderedRenderCommandQueue queue = context.commandQueue();

        CameraRenderState cameraState = context.worldState().cameraRenderState;
        Vec3d camPos = cameraState.pos;

        for (Deque<Ghost> deque : GHOSTS_BY_PLAYER.values()) {
            for (Ghost g : deque) {
                double dx = g.position.x - camPos.x;
                double dy = g.position.y - camPos.y;
                double dz = g.position.z - camPos.z;

                dispatcher.render(g.state, cameraState, dx, dy, dz, matrices, queue);
            }
        }
    }

    private static final class Ghost {
        final EntityRenderState state;
        int age = 0;
        final Vec3d position;

        Ghost(EntityRenderState state, Vec3d position) {
            this.state = state;
            this.position = position;
        }
    }
    private FlashAfterImage() {}
}
