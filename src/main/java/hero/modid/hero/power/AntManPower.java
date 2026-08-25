package hero.modid.hero.power;

import hero.modid.HeroMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntManPower implements HeroPower {

    private static final Identifier FALL_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_fall_damage");
    private static final Identifier REACH_MOD_ENTITY_ID = Identifier.of(HeroMod.MOD_ID, "antman_reach_entity");
    private static final Identifier REACH_MOD_BLOCK_ID = Identifier.of(HeroMod.MOD_ID, "antman_reach_block");
    private static final Identifier SCALE_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_scale");
    private static final Identifier DAMAGE_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_damage");
    private static final Identifier SPEED_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_speed");
    private static final Identifier HEALTH_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_health");
    private static final Identifier KNOCKBACK_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_knockback");
    private static final Identifier JUMP_MOD_ID = Identifier.of(HeroMod.MOD_ID, "antman_jump");


    public enum SizeMode {
        NORMAL(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
        BIG(4.0, 1.5, 2.2, 2.0, 0.8, 2.0, 3.0, 1.0),
        SMALL(0.1, 0.1, 1.0, 0.5, 2.0, 0.7, 0.4, 2.5);
        
        final double fallDmg;
        final double reachDistance;
        final double scale;
        final double damage;
        final double speed;
        final double health;
        final double knockback;
        final double jump;

        SizeMode(double scale, double fallDmg, double reachDistance, double damage, double speed, double health, double knockback, double jump) {
            this.scale = scale;
            this.fallDmg = fallDmg;
            this.reachDistance = reachDistance;
            this.damage = damage;
            this.speed = speed;
            this.health = health;
            this.knockback = knockback;
            this.jump = jump;
        }
    }

    private final Map<UUID, SizeMode> modeByPlayer = new ConcurrentHashMap<>();

    @Override
    public boolean use(ServerPlayerEntity player) {
        if (player.hasVehicle()) {
            player.stopRiding();
        }
        if (!player.getPassengerList().isEmpty()) {
            player.removeAllPassengers();
        }
        
        UUID uuid = player.getUuid();
        SizeMode current = modeByPlayer.getOrDefault(uuid, SizeMode.NORMAL);

        SizeMode next = switch (current) {
            case NORMAL -> SizeMode.BIG;
            case BIG -> SizeMode.SMALL;
            case SMALL -> SizeMode.NORMAL;
        };

        modeByPlayer.put(uuid, next);


        applyMultiplier(player, EntityAttributes.SCALE, SCALE_MOD_ID, next.scale);
        applyMultiplier(player, EntityAttributes.FALL_DAMAGE_MULTIPLIER, FALL_MOD_ID, next.fallDmg);
        applyMultiplier(player, EntityAttributes.ENTITY_INTERACTION_RANGE, REACH_MOD_ENTITY_ID, next.reachDistance + (next == SizeMode.BIG ? 0.65 : 0.0));
        applyMultiplier(player, EntityAttributes.BLOCK_INTERACTION_RANGE, REACH_MOD_BLOCK_ID, next.reachDistance);
        applyMultiplier(player, EntityAttributes.ATTACK_DAMAGE, DAMAGE_MOD_ID, next.damage);
        applyMultiplier(player, EntityAttributes.MOVEMENT_SPEED, SPEED_MOD_ID, next.speed);
        applyMultiplier(player, EntityAttributes.MAX_HEALTH, HEALTH_MOD_ID, next.health);
        applyMultiplier(player, EntityAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD_ID, next.knockback);
        applyMultiplier(player, EntityAttributes.JUMP_STRENGTH, JUMP_MOD_ID, next.jump);

        if(next == SizeMode.BIG){
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    30,
                    4,
                    true,
                    false,
                    false
            ));
        }
        player.sendMessage(Text.literal("Ant-Man mode: " + next.name()), true);
        return true;
    }

    @Override
    public void onDeselected(ServerPlayerEntity player){
        reset(player);
    }

    private static void applyMultiplier(ServerPlayerEntity player,
                                        RegistryEntry<EntityAttribute> attribute,
                                        Identifier modId,
                                        double multiplier) {
        EntityAttributeInstance inst = player.getAttributeInstance(attribute);
        if (inst == null) return;

        var old = inst.getModifier(modId);
        if (old != null) inst.removeModifier(modId);

        double amount = multiplier - 1.0;
        if (amount == 0.0) return;

        inst.addTemporaryModifier(new EntityAttributeModifier(
                modId,
                amount,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    public SizeMode getSizeMode(UUID uuid){
        return modeByPlayer.getOrDefault(uuid, SizeMode.NORMAL);
    }

    public void throwPassengers(ServerPlayerEntity host) {
        if (!host.isSneaking()) return;
        if (getSizeMode(host.getUuid()) != SizeMode.BIG) return;
        if (host.getPassengerList().isEmpty()) return;

        Vec3d dir = host.getRotationVec(1.0f).normalize();
        Vec3d vel = dir.multiply(2.6).add(0, 0.25, 0);

        List<Entity> passengers = List.copyOf(host.getPassengerList());

        for (Entity p : passengers) {
            p.stopRiding();

            p.refreshPositionAfterTeleport(
                    host.getX() + dir.x * 0.7,
                    host.getY() + 2.0,
                    host.getZ() + dir.z * 0.7
            );

            p.setVelocity(vel);
            p.velocityDirty = true;
            p.fallDistance = 0;

            if (p instanceof ServerPlayerEntity sp) {
                sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
            } else if (host.getEntityWorld() instanceof ServerWorld sw) {
                sw.getChunkManager().sendToNearbyPlayers(p, new EntityVelocityUpdateS2CPacket(p));
            }
        }
    }

    private void reset(ServerPlayerEntity player) {
        modeByPlayer.remove(player.getUuid());
        applyMultiplier(player, EntityAttributes.SCALE, SCALE_MOD_ID, 1.0);
        applyMultiplier(player, EntityAttributes.FALL_DAMAGE_MULTIPLIER, FALL_MOD_ID, 1.0);
        applyMultiplier(player, EntityAttributes.ENTITY_INTERACTION_RANGE, REACH_MOD_ENTITY_ID, 1.0);
        applyMultiplier(player, EntityAttributes.BLOCK_INTERACTION_RANGE, REACH_MOD_BLOCK_ID, 1.0);
        applyMultiplier(player, EntityAttributes.ATTACK_DAMAGE, DAMAGE_MOD_ID, 1.0);
        applyMultiplier(player, EntityAttributes.MOVEMENT_SPEED, SPEED_MOD_ID, 1.0);
        applyMultiplier(player, EntityAttributes.MAX_HEALTH, HEALTH_MOD_ID, 1.0);
        applyMultiplier(player, EntityAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD_ID, 1.0);
        applyMultiplier(player, EntityAttributes.JUMP_STRENGTH, JUMP_MOD_ID, 1.0);
    }
}