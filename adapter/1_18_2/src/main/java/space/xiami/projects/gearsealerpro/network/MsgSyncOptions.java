package space.xiami.projects.gearsealerpro.network;

import space.xiami.projects.gearsealerpro.client.ClientSealData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: the currently selectable sealed modifiers, replacement targets and costs. */
public class MsgSyncOptions {

    public record OptionEntry(ResourceLocation id, byte type, Component display) {
    }

    public record TargetEntry(byte type, Component display) {
    }

    public boolean hasGear;
    public boolean modifiable;
    public boolean canSeal;
    public int costGold;
    public int costPainite;
    public int costFocus;
    public int haveGold;
    public int havePainite;
    public int haveFocus;
    public final List<OptionEntry> options = new ArrayList<>();
    public final List<TargetEntry> targets = new ArrayList<>();

    public MsgSyncOptions() {
    }

    public static void encode(MsgSyncOptions msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasGear);
        buf.writeBoolean(msg.modifiable);
        buf.writeBoolean(msg.canSeal);
        buf.writeVarInt(msg.costGold);
        buf.writeVarInt(msg.costPainite);
        buf.writeVarInt(msg.costFocus);
        buf.writeVarInt(msg.haveGold);
        buf.writeVarInt(msg.havePainite);
        buf.writeVarInt(msg.haveFocus);

        buf.writeVarInt(msg.options.size());
        for (OptionEntry entry : msg.options) {
            buf.writeResourceLocation(entry.id());
            buf.writeByte(entry.type());
            buf.writeUtf(Component.Serializer.toJson(entry.display()));
        }

        buf.writeVarInt(msg.targets.size());
        for (TargetEntry entry : msg.targets) {
            buf.writeByte(entry.type());
            buf.writeUtf(Component.Serializer.toJson(entry.display()));
        }
    }

    public static MsgSyncOptions decode(FriendlyByteBuf buf) {
        MsgSyncOptions msg = new MsgSyncOptions();
        msg.hasGear = buf.readBoolean();
        msg.modifiable = buf.readBoolean();
        msg.canSeal = buf.readBoolean();
        msg.costGold = buf.readVarInt();
        msg.costPainite = buf.readVarInt();
        msg.costFocus = buf.readVarInt();
        msg.haveGold = buf.readVarInt();
        msg.havePainite = buf.readVarInt();
        msg.haveFocus = buf.readVarInt();

        int optionCount = buf.readVarInt();
        for (int i = 0; i < optionCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            byte type = buf.readByte();
            Component display = Component.Serializer.fromJson(buf.readUtf());
            msg.options.add(new OptionEntry(id, type, display));
        }

        int targetCount = buf.readVarInt();
        for (int i = 0; i < targetCount; i++) {
            byte type = buf.readByte();
            Component display = Component.Serializer.fromJson(buf.readUtf());
            msg.targets.add(new TargetEntry(type, display));
        }
        return msg;
    }

    public static void handle(MsgSyncOptions msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSealData.update(msg)));
        context.setPacketHandled(true);
    }
}
