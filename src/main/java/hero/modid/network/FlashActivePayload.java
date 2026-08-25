package hero.modid.network;

import hero.modid.HeroMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FlashActivePayload(UUID playerUuid) implements CustomPayload {
    public static final Id<FlashActivePayload> ID =
            new Id<>(Identifier.of(HeroMod.MOD_ID, "flash_active"));

    public static final PacketCodec<RegistryByteBuf, FlashActivePayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> buf.writeUuid(payload.playerUuid()),
                    buf -> new FlashActivePayload(buf.readUuid())
            );

    @Override
    public Id<? extends CustomPayload>getId() {return ID;}
}
