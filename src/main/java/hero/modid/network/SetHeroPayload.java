package hero.modid.network;

import hero.modid.HeroMod;
import hero.modid.hero.HeroType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;


public record SetHeroPayload(HeroType heroType) implements CustomPayload {

    public static final Id<SetHeroPayload> ID =
            new Id<>(Identifier.of(HeroMod.MOD_ID, "set_hero"));

    public static final PacketCodec<RegistryByteBuf, SetHeroPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, payload) -> buf.writeString(payload.heroType().name()),
                    buf -> new SetHeroPayload(HeroType.valueOf(buf.readString()))
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
