package hero.modid.network;

import hero.modid.hero.HeroManager;
import hero.modid.hero.HeroPowers;
import hero.modid.hero.HeroType;
import hero.modid.hero.power.AntManPower;
import hero.modid.hero.power.HeroPower;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;


public class HeroNetworking {

    public static void initCommon() {
        PayloadTypeRegistry.playC2S().register(SetHeroPayload.ID, SetHeroPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UsePowerPayload.ID, UsePowerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HeroTypeUpdatePayload.ID, HeroTypeUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FlashActivePayload.ID, FlashActivePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ThrowPassengersPayload.ID, ThrowPassengersPayload.CODEC);
    }

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(SetHeroPayload.ID,((payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                HeroType type = HeroManager.getHeroType(player);
                HeroType heroType = payload.heroType();
                UUID uuid = player.getUuid();
                var server = context.server();

                HeroPowers.getPower(type).onDeselected(player);

                HeroManager.setHeroType(player, heroType);

                for (var p : server.getPlayerManager().getPlayerList()){
                    ServerPlayNetworking.send(p, new HeroTypeUpdatePayload(uuid, heroType));
                }
            });
        }));

        ServerPlayNetworking.registerGlobalReceiver((UsePowerPayload.ID),((payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                HeroType type = HeroManager.getHeroType(player);

                HeroPower power = HeroPowers.getPower(type);
                boolean ok = power.use(player);

                if (!ok) return;

                if (type == HeroType.FLASH) {
                    for (var p : context.server().getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(p, new FlashActivePayload(player.getUuid()));
                    }
                }
            });
        }));

        ServerPlayNetworking.registerGlobalReceiver((ThrowPassengersPayload.ID), ((payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity host = context.player();

                HeroType type = HeroManager.getHeroType(host);
                HeroPower power = HeroPowers.getPower(type);

                if (power instanceof AntManPower antManPower) {
                    antManPower.throwPassengers(host);
                }
            });
        }));

        ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            HeroType type = HeroManager.getHeroType(player);
            HeroPower power = HeroPowers.getPower(type);

            if (power != null) {
                power.onDeselected(player);
                power.onDisconnect(player);
            }

            HeroManager.removeHeroType(player);

            for (var p : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, new HeroTypeUpdatePayload(uuid, HeroType.DEFAULT));
            }
        }));

        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
            ServerPlayerEntity joiner = handler.player;

            for (var p : server.getPlayerManager().getPlayerList()) {
                HeroType type = HeroManager.getHeroType(p);
                ServerPlayNetworking.send(joiner, new HeroTypeUpdatePayload(p.getUuid(), type));
            }
        }));
    }

    private HeroNetworking () {}
}
