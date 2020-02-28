package owmii.enemyz;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import owmii.enemyz.network.Packets;
import owmii.lib.util.FML;

import static owmii.lib.Lollipop.addModListener;

@Mod(Enemyz.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Enemyz {
    public static final String MOD_ID = "enemyz";
    public static Item ICON = new Item(new Item.Properties().maxStackSize(1));

    public Enemyz() {
        addModListener(this::commonSetup);
        addModListener(this::clientSetup);
    }

    void commonSetup(final FMLCommonSetupEvent event) {
        Packets.register();
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (FML.isClient()) {

        }
    }

    @SubscribeEvent
    public static void registerIcon(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ICON.setRegistryName("icon"));
    }
}
