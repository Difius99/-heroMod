package hero.modid.network;

import hero.modid.HeroMod;
import hero.modid.hero.HeroType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record HeroTypeUpdatePayload(UUID playerUuid, HeroType heroType) implements CustomPayload {
    public static final Id<HeroTypeUpdatePayload> ID =
            new Id<>(Identifier.of(HeroMod.MOD_ID, "hero_type_update"));

    public static final PacketCodec<RegistryByteBuf, HeroTypeUpdatePayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> {
                        buf.writeUuid(payload.playerUuid());
                        buf.writeString(payload.heroType().name());
                    },
                     buf -> new HeroTypeUpdatePayload(
                             buf.readUuid(),
                             HeroType.valueOf(buf.readString())
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId(){
        return ID;
    }
}
