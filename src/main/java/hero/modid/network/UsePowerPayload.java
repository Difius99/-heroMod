package hero.modid.network;

import hero.modid.HeroMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UsePowerPayload() implements CustomPayload {
    public static final Id<UsePowerPayload> ID =
            new Id<>(Identifier.of(HeroMod.MOD_ID, "use_power"));

    public static final PacketCodec<RegistryByteBuf, UsePowerPayload> CODEC =
            PacketCodec.ofStatic(
                    ((buf, payload) -> {}),
                    buf -> new UsePowerPayload()
            );

    @Override
    public Id<? extends CustomPayload>getId(){
        return ID;
    }
 }
