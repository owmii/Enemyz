package xieao.enemyz;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;
import java.util.function.Supplier;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;

@Mod(Enemyz.MOD_ID)
public class Enemyz {
    public static final String MOD_ID = "enemyz";
    public static final String TAG_PLAYER_UUID = "PlayerTargetId";
    public static final ForgeConfigSpec CONFIG_SPEC;
    public static final Config CONFIG;
    public static final SimpleChannel NET;

    public Enemyz() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC);
        NET.registerMessage(0, SyncTarget.class, SyncTarget::encode, SyncTarget::decode, SyncTarget::handle);
    }

    static {
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        CONFIG_SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
        NET = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MOD_ID, "main"))
                .clientAcceptedVersions("1"::equals)
                .serverAcceptedVersions("1"::equals)
                .networkProtocolVersion(() -> "1")
                .simpleChannel();
    }

    public static class Config {
        public final ForgeConfigSpec.DoubleValue iconSize;
        public final ForgeConfigSpec.DoubleValue yOffset;

        public Config(ForgeConfigSpec.Builder builder) {
            this.iconSize = builder.comment("Change the icon size.")
                    .defineInRange("icon_size", 1.0D, 0.0D, 1.0D);
            this.yOffset = builder.comment("Move the icon up and down.")
                    .defineInRange("icon_y_offset", 0.0D, -1.0D, 1.0D);
        }
    }

    public static class SyncTarget {
        private int entityId;
        private UUID uuid;

        public SyncTarget(int entityId, UUID uuid) {
            this.entityId = entityId;
            this.uuid = uuid;
        }

        public static void encode(SyncTarget msg, PacketBuffer buffer) {
            buffer.writeInt(msg.entityId);
            buffer.writeUniqueId(msg.uuid);
        }

        public static SyncTarget decode(PacketBuffer buffer) {
            return new SyncTarget(buffer.readInt(), buffer.readUniqueId());
        }

        public static void handle(SyncTarget msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Entity entity = Minecraft.getInstance().world.getEntityByID(msg.entityId);
                if (entity instanceof MobEntity) {
                    entity.getEntityData().putUniqueId(TAG_PLAYER_UUID, msg.uuid);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber
    public static class Handler {
        private static final UUID EMPTY_UUID = new UUID(0L, 0L);

        @SubscribeEvent
        public static void setAndSyncTarget(LivingEvent.LivingUpdateEvent event) {
            LivingEntity entity = event.getEntityLiving();
            World world = entity.world;
            if (!world.isRemote) {
                if (entity instanceof MobEntity) {
                    MobEntity mobEntity = (MobEntity) entity;
                    CompoundNBT nbt = mobEntity.getEntityData();
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

        public static final ResourceLocation LOCATION = new ResourceLocation(MOD_ID, "textures/misc/skull.png");

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void renderIcon(RenderLivingEvent.Post event) {
            PlayerEntity player = Minecraft.getInstance().player;
            LivingEntity entity = event.getEntity();
            UUID uuid = entity.getEntityData().getUniqueId(TAG_PLAYER_UUID);
            if (player.getUniqueID().equals(uuid) && entity.isAlive()) {
                GlStateManager.pushMatrix();
                Minecraft.getInstance().getTextureManager().bindTexture(LOCATION);
                GlStateManager.translated(event.getX(), entity.getHeight() + 0.5D + event.getY() + CONFIG.yOffset.get(), event.getZ());
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableBlend();
                GlStateManager.depthMask(false);
                boolean isSneaking = player.isSneaking();
                if (isSneaking) {
                    GlStateManager.disableDepthTest();
                }
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 15728880 >> 16 & '\uffff', 15728880 & '\uffff');
                GlStateManager.scalef(0.5F, 0.5F, 0.5F);
                GlStateManager.color4f(1.0F, 0.0F, 0.0F, 1.0F);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                GlStateManager.rotatef(180.0F - event.getRenderer().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
                bufferbuilder.pos(-0.5D, -0.25D, 0.0D).tex(0.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(0.5D, -0.25D, 0.0D).tex(1.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(0.5D, 0.75D, 0.0D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(-0.5D, 0.75D, 0.0D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                tessellator.draw();
                if (isSneaking) {
                    GlStateManager.enableDepthTest();
                }
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.disableRescaleNormal();
                GlStateManager.popMatrix();
            }
        }
    }
}
