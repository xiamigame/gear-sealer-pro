package com.gearsealerpro.client;

import com.gearsealerpro.network.MsgSyncOptions;

/** Latest option snapshot pushed by the server for the currently open Gear Sealer Pro. */
public final class ClientSealData {

    private static MsgSyncOptions current = new MsgSyncOptions();

    public static MsgSyncOptions get() {
        return current;
    }

    public static void update(MsgSyncOptions msg) {
        current = msg;
    }

    private ClientSealData() {
    }
}
