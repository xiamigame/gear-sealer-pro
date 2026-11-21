package com.gearsealerpro.network;

import com.gearsealerpro.logic.SealerLogic;
import com.gearsealerpro.menu.GearSealerProMenu;
import iskallia.vault.gear.attribute.VaultGearModifier;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client -> server: seal the selected modifier onto the selected affix (or add a new affix). */
public class MsgCommitSeal {

    private final ResourceLocation optionId;
    private final byte affixType;
    private final int targetIndex;

    public MsgCommitSeal(ResourceLocation optionId, VaultGearModifier.AffixType affixType, int targetIndex) {
        this.optionId = optionId;
        this.affixType = (byte) affixType.ordinal();
        this.targetIndex = targetIndex;
    }

    private MsgCommitSeal(ResourceLocation optionId, byte affixType, int targetIndex) {
        this.optionId = optionId;
        this.affixType = affixType;
        this.targetIndex = targetIndex;
    }

    public static void encode(MsgCommitSeal msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.optionId);
        buf.writeByte(msg.affixType);
        buf.writeVarInt(msg.targetIndex + 1);
    }

    public static MsgCommitSeal decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        byte type = buf.readByte();
        int target = buf.readVarInt() - 1;
        return new MsgCommitSeal(id, type, target);
    }

    public static void handle(MsgCommitSeal msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof GearSealerProMenu menu)) {
                return;
            }
            VaultGearModifier.AffixType[] types = VaultGearModifier.AffixType.values();
            if (msg.affixType < 0 || msg.affixType >= types.length) {
                return;
            }
            SealerLogic.seal(menu, player, msg.optionId, types[msg.affixType], msg.targetIndex);
            menu.sendOptions(player);
            menu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
