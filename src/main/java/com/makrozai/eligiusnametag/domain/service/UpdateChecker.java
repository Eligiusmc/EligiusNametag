package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.StartupLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/Eligiusmc/EligiusNametag/releases/latest";
    private static final String FALLBACK_DOWNLOAD_URL = "https://github.com/Eligiusmc/EligiusNametag/releases/latest";
    
    private static String latestVersion = null;
    private static String downloadUrl = FALLBACK_DOWNLOAD_URL;
    private static boolean updateAvailable = false;

    public static void fetch(String currentVersion) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "EligiusNametag-UpdateChecker")
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (Exception e) {
                // Silently fail on network issues
            }
            return null;
        }).thenAccept(json -> {
            if (json != null && !json.isEmpty()) {
                parseResponse(json, currentVersion);
            }
        });
    }

    private static void parseResponse(String json, String currentVersion) {
        // Very lightweight parsing to avoid gson dependency if not necessary
        String tagPrefix = "\"tag_name\":";
        int tagIndex = json.indexOf(tagPrefix);
        if (tagIndex != -1) {
            int start = json.indexOf("\"", tagIndex + tagPrefix.length()) + 1;
            int end = json.indexOf("\"", start);
            if (start > 0 && end > start) {
                String fetchedVersion = json.substring(start, end);
                latestVersion = fetchedVersion.replace("v", "");
                String cleanCurrent = currentVersion.replace("v", "");

                if (!cleanCurrent.equalsIgnoreCase(latestVersion)) {
                    updateAvailable = true;
                    // Try to get html_url
                    String urlPrefix = "\"html_url\":";
                    int urlIndex = json.indexOf(urlPrefix);
                    if (urlIndex != -1) {
                        int urlStart = json.indexOf("\"", urlIndex + urlPrefix.length()) + 1;
                        int urlEnd = json.indexOf("\"", urlStart);
                        if (urlStart > 0 && urlEnd > urlStart) {
                            downloadUrl = json.substring(urlStart, urlEnd);
                        }
                    }
                    StartupLogger.printUpdateNotice(latestVersion, downloadUrl);
                } else {
                    StartupLogger.printUpToDate(currentVersion);
                }
            }
        }
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getLatestVersion() {
        return latestVersion != null ? latestVersion : "Unknown";
    }

    public static String getDownloadUrl() {
        return downloadUrl;
    }
}
