package xieao.enemyz;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

@Mod(modid = Enemyz.MOD_ID, name = "Enemyz", version = Enemyz.VERSION)
public class Enemyz {
    public static final String MOD_ID = "enemyz";
    public static final String VERSION = "0.1.3";

    public static final String TAG_PLAYER_UUID = "PlayerTargetId";
    private static final SimpleNetworkWrapper NET = new SimpleNetworkWrapper(MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.init(event);
        NET.registerMessage(SyncTarget.class, SyncTarget.class, 0, Side.CLIENT);
    }

    @Mod.EventBusSubscriber
    public static class Config {
        private static Configuration config;
        public static float iconSize;
        public static float yOffset;
        public static boolean sneaking;


        public static void init(FMLPreInitializationEvent event) {
            File configFile = new File(event.getModConfigurationDirectory(), MOD_ID + ".cfg");
            config = new Configuration(configFile);
            config.load();
            setConfig();
        }

        private static void setConfig() {
            iconSize = (float) config.get("enemyz", "icon_size", 1.0D, "Change the icon size.", 0.0D, 1.0D).getDouble();
            yOffset = (float) config.get("enemyz", "icon_y_offset", 0.0D, "Move the icon up and down.", -1.0D, 1.0D).getDouble();
            sneaking = config.get("enemyz", "sneaking", true, "Show icon through blocks while sneaking.").getBoolean();
            if (config.hasChanged())
                config.save();
        }

        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(MOD_ID)) {
                setConfig();
            }
        }
    }

    public static class SyncTarget implements IMessage, IMessageHandler<SyncTarget, SyncTarget> {
        private int entityId;
        private UUID uuid;


        public SyncTarget(int entityId, UUID uuid) {
            this.entityId = entityId;
            this.uuid = uuid;
        }

        public SyncTarget() {
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.entityId = buf.readInt();
            this.uuid = new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(this.entityId);
            buf.writeLong(this.uuid.getMostSignificantBits());
            buf.writeLong(this.uuid.getLeastSignificantBits());
        }

        @Override
        public SyncTarget onMessage(SyncTarget message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Entity entity = Minecraft.getMinecraft().world.getEntityByID(message.entityId);
                if (entity instanceof EntityLiving) {
                    entity.getEntityData().setUniqueId(TAG_PLAYER_UUID, message.uuid);
                }
            });
            return null;
        }
    }

    @Mod.EventBusSubscriber
    public static class Handler {
        private static final UUID EMPTY_UUID = new UUID(0L, 0L);

        @SubscribeEvent
        public static void setAndSyncTarget(LivingEvent.LivingUpdateEvent event) {
            EntityLivingBase entity = event.getEntityLiving();
            World world = entity.world;
            if (!world.isRemote) {
                if (entity instanceof EntityLiving) {
                    EntityLiving mobEntity = (EntityLiving) entity;
                    NBTTagCompound nbt = mobEntity.getEntityData();
                    EntityLivingBase target = mobEntity.getAttackTarget();
                    if (target instanceof EntityPlayerMP) {
                        EntityPlayerMP player = (EntityPlayerMP) target;
                        if (!player.getUniqueID().equals(nbt.getUniqueId(TAG_PLAYER_UUID))) {
                            nbt.setUniqueId(TAG_PLAYER_UUID, player.getUniqueID());
                            NET.sendTo(new SyncTarget(mobEntity.getEntityId(), player.getUniqueID()), player);
                        }
                    } else {
                        if (nbt.hasUniqueId(TAG_PLAYER_UUID)) {
                            EntityPlayer player = world.getPlayerEntityByUUID(Objects.requireNonNull(nbt.getUniqueId(TAG_PLAYER_UUID)));
                            if (player instanceof EntityPlayerMP) {
                                nbt.setUniqueId(TAG_PLAYER_UUID, EMPTY_UUID);
                                NET.sendTo(new SyncTarget(mobEntity.getEntityId(), EMPTY_UUID), (EntityPlayerMP) player);
                            }
                        }
                    }
                }
            }
        }

        public static final ResourceLocation LOCATION = new ResourceLocation(MOD_ID, "textures/misc/skull.png");

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void renderIcon(RenderLivingEvent.Post event) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            EntityLivingBase entity = event.getEntity();
            UUID uuid = entity.getEntityData().getUniqueId(TAG_PLAYER_UUID);
            if (player.getUniqueID().equals(uuid) && !entity.isDead) {
                GlStateManager.pushMatrix();
                Minecraft.getMinecraft().getTextureManager().bindTexture(LOCATION);
                float neatOffset = Loader.isModLoaded("neat") ? 0.35F : 0.0F;
                GlStateManager.translate(event.getX(), entity.height + 0.5D + event.getY() + Config.yOffset + neatOffset, event.getZ());
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableBlend();
                GlStateManager.depthMask(false);
                boolean isSneaking = player.isSneaking();
                if (Config.sneaking && isSneaking) {
                    GlStateManager.disableDepth();
                }
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 15728880 >> 16 & '\uffff', 15728880 & '\uffff');
                GlStateManager.scale(0.5F * Config.iconSize, 0.5F * Config.iconSize, 0.5F * Config.iconSize);
                GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                GlStateManager.rotate(180.0F - event.getRenderer().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
                bufferbuilder.pos(-0.5D, -0.25D, 0.0D).tex(0.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(0.5D, -0.25D, 0.0D).tex(1.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(0.5D, 0.75D, 0.0D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                bufferbuilder.pos(-0.5D, 0.75D, 0.0D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                tessellator.draw();
                if (Config.sneaking && isSneaking) {
                    GlStateManager.enableDepth();
                }
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.disableRescaleNormal();
                GlStateManager.popMatrix();
            }
        }
    }
}
