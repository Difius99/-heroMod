package network;

import hero.modid.client.state.FlashActiveClientState;
import hero.modid.client.state.HeroClientState;
import hero.modid.hero.HeroType;
import hero.modid.network.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HeroNetworkingClient {
    public static void sendSelectedHero(HeroType heroType) {
        ClientPlayNetworking.send(new SetHeroPayload(heroType));
    }

    public static void sendUsePower(){
        ClientPlayNetworking.send(new UsePowerPayload());
    }

    public static void sendThrowPassengers() { ClientPlayNetworking.send((new ThrowPassengersPayload())); }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(HeroTypeUpdatePayload.ID, ((payload, context) -> {
            context.client().execute(() -> {
                HeroClientState.set(payload.playerUuid(), payload.heroType());
            });
        }));

        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
            HeroClientState.clear();
            FlashActiveClientState.clear();
        }));

        ClientPlayNetworking.registerGlobalReceiver(FlashActivePayload.ID, ((payload, context) -> {
            context.client().execute(() -> {
                FlashActiveClientState.activate(payload.playerUuid());
            });
        }));
    }

    private HeroNetworkingClient() {}
}
