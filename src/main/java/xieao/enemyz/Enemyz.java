package xieao.enemyz;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(Enemyz.MOD_ID)
public class Enemyz {
    public static final String MOD_ID = "enemyz";
    public static final SimpleChannel NET;

    public Enemyz() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CONFIG_SPEC);
    }

    static {
        NET = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MOD_ID, "main"))
                .clientAcceptedVersions("1"::equals)
                .serverAcceptedVersions("1"::equals)
                .networkProtocolVersion(() -> "1")
                .simpleChannel();
    }

    void commonSetup(FMLCommonSetupEvent event) {
        NET.registerMessage(0, SyncTarget.class, SyncTarget::encode, SyncTarget::decode, SyncTarget::handle);
        ClientRegistry.registerKeyBinding(Handler.KEY_TOGGLE);
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Objects {
        //Dummy way to render skull icon on 3d
        public static final Item ICON = new Item(new Item.Properties());

        @SubscribeEvent
        public static void onRegistry(RegistryEvent.Register<Item> event) {
            event.getRegistry().register(ICON.setRegistryName("icon"));
        }
    }
}
