package net.blitzcube.mlapi.util.packet.handler;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Created by iso2013 on 8/23/2017.
 */
public class PacketGoal {
    private int priority;
    private JavaPlugin parent;
    private Object handler;
    private Method method;

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
            method.invoke(handler, packetEvent);
        } catch (Exception e) {
            parent.getLogger().severe("Failed to execute listener. Error provided:");
            e.printStackTrace();
        }
    }
}
