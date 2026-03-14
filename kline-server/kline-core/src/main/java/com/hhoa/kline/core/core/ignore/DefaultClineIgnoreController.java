package com.hhoa.kline.core.core.ignore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认的 ClineIgnore 控制器实现
 *
 * <p>支持 .gitignore 语法的模式匹配。
 *
 * @author hhoa
 */
@Slf4j
public class DefaultClineIgnoreController implements ClineIgnoreController {

    private final String cwd;
    private final List<Pattern> ignorePatterns;

    public DefaultClineIgnoreController(String cwd) {
        this.cwd = cwd;
        this.ignorePatterns = new ArrayList<>();
        loadClineIgnore();
    }

    /** 加载 .clineignore 文件并解析模式 */
    private void loadClineIgnore() {
        try {
            Path ignoreFile = Paths.get(cwd, ".clineignore");
            if (Files.exists(ignoreFile) && Files.isRegularFile(ignoreFile)) {
                List<String> lines = Files.readAllLines(ignoreFile);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    Pattern pattern = convertGitIgnorePatternToRegex(line);
                    if (pattern != null) {
                        ignorePatterns.add(pattern);
                    }
                }
                log.debug("Loaded {} ignore patterns from .clineignore", ignorePatterns.size());
            } else {
                addDefaultPatterns();
            }
        } catch (IOException e) {
            log.warn("Failed to load .clineignore file: {}", e.getMessage());
            addDefaultPatterns();
        }

        ignorePatterns.add(Pattern.compile("^\\.clineignore$"));
    }

    private void addDefaultPatterns() {
        String[] defaultPatterns = {
            "^\\.git/",
            "^\\.git$",
            "^node_modules/",
            "^target/",
            "^build/",
            "^\\.env$",
            "^\\.env\\..*",
            ".*\\.secret$",
            ".*\\.key$",
        };

        for (String patternStr : defaultPatterns) {
            try {
                ignorePatterns.add(Pattern.compile(patternStr));
            } catch (Exception e) {
                log.warn("Invalid default ignore pattern: {}", patternStr);
            }
        }
    }

    /**
     * 将 gitignore 模式转换为正则表达式
     *
     * <p>简化实现，支持基本的 gitignore 语法： - 通配符 * 和 ? - 目录匹配 / - 否定模式 !
     *
     * @param gitIgnorePattern gitignore 模式
     * @return 正则表达式 Pattern，如果模式无效则返回 null
     */
    private Pattern convertGitIgnorePatternToRegex(String gitIgnorePattern) {
        if (gitIgnorePattern == null || gitIgnorePattern.isEmpty()) {
            return null;
        }

        boolean isNegation = gitIgnorePattern.startsWith("!");
        if (isNegation) {
            gitIgnorePattern = gitIgnorePattern.substring(1);
        }

        StringBuilder regex = new StringBuilder();

        if (gitIgnorePattern.startsWith("/")) {
            regex.append("^");
            gitIgnorePattern = gitIgnorePattern.substring(1);
        } else {
            regex.append(".*");
        }

        for (int i = 0; i < gitIgnorePattern.length(); i++) {
            char c = gitIgnorePattern.charAt(i);
            switch (c) {
                case '*':
                    if (i + 1 < gitIgnorePattern.length()
                            && gitIgnorePattern.charAt(i + 1) == '*') {
                        regex.append(".*");
                        i++;
                    } else {
                        regex.append("[^/]*");
                    }
                    break;
                case '?':
                    regex.append("[^/]");
                    break;
                case '.':
                case '+':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '$':
                case '|':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                case '/':
                    if (i == gitIgnorePattern.length() - 1) {
                        regex.append("/.*");
                    } else {
                        regex.append("/");
                    }
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }

        if (gitIgnorePattern.endsWith("/")) {
            regex.append(".*");
        }

        regex.append("$");

        try {
            return Pattern.compile(regex.toString());
        } catch (Exception e) {
            log.warn("Failed to compile pattern: {}", gitIgnorePattern);
            return null;
        }
    }

    @Override
    public boolean validateAccess(String relPath) {
        if (relPath == null || relPath.isEmpty()) {
            return true;
        }

        String normalizedPath = relPath.replace("\\", "/");

        for (Pattern pattern : ignorePatterns) {
            if (pattern.matcher(normalizedPath).matches()) {
                log.debug("Path '{}' is blocked by pattern: {}", normalizedPath, pattern.pattern());
                return false;
            }
        }

        return true;
    }

    @Override
    public String validateCommand(String command) {
        boolean hasIgnorePatterns = false;
        for (Pattern pattern : ignorePatterns) {
            if (!pattern.pattern().equals("^\\.clineignore$")) {
                hasIgnorePatterns = true;
                break;
            }
        }
        if (!hasIgnorePatterns) {
            return null;
        }

        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) {
            return null;
        }

        String baseCommand = parts[0].toLowerCase();

        String[] fileReadingCommands = {
            "cat",
            "less",
            "more",
            "head",
            "tail",
            "grep",
            "awk",
            "sed",
            "get-content",
            "gc",
            "type",
            "select-string",
            "sls"
        };

        boolean isFileReadingCommand = false;
        for (String cmd : fileReadingCommands) {
            if (cmd.equals(baseCommand)) {
                isFileReadingCommand = true;
                break;
            }
        }

        if (isFileReadingCommand) {
            for (int i = 1; i < parts.length; i++) {
                String arg = parts[i];
                if (arg.startsWith("-") || arg.startsWith("/")) {
                    continue;
                }
                if (arg.contains(":")) {
                    continue;
                }
                String filePath = arg.replace("\"", "").replace("'", "");
                if (!validateAccess(filePath)) {
                    return filePath;
                }
            }
        }

        return null;
    }

    public void reload() {
        ignorePatterns.clear();
        loadClineIgnore();
    }
}
