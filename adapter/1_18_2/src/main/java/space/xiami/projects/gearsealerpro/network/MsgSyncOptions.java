package space.xiami.projects.gearsealerpro.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import space.xiami.projects.gearsealerpro.client.ClientSealData;

/**
 * Server -> client: which sealed modifiers are available (id + allowed affix sides), which
 * affixes can be replaced, and the material state.
 *
 * <p>No display text is sent: The Vault's display helpers are client-only, so the client
 * builds the labels itself from the same gear stack it already has. This keeps the dedicated
 * server free of client classes.</p>
 */
public class MsgSyncOptions {

    /** Affix sides a sealed modifier can occupy. */
    public static final byte SIDE_PREFIX = 1;
    public static final byte SIDE_SUFFIX = 2;

    /** @param sides bit mask of {@link #SIDE_PREFIX} / {@link #SIDE_SUFFIX} */
    public record OptionEntry(ResourceLocation id, byte sides) {

        public boolean allows(boolean prefix) {
            return (this.sides & (prefix ? SIDE_PREFIX : SIDE_SUFFIX)) != 0;
        }
    }

    /** One replaceable affix, in prefix-then-suffix order. */
    public record TargetEntry(boolean prefix) {
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
            buf.writeByte(entry.sides());
        }

        buf.writeVarInt(msg.targets.size());
        for (TargetEntry entry : msg.targets) {
            buf.writeBoolean(entry.prefix());
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
            msg.options.add(new OptionEntry(buf.readResourceLocation(), buf.readByte()));
        }

        int targetCount = buf.readVarInt();
        for (int i = 0; i < targetCount; i++) {
            msg.targets.add(new TargetEntry(buf.readBoolean()));
        }
        return msg;
    }

    public static void handle(MsgSyncOptions msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSealData.update(msg)));
        context.setPacketHandled(true);
    }
}
