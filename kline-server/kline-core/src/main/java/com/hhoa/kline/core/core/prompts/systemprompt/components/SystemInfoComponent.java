package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 系统信息组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class SystemInfoComponent implements SystemPromptComponent {

    private static final String SYSTEM_INFO_TEMPLATE_TEXT =
            """
        SYSTEM INFORMATION

        Operating System: {{os}}
        IDE: {{ide}}
        Default Shell: {{shell}}
        Home Directory: {{homeDir}}
        {{WORKSPACE_TITLE}}: {{workingDir}}""";

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        String template = SYSTEM_INFO_TEMPLATE_TEXT;

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.SYSTEM_INFO)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.SYSTEM_INFO);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        String os = getOperatingSystem();
        String shell = getDefaultShell();
        String homeDir = System.getProperty("user.home");
        String cwd = context.getCwd() != null ? context.getCwd() : System.getProperty("user.dir");

        String workspaceTitle;
        String workingDirInfo;

        if (context.getWorkspaceRoots() != null) {
            workspaceTitle = "Workspace Roots";
            StringBuilder rootsInfo = new StringBuilder();
            for (var root : context.getWorkspaceRoots()) {
                String vcsInfo = root.getVcs() != null ? " (" + root.getVcs() + ")" : "";
                rootsInfo
                        .append("\n  - ")
                        .append(root.getName())
                        .append(": ")
                        .append(root.getPath())
                        .append(vcsInfo);
            }
            workingDirInfo = rootsInfo + "\n\nPrimary Working Directory: " + cwd;
        } else {
            workspaceTitle = "Current Working Directory";
            workingDirInfo = cwd;
        }

        return templateEngine.resolve(
                template,
                context,
                Map.of(
                        "os", os,
                        "shell", shell,
                        "homeDir", homeDir,
                        "WORKSPACE_TITLE", workspaceTitle,
                        "workingDir", workingDirInfo));
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.SYSTEM_INFO;
    }

    private String getOperatingSystem() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        return osName + " " + osVersion;
    }

    private String getDefaultShell() {
        String shell = System.getenv("SHELL");
        if (shell != null) {
            return shell;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "cmd.exe";
        } else if (osName.contains("mac")) {
            return "/bin/zsh";
        } else {
            return "/bin/bash";
        }
    }
}
