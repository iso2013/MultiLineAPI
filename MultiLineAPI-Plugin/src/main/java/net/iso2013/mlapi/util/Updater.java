/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.iso2013.mlapi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Frostalf
 */
public class Updater extends Thread {

    private static final String HOST = "https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=33473";

    private static final String USER_AGENT = "Updater (by Frostalf)";
    private final Plugin plugin;
    private boolean stopThread;

    public Updater(Plugin plugin, boolean stopThread) {
        this.plugin = plugin;
        this.stopThread = stopThread;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(this.plugin.getConfig().getInt("updateTime"), 1440);
        } catch (InterruptedException ex) {
            if (stopThread) {
                return;
            }
        }
        while (!stopThread) {
            try {
                URL url = new URL(HOST);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(in);
                    String version = (String) json.get("current_version");
                    String[] remoteVersion = version.replace("version ", "").split(".");
                    String[] currentVersion = this.plugin.getDescription().getVersion().split(".");
                    StringBuilder comparedVersionRemote = new StringBuilder(3);
                    StringBuilder comparedVersionCurrent = new StringBuilder(3);

                    for (String version2 : remoteVersion) {
                        for (String version3 : currentVersion) {
                        comparedVersionRemote.append(version2);
                        comparedVersionCurrent.append(version3);
                        }
                    }

                    if (Integer.valueOf(comparedVersionRemote.toString()) > Integer.valueOf(comparedVersionCurrent.toString())) {
                        this.plugin.getLogger().log(Level.WARNING, "There is a new version avaiable on SpigotMC Resources!");
                    }
                }
            } catch (MalformedURLException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Something went wrong with the URL!");
            } catch (IOException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Could not establish a connection with SpigotMC!");
            } catch (ParseException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Established a connection but something went wrong with the payload!");
            }
        }
    }

    public boolean setStop(boolean stop) {
       return this.stopThread = stop;
    }
}
