package net.blitzcube.mlapi.util.packet.handler;

import java.lang.reflect.Method;

import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by iso2013 on 8/23/2017.
 */
public class PacketGoal {

    private final JavaPlugin parent;
    private final Object handler;
    private final Method method;
    private final int priority;

    public PacketGoal(JavaPlugin parent, Object handler, Method method, PacketHandler.Priority priority) {
        this.parent = parent;
        this.handler = handler;
        this.method = method;
        this.priority = priority.getNumeric();
    }

    public int getPriority() {
        return priority;
    }

    public void invoke(PacketEvent packetEvent) {
        try {
            this.method.invoke(handler, packetEvent);
        } catch (Exception e) {
            this.parent.getLogger().severe("Failed to execute listener. Error provided:");
            e.printStackTrace();
        }
    }

}
