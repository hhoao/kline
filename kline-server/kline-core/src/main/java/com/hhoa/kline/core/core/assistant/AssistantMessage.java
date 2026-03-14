package com.hhoa.kline.core.core.assistant;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @see <a
 *     href="https://github.com/anthropics/anthropic-sdk-typescript/blob/main/src/resources/messages/messages.ts">TypeScript
 *     AssistantMessageParam</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantMessage implements MessageParam {
    private List<UserContentBlock> content;

    public MessageRole getRole() {
        return MessageRole.ASSISTANT;
    }
}
