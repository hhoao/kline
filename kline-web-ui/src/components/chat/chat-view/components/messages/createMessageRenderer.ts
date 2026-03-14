/**
 * Factory function to create a message renderer function
 * This is kept for backward compatibility but is not needed in Vue
 * as we can directly use the MessageRenderer component in templates
 * 
 * @deprecated Use MessageRenderer component directly in templates instead
 */

import type { ClineMessage } from '@/shared/ExtensionMessage'
import type { MessageHandlers } from '../../types/chatTypes'

/**
 * Factory function for creating message renderer configuration
 * In Vue, this is mainly for backward compatibility
 * The actual rendering is done directly in templates using MessageRenderer component
 */
export const createMessageRenderer = (
  groupedMessages: (ClineMessage | ClineMessage[])[],
  modifiedMessages: ClineMessage[],
  expandedRows: Record<number, boolean>,
  onToggleExpand: (ts: number) => void,
  onHeightChange: (isTaller: boolean) => void,
  onSetQuote: (quote: string | null) => void,
  inputValue: string,
  messageHandlers: MessageHandlers
) => {
  // Return a function that can be used if needed
  // In Vue, we typically use the component directly in templates
  return (index: number, messageOrGroup: ClineMessage | ClineMessage[]) => {
    // This is mainly for compatibility - actual rendering happens in template
    return {
      index,
      messageOrGroup,
      groupedMessages,
      modifiedMessages,
      expandedRows,
      inputValue,
      messageHandlers,
      onToggleExpand,
      onHeightChange,
      onSetQuote,
    }
  }
}

