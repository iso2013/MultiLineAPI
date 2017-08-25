package net.blitzcube.mlapi.util.packet.handler;

import com.comphenix.protocol.PacketType;

import java.lang.annotation.*;

/**
 * Created by iso2013 on 8/23/2017.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketHandler {
    PacketType.Protocol protocol();

    PacketType.Sender sender();

    String name();

    Priority priority() default Priority.NORMAL;

    enum Priority {
        LOWEST(-2),
        LOW(-1),
        NORMAL(0),
        HIGH(1),
        HIGHEST(2),
        MONITOR(3);

        private int numeric;

        Priority(int i) {
            this.numeric = i;
        }

        public int getNumeric() {
            return numeric;
        }
    }
}
