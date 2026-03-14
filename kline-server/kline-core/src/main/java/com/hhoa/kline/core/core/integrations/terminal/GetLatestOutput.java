package com.hhoa.kline.core.core.integrations.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

/** 获取最新终端输出工具类 注意：在 Java 环境中，我们无法直接访问剪贴板或 VSCode 命令 这里提供一个基于文件或进程输出的实现 */
@Slf4j
public class GetLatestOutput {
    /** 获取最新终端输出 在 Java 环境中，这可能需要通过其他方式实现，比如读取进程输出或文件 */
    public static String getLatestTerminalOutput() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = new ProcessBuilder();
            if (os.contains("win")) {
                pb.command("cmd.exe", "/c", "echo %cd%");
            } else {
                pb.command("sh", "-c", "pwd");
            }
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            log.error("Failed to get latest terminal output", e);
            return "";
        }
    }

    /** 尝试从临时文件读取终端输出（如果存在） */
    public static String getLatestTerminalOutputFromFile(Path outputFile) {
        try {
            if (Files.exists(outputFile)) {
                return Files.readString(outputFile);
            }
        } catch (IOException e) {
            log.error("Failed to read terminal output from file", e);
        }
        return "";
    }
}
