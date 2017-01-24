package com.nametagedit.plugin.api;

import com.google.common.collect.Maps;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.TagController;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Created by iso2013 on 1/13/17.
 */
public final class NametagAPI implements TagController {
    private Map<UUID,Data> playerNames;

    public NametagAPI(){
        this.playerNames = Maps.newHashMap();
    }

    public void clearNametag(Player player) {
        if(playerNames.containsKey(player.getUniqueId())){
            playerNames.remove(player.getUniqueId());
        }
        MultiLineAPI.getName(player).setText(player.getName());
    }

    public void reloadNametag(Player player) {
        Data d = playerNames.get(player.getUniqueId());
        if(d != null) {
            MultiLineAPI.getName(player).setText((d.prefix != null ? d.prefix : "") + player.getName() + (d.suffix != null ? d.suffix : ""));
        }
        MultiLineAPI.refresh(player);
    }

    public void clearNametag(String player) {
        Player bPlayer;
        if((bPlayer = Bukkit.getPlayer(player)) != null) {
            if(playerNames.containsKey(bPlayer.getUniqueId())){
                playerNames.remove(bPlayer.getUniqueId());
            }
            MultiLineAPI.getName(bPlayer).setText(bPlayer.getName());
        }
    }

    public void setPrefix(Player player, String prefix) {
        if(!playerNames.containsKey(player.getUniqueId())) playerNames.put(player.getUniqueId(), new Data());
        Data d = playerNames.get(player.getUniqueId());
        d.prefix = prefix;
        reloadNametag(player);
    }

    public void setSuffix(Player player, String suffix) {
        if(!playerNames.containsKey(player.getUniqueId())) playerNames.put(player.getUniqueId(), new Data());
        Data d = playerNames.get(player.getUniqueId());
        d.prefix = suffix;
        reloadNametag(player);
    }

    public void setPrefix(String sPlayer, String prefix) {
        Player player;
        if((player = Bukkit.getPlayer(sPlayer)) != null) {
            if(!playerNames.containsKey(player.getUniqueId())) playerNames.put(player.getUniqueId(), new Data());
            Data d = playerNames.get(player.getUniqueId());
            d.prefix = prefix;
            reloadNametag(player);
        }
    }

    public void setSuffix(String sPlayer, String suffix) {
        Player player;
        if((player = Bukkit.getPlayer(sPlayer)) != null) {
            if(!playerNames.containsKey(player.getUniqueId())) playerNames.put(player.getUniqueId(), new Data());
            Data d = playerNames.get(player.getUniqueId());
            d.suffix = suffix;
            reloadNametag(player);
        }
    }

    public void setNametag(Player player, String prefix, String suffix) {
        if(!playerNames.containsKey(player.getUniqueId())) playerNames.put(player.getUniqueId(), new Data());
        Data d = playerNames.get(player.getUniqueId());
        d.prefix = prefix;
        d.suffix = suffix;
        reloadNametag(player);
    }

    public void setNametag(String sPlayer, String prefix, String suffix) {
        Player player;
        if((player = Bukkit.getPlayer(sPlayer)) != null) {
            if(!playerNames.containsKey(player.getUniqueId())) playerNames.put(player.getUniqueId(), new Data());
            Data d = playerNames.get(player.getUniqueId());
            d.prefix = prefix;
            d.suffix = suffix;
            reloadNametag(player);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private class Data {
        private String prefix;
        private String suffix;
    }
}
