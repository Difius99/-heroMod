package hero.modid.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPassengerPosMixin {

    @Shadow
    protected abstract Vec3d getPassengerRidingPos(Entity passenger);

    @Inject(
            method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hero$updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof PlayerEntity host)) return;

        int idx = host.getPassengerList().indexOf(passenger);
        if (idx < 0 || idx > 1) return;

        double hostScale = host.getAttributeValue(EntityAttributes.SCALE);
        double passengerScale = (passenger instanceof LivingEntity le)
                ? le.getAttributeValue(EntityAttributes.SCALE)
                : 1.0;

        boolean hostIsBig = hostScale > 1.2;
        boolean passengerIsSmall = passengerScale < 0.5;

        if (!hostIsBig && !passengerIsSmall) return;

        Vec3d base = this.getPassengerRidingPos(passenger);

        float bodyYawDeg = (host instanceof LivingEntity le) ? le.bodyYaw : host.getYaw();
        float yawRad = -bodyYawDeg * MathHelper.RADIANS_PER_DEGREE;

        double side;
        double forward;
        double yExtra;

        if (hostIsBig) {
            side = (idx == 0) ? 1.95 : -1.95;
            forward = 0.10;
            yExtra = -2.0;
        } else {
            side = (idx == 0) ? 0.47 : -0.47;
            forward = 0.10;
            yExtra = -0.42;
        }


        Vec3d add = new Vec3d(side, 0.0, forward).rotateY(yawRad);
        Vec3d pos = base.add(add);

        positionUpdater.accept(passenger, pos.x, pos.y + yExtra, pos.z);
        ci.cancel();
    }
}