package com.hhoa.kline.core.core.shared;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AudioProgramConstants {
    public static final AudioProgramConfig AUDIO_PROGRAM_CONFIG =
            AudioProgramConfig.builder()
                    .darwin(createDarwinConfig())
                    .linux(createLinuxConfig())
                    .win32(createWin32Config())
                    .build();

    private static PlatformConfig createDarwinConfig() {
        return PlatformConfig.builder()
                .command("ffmpeg")
                .fallbackPaths(Arrays.asList("/usr/local/bin/ffmpeg", "/opt/homebrew/bin/ffmpeg"))
                .getArgs(
                        outputFile ->
                                Arrays.asList(
                                        "-f",
                                        "avfoundation",
                                        "-i",
                                        ":default",
                                        "-c:a",
                                        "libopus",
                                        "-b:a",
                                        "32k",
                                        "-application",
                                        "voip",
                                        "-ar",
                                        "16000",
                                        "-ac",
                                        "1",
                                        outputFile))
                .dependencyName("FFmpeg")
                .installCommand("brew install ffmpeg")
                .error(
                        "FFmpeg is required for voice recording but is not installed on your system.")
                .installDescription(
                        "FFmpeg is a multimedia framework that Cline uses to record audio from your microphone.")
                .build();
    }

    private static PlatformConfig createLinuxConfig() {
        return PlatformConfig.builder()
                .command("ffmpeg")
                .fallbackPaths(
                        Arrays.asList(
                                "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/snap/bin/ffmpeg"))
                .getArgs(
                        outputFile ->
                                Arrays.asList(
                                        "-f",
                                        "alsa",
                                        "-i",
                                        "default",
                                        "-c:a",
                                        "libopus",
                                        "-b:a",
                                        "32k",
                                        "-application",
                                        "voip",
                                        "-ar",
                                        "16000",
                                        "-ac",
                                        "1",
                                        outputFile))
                .dependencyName("FFmpeg")
                .installCommand("sudo apt-get update && sudo apt-get install -y ffmpeg")
                .error(
                        "FFmpeg is required for voice recording but is not installed on your system.")
                .installDescription(
                        "FFmpeg is a multimedia framework that Cline uses to record audio from your microphone.")
                .build();
    }

    private static PlatformConfig createWin32Config() {
        return PlatformConfig.builder()
                .command("ffmpeg")
                .fallbackPaths(
                        Arrays.asList(
                                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                                "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
                                "C:\\Program Files (x86)\\ffmpeg\\bin\\ffmpeg.exe"))
                .getArgs(
                        outputFile ->
                                Arrays.asList(
                                        "-f",
                                        "wasapi",
                                        "-i",
                                        "audio=default",
                                        "-c:a",
                                        "libopus",
                                        "-b:a",
                                        "32k",
                                        "-application",
                                        "voip",
                                        "-ar",
                                        "16000",
                                        "-ac",
                                        "1",
                                        outputFile))
                .dependencyName("FFmpeg")
                .installCommand("winget install Gyan.FFmpeg")
                .error(
                        "FFmpeg is required for voice recording but is not installed on your system.")
                .installDescription(
                        "FFmpeg is a multimedia framework that Cline uses to record audio from your microphone.")
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioProgramConfig {
        private PlatformConfig darwin;
        private PlatformConfig linux;
        private PlatformConfig win32;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformConfig {
        private String command;
        private List<String> fallbackPaths;

        private Function<String, List<String>> getArgs;

        private String dependencyName;
        private String installCommand;
        private String error;
        private String installDescription;
    }
}
