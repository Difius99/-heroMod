package hero.modid;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hero.modid.hero.HeroType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


public class HeroModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("hero-mod.json");

    public HeroType selectedMode = HeroType.DEFAULT;

    public static HeroModConfig load() {
        try {
            if (Files.notExists(PATH)) return new HeroModConfig();
            return GSON.fromJson(Files.readString(PATH), HeroModConfig.class);
        } catch (Exception e) {
            return new HeroModConfig();
        }
    }

    public void save() {
        try {
            Files.writeString(
                    PATH,
                    GSON.toJson(this),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {

        }
    }
}
