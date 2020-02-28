package owmii.enemyz.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import owmii.enemyz.handler.EventHandler;
import owmii.lib.Lollipop;
import owmii.lib.network.IPacket;

import java.util.UUID;
import java.util.function.Supplier;

public class CSyncTarget implements IPacket<CSyncTarget> {
    private int entityId;
    private UUID uuid;

    public CSyncTarget(int entityId, UUID uuid) {
        this.entityId = entityId;
        this.uuid = uuid;
    }

    public CSyncTarget() {
        this(0, new UUID(0L, 0L));
    }

    public static void send(int entityId, UUID uuid, PlayerEntity player) {
        Lollipop.NET.toClient(new CSyncTarget(entityId, uuid), player);
    }

    public void encode(CSyncTarget msg, PacketBuffer buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeUniqueId(msg.uuid);
    }

    public CSyncTarget decode(PacketBuffer buffer) {
        return new CSyncTarget(buffer.readInt(), buffer.readUniqueId());
    }

    public void handle(CSyncTarget msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            World world = Minecraft.getInstance().world;
            if (world != null) {
                Entity entity = world.getEntityByID(msg.entityId);
                if (entity instanceof MobEntity) {
                    entity.getPersistentData().putUniqueId(EventHandler.TAG_PLAYER_UUID, msg.uuid);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}