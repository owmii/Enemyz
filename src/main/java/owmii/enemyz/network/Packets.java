package owmii.enemyz.network;

import owmii.enemyz.network.packet.CSyncTarget;
import owmii.lib.Lollipop;

public class Packets {
    public static void register() {
        Lollipop.NET.register(new CSyncTarget());
    }
}
