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
                    String remoteVersion = version.replace("Version ", "").replace(".", "");
                    String pluginVersion = this.plugin.getDescription().getVersion().replace("-SNAPSHOT", "").replace(".", "");
                    if (Integer.valueOf(remoteVersion) > Integer.valueOf(pluginVersion)) {
                        this.plugin.getLogger().log(Level.INFO, "There is a new version avaiable on SpigotMC Resources!");
                    } else {
                        this.plugin.getLogger().log(Level.INFO, "No updates found.");
                    }
                }
            } catch (MalformedURLException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Something went wrong with the URL!");
            } catch (IOException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Could not establish a connection with SpigotMC!");
            } catch (ParseException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Established a connection but something went wrong with the payload!");
            }
            try {
                Thread.sleep(this.plugin.getConfig().getInt("updateTime", 1440)*1000);
            } catch (InterruptedException ex) {
                if (stopThread) {
                return;
                }
            }
        }
    }

    public boolean setStop(boolean stop) {
       return this.stopThread = stop;
    }
}
