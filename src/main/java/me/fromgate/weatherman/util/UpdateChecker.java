/*
 *  WeatherMan, Minecraft bukkit plugin
 *  ©2012-2018, fromgate, fromgate@gmail.com
 *  https://www.spigotmc.org/resources/weatherman.43379/
 *
 *  This file is part of WeatherMan.
 *
 *  WeatherMan is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  WeatherMan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with WeatherMan.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.fromgate.weatherman.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UpdateChecker {
    private static JavaPlugin pluginInstance;
    private static boolean enableUpdateChecker;
    private static String projectId;
    private static String projectName;
    private static String projectApiUrl;
    private static String informPermission; // кого оповещать об обнволениях
    private static String projectBukkitDev;
    private static String projectCurrentVersion;
    private static String projectLastVersion;
    private static List<String> updateMessages;

    /**
     * UpdateChecker initialization
     *
     * @param plugin        - plugin
     * @param pluginName    - plugin name (for example: FaceChat)
     * @param projectID     - CurseAPI project id
     * @param bukkitDevName - bukkit-dev name
     * @param enable        - enable (true) update checker
     *                      Example:
     *                      UpdateChecker.init(this, "FaceChat", "12345", "facechat", true);
     */
    public static void init(JavaPlugin plugin, String pluginName, String projectID, String bukkitDevName, boolean enable) {
        pluginInstance = plugin;
        projectId = projectID;
        projectApiUrl = "https://api.curseforge.com/servermods/files?projectIds=" + projectId;
        projectName = pluginName;
        projectCurrentVersion = pluginInstance.getDescription().getVersion();
        enableUpdateChecker = enable && (!projectId.isEmpty());
        projectBukkitDev = "http://dev.bukkit.org/projects/" + bukkitDevName + "/";
        informPermission = bukkitDevName + ".config";
        projectLastVersion = "";
        setUpdateMessage(new ArrayList<>());
        if (enableUpdateChecker) {
            updateMsg();
            Bukkit.getScheduler().runTaskTimerAsynchronously(pluginInstance, (Runnable) UpdateChecker::updateMsg, (40 + (new Random()).nextInt(20) * 1200), 60 * 1200);
        }
    }

    /**
     * Change permission for informing players (player with this permission will see update message)
     * Default permission (if this method was not used) is: <BukkitDevName>.config (for example: facechat.config)
     *
     * @param permission Example:
     *                   UpdateChecker.setPermission ("facechat.updateinform");
     */
    public static void setPermission(String permission) {
        informPermission = permission;
    }

    /**
     * Setup update message (could contains few lines)
     * Supported placeholders:
     * %plugin% %newversion% %oldversion% %url%
     *
     * @param list - String list containing message
     *             if list is empty will filled with default messages
     *             <p>
     *             Example:
     *             List<String> list = new ArrayList<String>();
     *             list.add("&6%plugin% &eis outdated! Recommended version is &6v%newversion%");
     *             list.add("&ePlease download new version from BukkitDev:");
     *             list.add("&b%url%");
     *             UpdateChecker.setUpdateMessage(list);
     */
    public static void setUpdateMessage(List<String> list) {
        if (list == null || list.isEmpty()) {
            updateMessages = new ArrayList<>();
            updateMessages.add("&6%plugin% &eis outdated! Recommended version is &6v%newversion%");
            updateMessages.add("&ePlease download new version from BukkitDev:");
            updateMessages.add("&b%url%");
        } else updateMessages = list;
    }

    /**
     * Setup update message (could contains few lines)
     * Supported placeholders:
     * %plugin% %newversion% %oldversion% %url%
     *
     * @param str - String list containing message
     *            <p>
     *            Example:
     *            UpdateChecker.setUpdateMessage ("&6%plugin% &eis outdated! Recommended version is &6v%newversion%",
     *            "&ePlease download new version from BukkitDev:","&b%url%");
     */
    public static void setUpdateMessage(String... str) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, str);
        setUpdateMessage(list);
    }


    /**
     * Show update message for player. You must call this method inside PlayerJoinEvent handler
     *
     * @param player Example:
     * @EventHandler public void onJoin (PlayerJoinEvent event){
     * UpdateChecker.updateMsg(event.getPlayer());
     * }
     */
    public static void updateMsg(Player player) {
        if (isUpdateRequired() && player.hasPermission(informPermission)) {
            for (String message : updateMessages)
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(message)));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void updateMsg() {
        pluginInstance.getServer().getScheduler().runTaskAsynchronously(pluginInstance, () -> {
            updateLastVersion();
            if (isUpdateRequired()) {
                log(projectName + " v" + projectCurrentVersion + " is outdated! Recommended version is v" + projectLastVersion);
                log(projectBukkitDev);
            }
        });
    }


    private static void updateLastVersion() {
        if (!enableUpdateChecker) return;
        URL url = null;
        try {
            url = new URL(projectApiUrl);
        } catch (Exception e) {
            log("Failed to create URL: " + projectApiUrl);
            return;
        }
        try {
            URLConnection conn = url.openConnection();
            conn.addRequestProperty("X-API-Key", null);
            conn.addRequestProperty("User-Agent", projectName + " using UpdateChecker (by fromgate)");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();
            JSONArray array = (JSONArray) JSONValue.parse(response);
            if (array.size() > 0) {
                JSONObject latest = (JSONObject) array.get(array.size() - 1);
                String plugin_name = (String) latest.get("name");
                projectLastVersion = plugin_name.replace(projectName + " v", "").trim();
            }
        } catch (Exception e) {
            log("Failed to check last version");
        }
    }

    private static boolean isUpdateRequired() {
        if (!enableUpdateChecker) return false;
        if (projectId.isEmpty()) return false;
        if (projectLastVersion.isEmpty()) return false;
        if (projectCurrentVersion.isEmpty()) return false;
        if (projectCurrentVersion.equalsIgnoreCase(projectLastVersion)) return false;
        double current_version = parseVersion(projectCurrentVersion);//Double.parseDouble(projectCurrentVersion.replaceFirst("\\.", "").replace("/", ""));
        double last_version = parseVersion(projectLastVersion); //Double.parseDouble(projectLastVersion.replaceFirst("\\.", "").replace("/", ""));
        return (last_version > current_version);
    }

    /*
     * ReActions v0.1.0/10 -> 000100.010
     * ReActions v1.2.3/4  -> 010102.004
     **/
    private static double parseVersion(String versionStr) {
        String a = versionStr;
        String b = "000";
        if (versionStr.contains("/")) {
            a = versionStr.substring(0, versionStr.indexOf("/"));
            b = versionStr.substring(versionStr.indexOf("/") + 1);
        }
        String[] ln = a.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String s : ln) {
            sb.append(s.matches("\\d+") ? String.format("%02d", Integer.parseInt(s)) : "00");
        }
        sb.append(".");
        sb.append(b.matches("\\d+") ? String.format("%03d", Integer.parseInt(b)) : "000");
        return Double.parseDouble(sb.toString());

    }

    private static void log(String message) {
        pluginInstance.getLogger().info(message);
    }

    private static String replacePlaceholders(String message) {
        return message.replace("%plugin%", projectName).replace("%newversion%", projectLastVersion).replace("%oldversion%", projectLastVersion).replace("%url%", projectBukkitDev);
    }
}
