package net.blitzcube.mlapi.packet;

import com.comphenix.protocol.ProtocolManager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.tag.Tag;

/**
 * Created by iso2013 on 8/5/2017.
 */
public class TagSender {

    public TagSender(ProtocolManager manager) {
        // TODO
    }

    public void spawnEntities(Tag t, Player target, Entity forWhat) {

    }

    public enum RefreshLevel {

        ENTITY,
        MOUNT,
        DATA

    }
}
