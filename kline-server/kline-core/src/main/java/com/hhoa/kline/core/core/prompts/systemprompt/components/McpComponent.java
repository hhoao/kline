package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.services.mcp.IMcpASyncClient;
import com.hhoa.kline.core.core.services.mcp.IMcpClient;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.mcp.IMcpSyncClient;
import com.hhoa.kline.core.core.services.mcp.McpServerConfig;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class McpComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpComponent(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.MCP)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.MCP);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        String mcpServersList = formatMcpServersList(context);

        return templateEngine.resolve(
                template, context, Map.of("MCP_SERVERS_LIST", mcpServersList));
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.MCP;
    }

    private String getTemplateText() {
        return """
            MCP SERVERS

            The Model Context Protocol (MCP) enables communication between the system and locally running MCP servers that provide additional tools, resources, and prompts to extend your capabilities.

            # Connected MCP Servers

            When a server is connected, you can use the server's tools via the `use_mcp_tool` tool, and access the server's resources via the `access_mcp_resource` tool.

            Servers may also provide prompts - predefined templates that can be invoked by users to generate contextual messages.

            {{MCP_SERVERS_LIST}}
            """;
    }

    private String formatMcpServersList(SystemPromptContext context) {
        if (context.getMcpHub() == null) {
            return "(No MCP servers available)";
        }
        String list = formatExternalMcpServers(context);
        return list.isEmpty() ? "(No MCP servers available)" : list;
    }

    private String formatExternalMcpServers(SystemPromptContext context) {
        IMcpHub hub = context.getMcpHub();
        if (hub == null) {
            return "";
        }
        List<IMcpHub.ServerConnectionForPrompt> connections = hub.getConnectionsForPrompt();
        if (connections.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (IMcpHub.ServerConnectionForPrompt conn : connections) {
            String section = formatServerSection(conn);
            if (!section.isEmpty()) {
                parts.add(section);
            }
        }
        return String.join("\n\n", parts);
    }

    private String formatServerSection(IMcpHub.ServerConnectionForPrompt conn) {
        StringBuilder sb = new StringBuilder();
        String serverName = conn.serverName();
        McpServerConfig config = conn.config();

        String header = "## " + serverName;
        if (config.getCommand() != null && !config.getCommand().isBlank()) {
            String cmd = config.getCommand();
            if (config.getArgs() != null && !config.getArgs().isEmpty()) {
                cmd = cmd + " " + String.join(" ", config.getArgs());
            }
            header = header + " (`" + cmd + "`)";
        }
        sb.append(header);

        List<McpSchema.Tool> tools = fetchTools(conn.client());
        if (!tools.isEmpty()) {
            sb.append("\n\n### Available Tools\n");
            for (McpSchema.Tool tool : tools) {
                String desc = tool.description() != null ? tool.description() : "";
                sb.append("- ").append(tool.name()).append(": ").append(desc);
                if (tool.inputSchema() != null) {
                    String schemaStr = formatInputSchema(tool.inputSchema());
                    if (!schemaStr.isEmpty()) {
                        sb.append("\n    Input Schema:\n    ")
                                .append(schemaStr.replace("\n", "\n    "));
                    }
                }
                sb.append("\n\n");
            }
        }

        List<McpSchema.ResourceTemplate> templates = fetchResourceTemplates(conn.client());
        if (!templates.isEmpty()) {
            sb.append("\n### Resource Templates\n");
            for (McpSchema.ResourceTemplate t : templates) {
                String uri = t.uriTemplate() != null ? t.uriTemplate() : "";
                String name = t.name() != null ? t.name() : "";
                String desc = t.description() != null ? t.description() : "";
                sb.append("- ").append(uri).append(" (").append(name).append(")");
                if (!desc.isEmpty()) {
                    sb.append(": ").append(desc);
                }
                sb.append("\n");
            }
        }

        List<McpSchema.Resource> resources = fetchResources(conn.client());
        if (!resources.isEmpty()) {
            sb.append("\n### Direct Resources\n");
            for (McpSchema.Resource r : resources) {
                String uri = r.uri() != null ? r.uri() : "";
                String name = r.name() != null ? r.name() : "";
                String desc = r.description() != null ? r.description() : "";
                sb.append("- ").append(uri).append(" (").append(name).append(")");
                if (!desc.isEmpty()) {
                    sb.append(": ").append(desc);
                }
                sb.append("\n");
            }
        }

        List<McpSchema.Prompt> prompts = fetchPrompts(conn.client());
        if (!prompts.isEmpty()) {
            sb.append("\n### Available Prompts\n");
            for (McpSchema.Prompt prompt : prompts) {
                String title = prompt.name() != null ? prompt.name() : "";
                String desc =
                        prompt.description() != null ? prompt.description() : "No description";
                sb.append("- ").append(title).append(": ").append(desc);
                if (prompt.arguments() != null && !prompt.arguments().isEmpty()) {
                    String argsStr =
                            prompt.arguments().stream()
                                    .map(
                                            arg -> {
                                                String argName =
                                                        arg.name() != null ? arg.name() : "";
                                                String required =
                                                        Boolean.TRUE.equals(arg.required())
                                                                ? " (required)"
                                                                : "";
                                                String argDesc =
                                                        arg.description() != null
                                                                ? ": " + arg.description()
                                                                : "";
                                                return argName + required + argDesc;
                                            })
                                    .collect(java.util.stream.Collectors.joining(", "));
                    sb.append("\n    Arguments: ").append(argsStr);
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private List<McpSchema.Tool> fetchTools(IMcpClient client) {
        try {
            if (client instanceof IMcpSyncClient sync) {
                McpSchema.ListToolsResult result = sync.listTools();
                return result != null && result.tools() != null ? result.tools() : List.of();
            }
            if (client instanceof IMcpASyncClient async) {
                McpSchema.ListToolsResult result = async.listTools().block();
                return result != null && result.tools() != null ? result.tools() : List.of();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private List<McpSchema.Resource> fetchResources(IMcpClient client) {
        try {
            if (client instanceof IMcpSyncClient sync) {
                McpSchema.ListResourcesResult result = sync.listResources();
                return result != null && result.resources() != null
                        ? result.resources()
                        : List.of();
            }
            if (client instanceof IMcpASyncClient async) {
                McpSchema.ListResourcesResult result = async.listResources().block();
                return result != null && result.resources() != null
                        ? result.resources()
                        : List.of();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private List<McpSchema.ResourceTemplate> fetchResourceTemplates(IMcpClient client) {
        try {
            if (client instanceof IMcpSyncClient sync) {
                McpSchema.ListResourceTemplatesResult result = sync.listResourceTemplates();
                return result != null && result.resourceTemplates() != null
                        ? result.resourceTemplates()
                        : List.of();
            }
            if (client instanceof IMcpASyncClient async) {
                McpSchema.ListResourceTemplatesResult result =
                        async.listResourceTemplates().block();
                return result != null && result.resourceTemplates() != null
                        ? result.resourceTemplates()
                        : List.of();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private List<McpSchema.Prompt> fetchPrompts(IMcpClient client) {
        try {
            if (client instanceof IMcpSyncClient sync) {
                McpSchema.ListPromptsResult result = sync.listPrompts();
                return result != null && result.prompts() != null ? result.prompts() : List.of();
            }
            if (client instanceof IMcpASyncClient async) {
                McpSchema.ListPromptsResult result = async.listPrompts().block();
                return result != null && result.prompts() != null ? result.prompts() : List.of();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private String formatInputSchema(Object inputSchema) {
        if (inputSchema == null) {
            return "";
        }
        try {
            String json =
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputSchema);
            return json != null ? json : "";
        } catch (Exception e) {
            return "";
        }
    }
}
