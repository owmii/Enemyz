package owmii.enemyz;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import owmii.enemyz.client.KeyHandler;

import java.util.UUID;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;

@Mod(Enemyz.MOD_ID)
public class Enemyz {
    public static final String MOD_ID = "enemyz";
    public static final SimpleChannel NET;

    public Enemyz() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
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
    }

    void clientSetup(FMLClientSetupEvent event) {
        KeyHandler.register();
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

    @Mod.EventBusSubscriber
    public static class Handler {
        public static final String TAG_PLAYER_UUID = "PlayerTargetId";
        private static final UUID EMPTY_UUID = new UUID(0L, 0L);
        public static boolean visible = true;

        @SubscribeEvent
        public static void setAndSyncTarget(LivingEvent.LivingUpdateEvent event) {
            LivingEntity entity = event.getEntityLiving();
            if (entity instanceof MobEntity) {
                World world = entity.getEntityWorld();
                if (!world.isRemote) {
                    MobEntity mobEntity = (MobEntity) entity;
                    CompoundNBT nbt = mobEntity.getPersistentData();
                    LivingEntity target = mobEntity.getAttackTarget();
                    if (target instanceof ServerPlayerEntity) {
                        ServerPlayerEntity player = (ServerPlayerEntity) target;
                        if (!nbt.getUniqueId(TAG_PLAYER_UUID).equals(player.getUniqueID())) {
                            nbt.putUniqueId(TAG_PLAYER_UUID, player.getUniqueID());
                            NET.sendTo(new SyncTarget(mobEntity.getEntityId(), player.getUniqueID()), player.connection.getNetworkManager(), PLAY_TO_CLIENT);
                        }
                    } else {
                        if (nbt.hasUniqueId(TAG_PLAYER_UUID)) {
                            PlayerEntity player = world.getPlayerByUuid(nbt.getUniqueId(TAG_PLAYER_UUID));
                            if (player instanceof ServerPlayerEntity) {
                                nbt.putUniqueId(TAG_PLAYER_UUID, EMPTY_UUID);
                                NET.sendTo(new SyncTarget(mobEntity.getEntityId(), EMPTY_UUID), ((ServerPlayerEntity) player).connection.getNetworkManager(), PLAY_TO_CLIENT);
                            }
                        }
                    }
                }
            }
        }


        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void renderIcon(RenderLivingEvent.Post event) {
            if (!visible) return;
            PlayerEntity player = Minecraft.getInstance().player;
            LivingEntity entity = event.getEntity();
            UUID uuid = entity.getPersistentData().getUniqueId(TAG_PLAYER_UUID);
            if (player.getUniqueID().equals(uuid) && entity.isAlive()) {
                if (player.getDistance(entity) > Config.CONFIG.distance.get()) return;
                GlStateManager.pushMatrix();
                float neatOffset = ModList.get().isLoaded("neat") ? 0.45F : 0.0F;
                GlStateManager.translated(event.getX(), entity.getHeight() + 0.5D + event.getY() + Config.CONFIG.yOffset.get() + neatOffset, event.getZ());
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 15728880 >> 16 & '\uffff', 15728880 & '\uffff');
                GlStateManager.scalef((float) (0.5F * Config.CONFIG.iconSize.get()), (float) (0.5F * Config.CONFIG.iconSize.get()), (float) (0.5F * Config.CONFIG.iconSize.get()));
                GlStateManager.color4f(1.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotatef(180.0F - event.getRenderer().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
                final ItemStack stack = new ItemStack(Objects.ICON);
                ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
                textureManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
                IBakedModel bakedmodel = ForgeHooksClient.handleCameraTransforms(itemRenderer.getModelWithOverrides(stack), ItemCameraTransforms.TransformType.FIXED, false);
                GlStateManager.disableLighting();
                GlStateManager.scalef(1.0F, 1.0F, 0.3F);
                itemRenderer.renderItem(stack, bakedmodel);
                GlStateManager.enableLighting();
                textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
                textureManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }
    }
}
