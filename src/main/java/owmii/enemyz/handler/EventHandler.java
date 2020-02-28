package owmii.enemyz.handler;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.vertex.VertexBuilderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import owmii.enemyz.Enemyz;
import owmii.enemyz.network.packet.CSyncTarget;
import owmii.lib.client.util.Render;
import owmii.lib.client.util.RenderTypes;
import owmii.lib.util.Empty;

import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber
public class EventHandler {
    public static final String TAG_PLAYER_UUID = "PlayerTargetId";
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    public static boolean visible = true;

    @SubscribeEvent
    public static void setTarget(LivingEvent.LivingUpdateEvent event) {
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
                        CSyncTarget.send(mobEntity.getEntityId(), player.getUniqueID(), player);
                    }
                } else if (nbt.hasUniqueId(TAG_PLAYER_UUID)) {
                    PlayerEntity player = world.getPlayerByUuid(nbt.getUniqueId(TAG_PLAYER_UUID));
                    if (player instanceof ServerPlayerEntity) {
                        nbt.putUniqueId(TAG_PLAYER_UUID, EMPTY_UUID);
                        CSyncTarget.send(mobEntity.getEntityId(), EMPTY_UUID, player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void renderIcon(RenderLivingEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || !visible) return;
        LivingEntity entity = event.getEntity();
        UUID uuid = entity.getPersistentData().getUniqueId(TAG_PLAYER_UUID);
        if (player.getUniqueID().equals(uuid) && entity.isAlive()) {
            MatrixStack matrix = event.getMatrixStack();
            matrix.push();


            matrix.translate(0.0D, 0.7D + entity.getHeight(), 0.0D);
            matrix.rotate(mc.getRenderManager().getCameraOrientation());
            matrix.rotate(Vector3f.YP.rotationDegrees(180.0F));
            matrix.scale(0.8F, 0.8F, 0.07F);
            ItemStack itemstack = new ItemStack(Enemyz.ICON);


            IRenderTypeBuffer.Impl rtb = mc.getRenderTypeBuffers().getBufferSource();
            IVertexBuilder buffer = rtb.getBuffer(RenderTypes.getTextBlended(Empty.LOCATION));

            matrix.translate(-0.5D, -0.5D, -0.5D);

            RenderType rendertype = RenderTypeLookup.getRenderType(itemstack);

            IVertexBuilder ivertexbuilder = getBuffer(event.getBuffers(), RenderTypes.entityBlended(AtlasTexture.LOCATION_BLOCKS_TEXTURE), true, itemstack.hasEffect());
            ItemRenderer ir = mc.getItemRenderer();
            renderModel(ir.getItemModelMesher().getItemModel(itemstack), itemstack, Render.MAX_LIGHT, OverlayTexture.NO_OVERLAY, matrix, ivertexbuilder);
            matrix.pop();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static IVertexBuilder getBuffer(IRenderTypeBuffer bufferIn, RenderType renderTypeIn, boolean isItemIn, boolean glintIn) {
        return glintIn ? VertexBuilderUtils.newDelegate(bufferIn.getBuffer(isItemIn ? RenderType.getGlint() : RenderType.getEntityGlint()), bufferIn.getBuffer(renderTypeIn)) : bufferIn.getBuffer(renderTypeIn);
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderModel(IBakedModel modelIn, ItemStack stack, int combinedLightIn, int combinedOverlayIn, MatrixStack matrixStackIn, IVertexBuilder bufferIn) {
        Random random = new Random();
        long i = 42L;
        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            ir.renderQuads(matrixStackIn, bufferIn, modelIn.getQuads(null, direction, random), stack, combinedLightIn, combinedOverlayIn);
        }
        random.setSeed(42L);
        ir.renderQuads(matrixStackIn, bufferIn, modelIn.getQuads(null, null, random), stack, combinedLightIn, combinedOverlayIn);
    }
}
