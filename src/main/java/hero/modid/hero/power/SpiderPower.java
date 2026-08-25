package hero.modid.hero.power;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SpiderPower implements HeroPower {

    private static final double RANGE = 60.0;
    private static final int WEB_MAX_TICKS = 20 * 4;
    private static final int COOLDOWN_TICKS = 10;

    private static final double PULL_STRENGTH = 1.0;  // базовая "подтяжка" к якорю
    private static final double REEL_SPEED = 0.15;     // скорость подмотки (уменьшение длины)
    private static final double STOP_DIST = 1.8;       // если близко к якорю — отцепляемся
    private static final double MAX_SPEED = 3.15;      // ограничение скорости (иначе moved too quickly)

    private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, WebState> webByPlayer = new ConcurrentHashMap<>();

    @Override
    public boolean use(ServerPlayerEntity player) {
        long now = player.getEntityWorld().getTime();
        UUID uuid = player.getUuid();

        if (webByPlayer.remove(uuid) != null) {
            return true;
        }

        long cd = cooldownUntil.getOrDefault(uuid, 0L);
        if (now < cd) {
            float seconds = (cd - now) / 20.0f;
            player.sendMessage(Text.literal("Web cooldown: " + seconds + "s"), true);
            return false;
        }

        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(RANGE));

        HitResult hit = player.getEntityWorld().raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendMessage(Text.literal("No block to web"), true);
            return false;
        }

        Vec3d anchor = hit.getPos();

        Vec3d playerPos = player.getEntityPos().add(0, player.getStandingEyeHeight() * 0.8, 0);
        double length = anchor.distanceTo(playerPos);

        webByPlayer.put(uuid, new WebState(anchor, length, now + WEB_MAX_TICKS, false));
        cooldownUntil.put(uuid, now + COOLDOWN_TICKS);
        return true;
    }

    @Override
    public void serverTick(ServerPlayerEntity player) {
        WebState s = webByPlayer.get(player.getUuid());
        if (s == null) return;

        long now = player.getEntityWorld().getTime();
        if (now > s.untilTick) {
            webByPlayer.remove(player.getUuid());
            return;
        }

        Vec3d playerPos = player.getEntityPos().add(0, player.getStandingEyeHeight() * 0.8, 0);
        Vec3d r = s.anchor.subtract(playerPos);
        double dist = r.length();

        if (dist < STOP_DIST) {
            webByPlayer.remove(player.getUuid());
            return;
        }

        Vec3d dir = r.normalize();
        Vec3d v = player.getVelocity();

        double radial = v.dotProduct(dir);
        Vec3d tangentV = v.subtract(dir.multiply(radial));

        double length = s.length;

        if (s.holding) {
            length = Math.max(2.5, length - REEL_SPEED);
        }

        double stretch = dist - length;
        Vec3d pull;

        if (stretch > 0) {
            pull = dir.multiply(Math.min(0.35, stretch * 0.25));
        } else {
            pull = dir.multiply(PULL_STRENGTH * (s.holding ? 1.6 : 1.0));
        }

        double BOOST = s.holding ? 0.08 : 0.02;
        Vec3d tangentDir = tangentV.lengthSquared() > 1e-6 ? tangentV.normalize() : Vec3d.ZERO;

        Vec3d newV = tangentV.add(pull).add(tangentDir.multiply(BOOST));

        double sp2 = newV.lengthSquared();
        if (sp2 > MAX_SPEED * MAX_SPEED) {
            newV = newV.normalize().multiply(MAX_SPEED);
        }

        player.setVelocity(newV);
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.fallDistance = 0;

        if (length != s.length) {
            webByPlayer.put(player.getUuid(), s.withLength(length));
        }

        if (player.getEntityWorld() instanceof ServerWorld sw && (player.age % 2 == 0)) {
            spawnLine(sw, playerPos, s.anchor, 12);
        }
    }

    @Override
    public void onDisconnect(ServerPlayerEntity player){
        cooldownUntil.remove(player.getUuid());
        webByPlayer.remove(player.getUuid());
    }

    private static void spawnLine(ServerWorld world, Vec3d a, Vec3d b, int points) {
        Vec3d step = b.subtract(a).multiply(1.0 / points);
        Vec3d p = a;
        for (int i = 0; i <= points; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            p = p.add(step);
        }
    }

    private record WebState(Vec3d anchor, double length, long untilTick, boolean holding) {
        WebState withLength(double l) { return new WebState(anchor, l, untilTick, holding); }
    }

}