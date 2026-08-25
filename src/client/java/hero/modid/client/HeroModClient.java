package hero.modid.client;

import hero.modid.HeroMod;
import hero.modid.HeroModConfig;
import hero.modid.client.render.FlashAfterImage;
import hero.modid.client.state.FlashActiveClientState;
import hero.modid.hero.HeroType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import network.HeroNetworkingClient;
import org.lwjgl.glfw.GLFW;


public final class HeroModClient implements ClientModInitializer {

	private static final KeyBinding.Category HERO_MOD_CATEGORY =
			KeyBinding.Category.create(Identifier.of("hero-mod", "main"));

	public static KeyBinding OPEN_MENU_KEY;
	public static KeyBinding USE_POWER_KEY;
	private static boolean attackWasDown = false;

	@Override
	public void onInitializeClient() {
		HeroMod.CONFIG = HeroModConfig.load();

		OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.hero-mod.open-menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				HERO_MOD_CATEGORY
		));
		USE_POWER_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.hero-mod.use-power",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_P,
				HERO_MOD_CATEGORY
		));


		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU_KEY.wasPressed()) {
				MinecraftClient mc = MinecraftClient.getInstance();

				if (mc.player == null || mc.world == null) return;

				mc.setScreen(new HeroModScreen(mc.currentScreen));
			}
			while (USE_POWER_KEY.wasPressed()) {
				if (client.player == null || client.world == null) return;
				if (client.currentScreen != null) return;

				float seconds = (float)FlashActiveClientState.getCooldown(client.player.getUuid()) / 1000.0f;
				if (seconds > 0) {
					client.player.sendMessage(Text.literal("Cooldown: " + seconds + "s"), true);
				} else {
					HeroNetworkingClient.sendUsePower();
				}
			}
			boolean attackDown = client.options.attackKey.isPressed();
			boolean justPressed = attackDown && !attackWasDown;
			attackWasDown = attackDown;

			if (client.player != null && client.world != null && client.currentScreen == null) {
				if (justPressed && client.player.isSneaking() &&
						HeroMod.CONFIG.selectedMode == HeroType.ANT_MAN
				) {
					HeroNetworkingClient.sendThrowPassengers();
				}
			}
		});

		ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
			if (HeroMod.CONFIG == null) return;
			HeroNetworkingClient.sendSelectedHero(HeroMod.CONFIG.selectedMode);
		}));

		HeroNetworkingClient.initClient();
		FlashAfterImage.init();
	}


}