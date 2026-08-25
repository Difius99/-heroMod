package hero.modid.network;

import hero.modid.HeroMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ThrowPassengersPayload() implements CustomPayload {
    public static final Id<ThrowPassengersPayload> ID =
            new Id<>(Identifier.of(HeroMod.MOD_ID, "throw_passengers"));

    public static final PacketCodec<RegistryByteBuf, ThrowPassengersPayload> CODEC =
            PacketCodec.ofStatic(
                    ((buf, payload) -> {}),
                    buf -> new ThrowPassengersPayload()
            );

    @Override
    public Id<? extends CustomPayload>getId(){
        return ID;
    }
}
