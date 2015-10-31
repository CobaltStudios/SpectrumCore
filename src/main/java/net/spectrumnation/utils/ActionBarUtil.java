package net.spectrumnation.utils;


import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * Created by sirtidez on 10/31/15.
 */
public class ActionBarUtil {

    public static void sendMessage(String msg, Player player) {
        PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + msg + "\"}"), (byte) 2);

        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
    }

    public static void sendMessage(String msg, Player[] players) {
        PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + msg + "\"}"), (byte) 2);

        for(Player player : players) {
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
        }
    }
}
