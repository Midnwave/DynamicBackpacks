package com.blockforge.dynamicbackpacks;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Properties;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final String API_URL =
            "https://api.github.com/repos/Midnwave/DynamicBackpacks/releases/latest";
    private static final String TAG_PREFIX = "dev-";

    private final DynamicBackpacks plugin;
    private final int currentBuild;

    public UpdateChecker(DynamicBackpacks plugin) {
        this.plugin = plugin;
        this.currentBuild = loadBuildNumber();
    }

    public int getCurrentBuild() { return currentBuild; }

    private int loadBuildNumber() {
        try (InputStream is = getClass().getResourceAsStream("/build.properties")) {
            if (is == null) return 0;
            Properties props = new Properties();
            props.load(is);
            return Integer.parseInt(props.getProperty("build.number", "0").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // runs async; callback is posted back to main thread with latest build number, or null on failure
    public void checkAsync(Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "DynamicBackpacks-UpdateChecker");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }

                String tag = extractTagName(sb.toString());
                if (tag == null || !tag.startsWith(TAG_PREFIX)) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                    return;
                }

                int latestBuild = Integer.parseInt(tag.substring(TAG_PREFIX.length()));
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(latestBuild));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    private String extractTagName(String json) {
        String key = "\"tag_name\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
