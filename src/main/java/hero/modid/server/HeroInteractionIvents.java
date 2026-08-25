package hero.modid.server;

import hero.modid.hero.HeroManager;
import hero.modid.hero.HeroPowers;
import hero.modid.hero.HeroType;
import hero.modid.hero.power.AntManPower;
import hero.modid.hero.power.HeroPower;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class HeroInteractionIvents {
    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            Entity target = entity;
            if (sp == target) return ActionResult.PASS;

            HeroPower power = HeroPowers.getPower(HeroType.ANT_MAN);
            if (!(power instanceof AntManPower antManPower)) return ActionResult.PASS;

            HeroType targetType = null;
            AntManPower.SizeMode targetMode = AntManPower.SizeMode.NORMAL;

            if (entity instanceof ServerPlayerEntity targetPlayer) {
                targetType = HeroManager.getHeroType(targetPlayer);
                targetMode = antManPower.getSizeMode(targetPlayer.getUuid());
            }

            HeroType spType = HeroManager.getHeroType(sp);
            var spMode = antManPower.getSizeMode(sp.getUuid());

            if (spType == HeroType.ANT_MAN && spMode == AntManPower.SizeMode.SMALL) {
                if (target instanceof ServerPlayerEntity targetPlayer) {
                    return sp.startRiding(targetPlayer, true, true) ? ActionResult.SUCCESS : ActionResult.PASS;
                }
            }

            if (targetType == HeroType.ANT_MAN && targetMode == AntManPower.SizeMode.BIG) {
                return sp.startRiding(target, true, true) ? ActionResult.SUCCESS : ActionResult.PASS;
            }

            if (spType == HeroType.ANT_MAN && spMode == AntManPower.SizeMode.BIG) {
                if (target instanceof LivingEntity && sp.isSneaking()) {
                    return target.startRiding(sp, true, true) ? ActionResult.SUCCESS : ActionResult.PASS;
                }
            }
            return ActionResult.PASS;
        });
    }
}