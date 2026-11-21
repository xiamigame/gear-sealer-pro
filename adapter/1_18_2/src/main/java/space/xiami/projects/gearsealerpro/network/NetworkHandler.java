package space.xiami.projects.gearsealerpro.network;

import space.xiami.projects.gearsealerpro.GearSealerProMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "2";
    private static SimpleChannel channel;
    private static int packetId;

    public static void init() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(GearSealerProMod.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

        channel.registerMessage(packetId++, MsgRequestOptions.class,
                MsgRequestOptions::encode, MsgRequestOptions::decode, MsgRequestOptions::handle);
        channel.registerMessage(packetId++, MsgSyncOptions.class,
                MsgSyncOptions::encode, MsgSyncOptions::decode, MsgSyncOptions::handle);
        channel.registerMessage(packetId++, MsgCommitSeal.class,
                MsgCommitSeal::encode, MsgCommitSeal::decode, MsgCommitSeal::handle);
    }

    public static void sendToServer(Object message) {
        channel.sendToServer(message);
    }

    public static void sendTo(ServerPlayer player, Object message) {
        channel.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private NetworkHandler() {
    }
}
