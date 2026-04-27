package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;
import java.util.function.Function;

/**
 * 浏览器操作工具规格
 *
 * @author hhoa
 */
public class BrowserActionTool extends BaseToolSpec {

    private static final String DESCRIPTION =
            "Request to interact with a Puppeteer-controlled browser. Every action, except `close`, will be responded to with a screenshot of the browser's current state, along with any new console logs. You may only perform one browser action per message, and wait for the user's response including a screenshot and logs to determine the next action.\n"
                    + "- The sequence of actions **must always start with** launching the browser at a URL, and **must always end with** closing the browser. If you need to visit a new URL that is not possible to navigate to from the current webpage, you must first close the browser, then launch again at the new URL.\n"
                    + "- While the browser is active, only the `browser_action` tool can be used. No other tools should be called during this time. You may proceed to use other tools only after closing the browser. For example if you run into an error and need to fix a file, you must close the browser, then use other tools to make the necessary changes, then re-launch the browser to verify the result.\n"
                    + "- The browser window has a resolution of **{{BROWSER_VIEWPORT_WIDTH}}x{{BROWSER_VIEWPORT_HEIGHT}}** pixels. When performing any click actions, ensure the coordinates are within this resolution range.\n"
                    + "- Before clicking on any elements such as icons, links, or buttons, you must consult the provided screenshot of the page to determine the coordinates of the element. The click should be targeted at the **center of the element**, not on its edges.";

    private static final Function<SystemPromptContext, Boolean> CONTEXT_REQUIREMENTS =
            (context) -> Boolean.TRUE.equals(context.getSupportsBrowserUse());

    public static ToolSpec create(ModelFamily modelFamily) {
        if (modelFamily == ModelFamily.NATIVE_NEXT_GEN) {
            return createNativeNextGenVariant();
        }

        return createGenericVariant(modelFamily);
    }

    private static ToolSpec createGenericVariant(ModelFamily modelFamily) {
        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.BROWSER.getValue())
                .name(ClineDefaultTool.BROWSER.getValue())
                .description(DESCRIPTION)
                .contextRequirements(CONTEXT_REQUIREMENTS)
                .parameters(
                        List.of(
                                createParameter(
                                        "action",
                                        true,
                                        "The action to perform. The available actions are: \n"
                                                + "* launch: Launch a new Puppeteer-controlled browser instance at the specified URL. This **must always be the first action**. \n"
                                                + "  - Use with the `url` parameter to provide the URL. \n"
                                                + "  - Ensure the URL is valid and includes the appropriate protocol (e.g. http://localhost:3000/page, file:///path/to/file.html, etc.) \n"
                                                + "* click: Click at a specific x,y coordinate. \n"
                                                + "  - Use with the `coordinate` parameter to specify the location. \n"
                                                + "  - Always click in the center of an element (icon, button, link, etc.) based on coordinates derived from a screenshot. \n"
                                                + "* type: Type a string of text on the keyboard. You might use this after clicking on a text field to input text. \n"
                                                + "  - Use with the `text` parameter to provide the string to type. \n"
                                                + "* scroll_down: Scroll down the page by one page height. \n"
                                                + "* scroll_up: Scroll up the page by one page height. \n"
                                                + "* close: Close the Puppeteer-controlled browser instance. This **must always be the final browser action**. \n"
                                                + "  - Example: `<action>close</action>`",
                                        "Action to perform (e.g., launch, click, type, scroll_down, scroll_up, close)"),
                                createParameter(
                                        "url",
                                        false,
                                        "Use this for providing the URL for the `launch` action.\n"
                                                + "* Example: <url>https://example.com</url>",
                                        "URL to launch the browser at (optional)"),
                                createParameter(
                                        "coordinate",
                                        false,
                                        "The X and Y coordinates for the `click` action. Coordinates should be within the **{{BROWSER_VIEWPORT_WIDTH}}x{{BROWSER_VIEWPORT_HEIGHT}}** resolution.\n"
                                                + "* Example: <coordinate>450,300</coordinate>",
                                        "x,y coordinates (optional)"),
                                createParameter(
                                        "text",
                                        false,
                                        "Use this for providing the text for the `type` action.\n"
                                                + "* Example: <text>Hello, world!</text>",
                                        "Text to type (optional)")))
                .build();
    }

    private static ToolSpec createNativeNextGenVariant() {
        return ToolSpec.builder()
                .variant(ModelFamily.NATIVE_NEXT_GEN)
                .id(ClineDefaultTool.BROWSER.getValue())
                .name(ClineDefaultTool.BROWSER.getValue())
                .description(DESCRIPTION)
                .contextRequirements(CONTEXT_REQUIREMENTS)
                .parameters(
                        List.of(
                                createParameter(
                                        "action",
                                        true,
                                        "The action to perform. The available actions are: \n"
                                                + "* launch: Launch a new Puppeteer-controlled browser instance at the specified URL. This **must always be the first action**. \n"
                                                + "  - Use with the `url` parameter to provide the URL. \n"
                                                + "  - Ensure the URL is valid and includes the appropriate protocol (e.g. http://localhost:3000/page, file:///path/to/file.html, etc.) \n"
                                                + "* click: Click at a specific x,y coordinate. \n"
                                                + "  - Use with the `coordinate` parameter to specify the location. \n"
                                                + "  - Always click in the center of an element (icon, button, link, etc.) based on coordinates derived from a screenshot. \n"
                                                + "* type: Type a string of text on the keyboard. You might use this after clicking on a text field to input text. \n"
                                                + "  - Use with the `text` parameter to provide the string to type. \n"
                                                + "* scroll_down: Scroll down the page by one page height. \n"
                                                + "* scroll_up: Scroll up the page by one page height. \n"
                                                + "* close: Close the Puppeteer-controlled browser instance. This **must always be the final browser action**. \n"
                                                + "  - Example: 'scroll_up'",
                                        null),
                                createParameter(
                                        "url",
                                        false,
                                        "Use this for providing the URL for the `launch` action.",
                                        null),
                                createParameter(
                                        "coordinate",
                                        false,
                                        "x,y coordinates - The X and Y coordinates for the `click` action. Coordinates should be within the **{{BROWSER_VIEWPORT_WIDTH}}x{{BROWSER_VIEWPORT_HEIGHT}}** resolution. Example: '450,300'",
                                        null),
                                createParameter(
                                        "text",
                                        false,
                                        "Use this for providing the text for the `type` action. Example: 'Hello, world!'",
                                        null)))
                .build();
    }
}
