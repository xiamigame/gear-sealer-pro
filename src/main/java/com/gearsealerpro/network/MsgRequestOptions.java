package com.gearsealerpro.network;

import com.gearsealerpro.menu.GearSealerProMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client asks the server to recompute the selectable sealed modifiers for the current gear. */
public class MsgRequestOptions {

    public MsgRequestOptions() {
    }

    public static void encode(MsgRequestOptions msg, FriendlyByteBuf buf) {
    }

    public static MsgRequestOptions decode(FriendlyByteBuf buf) {
        return new MsgRequestOptions();
    }

    public static void handle(MsgRequestOptions msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof GearSealerProMenu menu) {
                menu.sendOptions(player);
            }
        });
        context.setPacketHandled(true);
    }
}
