package hero.modid.client;

import hero.modid.HeroMod;
import hero.modid.hero.HeroType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import network.HeroNetworkingClient;

import java.util.ArrayList;
import java.util.List;


public class HeroModScreen extends Screen{
    private final Screen parent;

    public HeroModScreen(Screen parent) {
        super(Text.literal("Hero mod menu"));
        this.parent = parent;

        options.add(new OptionEntry("Default player", HeroType.DEFAULT));
        options.add(new OptionEntry("Flash", HeroType.FLASH));
        options.add(new OptionEntry("Spider", HeroType.SPIDER));
        options.add(new OptionEntry("Ant Man", HeroType.ANT_MAN));
    }

    private static class OptionEntry {
        final String text;
        boolean selected;
        ButtonWidget button;
        final HeroType heroType;

        OptionEntry(String text, HeroType heroType) {
            this.text = text;
            this.heroType = heroType;
        }
    }

    private final List<OptionEntry> options = new ArrayList<>();



    @Override
    protected void init() {
        for (int i = 0; i < options.size(); i++){
            OptionEntry option = options.get(i);
            final int x = (this.width / 2 -
                (options.size() * 50 +
                (options.size() - 1) * 10) + i * 120);
            option.button =  this.addDrawableChild(
                    ButtonWidget.builder(Text.literal(option.text), b -> select(option))
                            .dimensions(x, this.height/2 - 20, 100, 20)
                            .build()
            );
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Done"), b -> close())
                        .dimensions(this.width - 120, this.height - 40, 100, 20)
                        .build()
        );
        applySavedSelection();
    }




    @Override
    public void close() {
        if (this.client == null) return;
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (HeroModClient.OPEN_MENU_KEY != null && HeroModClient.OPEN_MENU_KEY.matchesKey(input)) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    private void applySavedSelection(){
        if (HeroMod.CONFIG == null) return;
        for(OptionEntry option : options) {
            boolean isSelected = (HeroMod.CONFIG.selectedMode == option.heroType);

            option.selected = isSelected;
            option.button.active = !isSelected;
        }
    }

    private void select(OptionEntry selected){
        if (HeroMod.CONFIG == null) return;
        for (OptionEntry option : options){
            option.selected = (option == selected);
            option.button.active = !option.selected;
        }
        HeroMod.CONFIG.selectedMode = selected.heroType;
        HeroMod.CONFIG.save();
        HeroNetworkingClient.sendSelectedHero(selected.heroType);
    }
}
