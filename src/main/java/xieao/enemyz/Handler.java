package xieao.enemyz;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;

@Mod.EventBusSubscriber
public class Handler {
    public static final KeyBinding KEY_TOGGLE = new KeyBinding("enemyz.keybind.toggle", 293, "key.categories.enemyz");
    public static final String TAG_PLAYER_UUID = "PlayerTargetId";
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static boolean visible = true;

    @SubscribeEvent
    public static void setAndSyncTarget(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity instanceof MobEntity) {
            World world = entity.getEntityWorld();
            if (!world.isRemote) {
                MobEntity mobEntity = (MobEntity) entity;
                CompoundNBT nbt = mobEntity.getEntityData();
                LivingEntity target = mobEntity.getAttackTarget();
                if (target instanceof ServerPlayerEntity) {
                    ServerPlayerEntity player = (ServerPlayerEntity) target;
                    if (!nbt.getUniqueId(TAG_PLAYER_UUID).equals(player.getUniqueID())) {
                        nbt.putUniqueId(TAG_PLAYER_UUID, player.getUniqueID());
                        Enemyz.NET.sendTo(new SyncTarget(mobEntity.getEntityId(), player.getUniqueID()), player.connection.getNetworkManager(), PLAY_TO_CLIENT);
                    }
                } else {
                    if (nbt.hasUniqueId(TAG_PLAYER_UUID)) {
                        PlayerEntity player = world.getPlayerByUuid(nbt.getUniqueId(TAG_PLAYER_UUID));
                        if (player instanceof ServerPlayerEntity) {
                            nbt.putUniqueId(TAG_PLAYER_UUID, EMPTY_UUID);
                            Enemyz.NET.sendTo(new SyncTarget(mobEntity.getEntityId(), EMPTY_UUID), ((ServerPlayerEntity) player).connection.getNetworkManager(), PLAY_TO_CLIENT);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START) {
            PlayerEntity player = event.player;
            if (KEY_TOGGLE.isPressed()) {
                visible = !visible;
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void renderIcon(RenderLivingEvent.Post event) {
        if (!visible) return;
        PlayerEntity player = Minecraft.getInstance().player;
        LivingEntity entity = event.getEntity();
        LivingEntity revengeTarget = player.getRevengeTarget();
        UUID uuid = entity.getEntityData().getUniqueId(TAG_PLAYER_UUID);
        if (player.getUniqueID().equals(uuid) && entity.isAlive()) {
            GlStateManager.pushMatrix();
            float neatOffset = ModList.get().isLoaded("neat") ? 0.35F : 0.0F;
            GlStateManager.translated(event.getX(), entity.getHeight() + 0.5D + event.getY() + Config.CONFIG.yOffset.get() + neatOffset, event.getZ());
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 15728880 >> 16 & '\uffff', 15728880 & '\uffff');
            GlStateManager.scalef((float) (0.5F * Config.CONFIG.iconSize.get()), (float) (0.5F * Config.CONFIG.iconSize.get()), (float) (0.5F * Config.CONFIG.iconSize.get()));
            GlStateManager.color4f(1.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotatef(180.0F - event.getRenderer().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            final ItemStack stack = new ItemStack(Enemyz.Objects.ICON);
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
