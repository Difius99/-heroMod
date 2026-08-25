package hero.modid.hero;

import hero.modid.hero.power.AntManPower;
import hero.modid.hero.power.FlashPower;
import hero.modid.hero.power.HeroPower;
import hero.modid.hero.power.SpiderPower;

import java.util.EnumMap;
import java.util.Map;

public final class HeroPowers {
    private static final Map<HeroType, HeroPower> POWERS = new EnumMap<>(HeroType.class);

    public static void init() {
        POWERS.put(HeroType.DEFAULT, new HeroPower() {});
        POWERS.put(HeroType.FLASH, new FlashPower());
        POWERS.put(HeroType.SPIDER, new SpiderPower());
        POWERS.put(HeroType.ANT_MAN, new AntManPower());
    }

    public static HeroPower getPower(HeroType heroType){
        return POWERS.getOrDefault(heroType, POWERS.get(HeroType.DEFAULT));
    }

    private HeroPowers() {}
}


