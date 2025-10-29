package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.service.DownloadHelper.Site;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WSDownloadService {

  private static final Logger log = LoggerFactory.getLogger(WSDownloadService.class);

  private final PathProperties paths;

  public ConcurrentHashMap<String, String> hmap = new ConcurrentHashMap<>();

  // Constants
  private static final SortedSet<Integer> videoQuality = new TreeSet<>(
      Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
  );

  public enum MediaType {
    VIDEO("video"),
    VIDEO_ONLY("video_only"),
    AUDIO_ONLY("audio_only");

    private final String string;
    private static final HashMap<String, MediaType> byString = new HashMap<>();

    static {
      for (MediaType t : values()) {
        byString.put(t.string, t);
      }
    }

    private MediaType(String string) {
      this.string = string;
    }

    public String getString() {
      return string;
    }

    public static MediaType getMediaType(String str) {
      return byString.get(str);
    }
  };

  public void openWebSocketConnection(DownloadRequest request, SimpMessagingTemplate template) {
    template.convertAndSend("/topic/" + request.getId(), "Processing...");
    startDownload(request, template);
  }

  public boolean isPercentage(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9' || c == '.' || c == '%') {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  public void processLine(String l, HashMap<String, String> m, DownloadRequest r, SimpMessagingTemplate t) {
    String downloadString = "[download]";
    String temp = "";
    int flag = 0;
    if (l.startsWith(downloadString)) {
      for (int i = downloadString.length(); i < l.length(); i++) {
        char c = l.charAt(i);
        if (c == ' ') {
          if (temp.equals("")) {
            continue;
          } else {
            if (temp.equals("Destination:")) {
              flag = 1;
              temp = "";
              continue;
            }

            if (isPercentage(temp)) {
              t.convertAndSend("/topic/" + r.getId(), "Percent: " + temp);
            }


            if (flag == 1) {
              t.convertAndSend("/topic/" + r.getId(), "Url: " + temp);
              m.put(r.getMediaId(), temp);
            }

            temp = "";
          }
        } else {
          temp += c;
        }
      }

      if (!temp.equals("")) {
        if (flag == 1) {
          t.convertAndSend("/topic/" + r.getId(), "Url: " + temp);
          m.put(r.getMediaId(), temp);
        }
      }
    }
  }

  private void startDownload(DownloadRequest request, SimpMessagingTemplate template) {
    String url = request.getUrl();
    String type = request.getRequestType();
    String vidFormat = resolveVideoFormat(request.getVideoFormat());
    int vidQuality = resolveVideoQuality(request.getVideoQuality());
    String audFormat = resolveAudioFormat(request.getAudioFormat());

    if (vidQuality < 0) {
      vidQuality = 360;
    }

    Site site = parseSite(url);

    MediaType t = MediaType.getMediaType(type);
    boolean isVideo = (t == MediaType.VIDEO || t == MediaType.VIDEO_ONLY);
    boolean isVideoOnly = t == MediaType.VIDEO_ONLY;
    boolean isAudioOnly = t == MediaType.AUDIO_ONLY;

    String format = resolveCommandFormat(t, site, vidFormat, vidQuality, audFormat);
    log.info("[WSDownloadService] Command Format: " + format);

    log.info("[WSDownloadService] Got output name '" + request.getId() + "'");

    List<String> commands = new ArrayList<>();
    commands.add(paths.getYtdlpBin().toString());
    commands.addAll(Arrays.asList("-f", format));
    commands.addAll(Arrays.asList(url, "-P", paths.getDownloadPath().toString()));
    commands.add("--newline");
    commands.addAll(Arrays.asList("-o", request.getMediaId() + ".%(ext)s", "--no-warnings"));

    log.info("[WSDownloadService] Download Commands: " + String.join(" ", commands));

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(commands);
      Process process = processBuilder.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;

      HashMap<String, String> hInfo = new HashMap<String, String>();
      
      while ((line = reader.readLine()) != null) {
        processLine(line, hInfo, request, template);
        System.out.println("Helloz: " + line);
      }

      int exitCode = process.waitFor();

      template.convertAndSend("/topic/" + request.getId(), "Done");
      System.out.println("Noice! ");
      hmap.put(request.getMediaId(), hInfo.get(request.getMediaId()));

    } catch (Exception e) {

    }

  }

  public static int resolveVideoQuality(int vidQuality) {
    Iterator<Integer> iterator = videoQuality.iterator();
    int firstValue = iterator.next();

    if (vidQuality < firstValue)
      return -1;

    if (videoQuality.contains(vidQuality))
      return vidQuality;

    // Get the nearest video quality
    int prev = -1;
    for (int i : videoQuality) {
      if (i == firstValue) {
        prev = i;
        continue;
      }

      if (vidQuality > prev && vidQuality < i) {
        vidQuality = prev;
        break;
      }

      prev = i;
    }

    return vidQuality;
  }

  public static String resolveVideoFormat(String videoFormat) {
    if (videoFormat == null || videoFormat.isEmpty() || videoFormat.equals("Default")) {
      return "default";
    }

    return videoFormat;
  }

  public static String resolveAudioFormat(String audioFormat) {
    if (audioFormat == null || audioFormat.isEmpty() || audioFormat.equals("Default")) {
      return "default";
    }

    return audioFormat;
  }

  private String resolveCommandFormat(MediaType type, Site site, String videoFormat, int videoQuality,
      String audioFormat) {

    boolean isVideo = (type == MediaType.VIDEO || type == MediaType.VIDEO_ONLY);
    String formatType = "best";

    if (type == MediaType.VIDEO_ONLY) {
      formatType += "video";
    }

    if (type == MediaType.AUDIO_ONLY) {
      formatType = "bestaudio";
    }

    String format = formatType;

    if (isVideo) {
      format += String.format("[height<=%d]", videoQuality);

      if (!videoFormat.equals("default")) {
        format += String.format("[ext=%s]", videoFormat);
      }
    } else {
      if (audioFormat.equals("default")) {
        format = "bestaudio[ext=flac]/bestaudio[ext=m4a]/bestaudio[ext=mp3]/bestaudio";
      } else {
        format += String.format("[ext=%s]", audioFormat);
      }
    }

    if (isVideo && site != Site.YOUTUBE) {
      format += "/" + formatType;
    }

    return format;
  }

  private Site parseSite(String url) {
    Map<String, Site> siteMap = Map.ofEntries(
        Map.entry("youtube.com", Site.YOUTUBE),
        Map.entry("youtu.be", Site.YOUTUBE),
        Map.entry("facebook.com", Site.FACEBOOK),
        Map.entry("instagram.com", Site.INSTAGRAM));

    for (Map.Entry<String, Site> entry : siteMap.entrySet()) {
      if (url.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    return Site.UNKNOWN;
  }
}
