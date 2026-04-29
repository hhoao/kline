package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.BrowserActionInput;
import com.hhoa.kline.core.core.tools.handlers.BrowserToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * 浏览器操作工具规格
 *
 * @author hhoa
 */
public final class BrowserActionTool extends BaseToolSpec
        implements ToolSpecProvider<BrowserActionInput, BrowserToolHandler> {

    private static final String DESCRIPTION =
            "Request to interact with a Puppeteer-controlled browser. Every action, except `close`, will be responded to with a screenshot of the browser's current state, along with any new console logs. You may only perform one browser action per message, and wait for the user's response including a screenshot and logs to determine the next action.\n"
                    + "- The sequence of actions **must always start with** launching the browser at a URL, and **must always end with** closing the browser. If you need to visit a new URL that is not possible to navigate to from the current webpage, you must first close the browser, then launch again at the new URL.\n"
                    + "- While the browser is active, only the `browser_action` tool can be used. No other tools should be called during this time. You may proceed to use other tools only after closing the browser. For example if you run into an error and need to fix a file, you must close the browser, then use other tools to make the necessary changes, then re-launch the browser to verify the result.\n"
                    + "- The browser window has a resolution of **{{BROWSER_VIEWPORT_WIDTH}}x{{BROWSER_VIEWPORT_HEIGHT}}** pixels. When performing any click actions, ensure the coordinates are within this resolution range.\n"
                    + "- Before clicking on any elements such as icons, links, or buttons, you must consult the provided screenshot of the page to determine the coordinates of the element. The click should be targeted at the **center of the element**, not on its edges.";

    private static final Function<SystemPromptContext, Boolean> CONTEXT_REQUIREMENTS =
            (context) -> Boolean.TRUE.equals(context.getSupportsBrowserUse());

    @Override
    public String id() {
        return ClineDefaultTool.BROWSER.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return CONTEXT_REQUIREMENTS;
    }
}
