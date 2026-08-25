package hero.modid.mixin;

import hero.modid.hero.HeroManager;
import hero.modid.hero.HeroPowers;
import hero.modid.hero.HeroType;
import hero.modid.hero.power.AntManPower;
import hero.modid.hero.power.HeroPower;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow @Nullable private Entity vehicle;

    @Unique @Nullable private Entity hero$prevVehicle;

    private static boolean hero$allowRide(Entity passenger, ServerPlayerEntity host, AntManPower antManPower) {
        boolean hostIsBigAntMan =
                HeroManager.getHeroType(host) == HeroType.ANT_MAN &&
                        antManPower.getSizeMode(host.getUuid()) == AntManPower.SizeMode.BIG;

        if (hostIsBigAntMan) return true;

        if (passenger instanceof ServerPlayerEntity rider) {
            return HeroManager.getHeroType(rider) == HeroType.ANT_MAN &&
                    antManPower.getSizeMode(rider.getUuid()) == AntManPower.SizeMode.SMALL;
        }

        return false;
    }

    @Inject(
            method = "startRiding(Lnet/minecraft/entity/Entity;ZZ)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hero$startRiding(Entity newVehicle, boolean force, boolean thirdArg, CallbackInfoReturnable<Boolean> cir) {
        Entity passenger = (Entity) (Object) this;

        if (!(newVehicle instanceof ServerPlayerEntity host)) return;

        HeroPower power = HeroPowers.getPower(HeroType.ANT_MAN);
        if (!(power instanceof AntManPower antManPower)) return;

        if (!hero$allowRide(passenger, host, antManPower)) return;

        if (passenger == host) { cir.setReturnValue(false); return; }

        if (host.getPassengerList().size() >= 2) {
            cir.setReturnValue(false);
            return;
        }

        if (passenger.hasVehicle()) passenger.stopRiding();

        this.vehicle = host;
        ((EntityInvoker) (Object) host).hero$addPassenger(passenger);

        if (host.getEntityWorld() instanceof ServerWorld sw) {
            var pkt = new EntityPassengersSetS2CPacket(host);
            sw.getChunkManager().sendToNearbyPlayers(host, pkt);
            host.networkHandler.sendPacket(pkt);
            if (passenger instanceof ServerPlayerEntity rider) {
                rider.networkHandler.sendPacket(pkt);
            }
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "stopRiding()V", at = @At("HEAD"), require = 0)
    private void hero$storePrevVehicle(CallbackInfo ci) {
        this.hero$prevVehicle = this.vehicle;
    }

    @Inject(method = "stopRiding()V", at = @At("TAIL"), require = 0)
    private void hero$syncDismount(CallbackInfo ci) {
        Entity prev = this.hero$prevVehicle;
        this.hero$prevVehicle = null;

        if (!(prev instanceof ServerPlayerEntity host)) return;

        if (host.getEntityWorld() instanceof ServerWorld sw) {
            var pkt = new EntityPassengersSetS2CPacket(host);
            sw.getChunkManager().sendToNearbyPlayers(host, pkt);
            host.networkHandler.sendPacket(pkt);
            if (((Object) this) instanceof ServerPlayerEntity rider) {
                rider.networkHandler.sendPacket(pkt);
            }
        }
    }
}