package hero.modid.hero.power;

import hero.modid.HeroMod;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlashPower implements HeroPower {

    private static final int COOLDOWN_TICKS = 20 * 4;

    private static final int BOOST_TICKS = 40;
    private static final Identifier WATER_EFF_ID =
            Identifier.of(HeroMod.MOD_ID, "flash_water_eff");

    private static final double WATER_EFF_ADD = 1.2;

    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, Long> waterBoostUntil = new HashMap<>();

    @Override
    public void serverTick(ServerPlayerEntity player) {
        long now = player.getEntityWorld().getTime();
        UUID uuid = player.getUuid();

        Long until = waterBoostUntil.get(uuid);
        if (until != null && now >= until) {
            removeWaterEfficiency(player);
            waterBoostUntil.remove(uuid);
        }
    }

    @Override
    public boolean use(ServerPlayerEntity player) {
        long now = player.getEntityWorld().getTime();
        UUID uuid = player.getUuid();

        long until = cooldownUntil.getOrDefault(uuid, 0L);
        if (now < until) {
            float seconds = (until - now) / 20.0f;
            player.sendMessage(Text.literal("Cooldown: " + seconds + " sec"), true);
            return false;
        }

        player.fallDistance = 0;

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                BOOST_TICKS,
                70,
                true, false, false
        ));

        addWaterEfficiency(player);
        waterBoostUntil.put(uuid, now + BOOST_TICKS);

        cooldownUntil.put(uuid, now + COOLDOWN_TICKS);
        return true;
    }

    @Override
    public void onDisconnect(ServerPlayerEntity player) {
        cooldownUntil.remove(player.getUuid());
        waterBoostUntil.remove(player.getUuid());
        removeWaterEfficiency(player);
    }


    private static void addWaterEfficiency(ServerPlayerEntity player) {
        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (inst == null) return;

        var old = inst.getModifier(WATER_EFF_ID);
        if (old != null) inst.removeModifier(WATER_EFF_ID);

        inst.addTemporaryModifier(new EntityAttributeModifier(
                WATER_EFF_ID,
                WATER_EFF_ADD,
                EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void removeWaterEfficiency(ServerPlayerEntity player) {
        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (inst == null) return;

        var old = inst.getModifier(WATER_EFF_ID);
        if (old != null) inst.removeModifier(WATER_EFF_ID);
    }
}