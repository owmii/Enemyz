package xieao.enemyz;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncTarget {
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
                entity.getEntityData().putUniqueId(Handler.TAG_PLAYER_UUID, msg.uuid);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
