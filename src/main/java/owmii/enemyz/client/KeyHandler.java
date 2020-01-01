package owmii.enemyz.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import owmii.enemyz.Enemyz;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class KeyHandler {
    public static final KeyBinding KEY_TOGGLE = new KeyBinding("enemyz.keybind.toggle", 293, "key.categories.enemyz");

    public static void register() {
        ClientRegistry.registerKeyBinding(KEY_TOGGLE);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START) {
            PlayerEntity player = event.player;
            if (KEY_TOGGLE.isPressed()) {
                Enemyz.Handler.visible = !Enemyz.Handler.visible;
            }
        }
    }
}
