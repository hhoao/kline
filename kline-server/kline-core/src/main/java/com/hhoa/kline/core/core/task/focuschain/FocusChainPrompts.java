package com.hhoa.kline.core.core.task.focuschain;

/**
 * Prompt templates for focus chain / TodoWrite list management.
 *
 * <p>Each constant is a multi-line string template used by {@link AbstractFocusChainManager} when
 * generating focus chain instructions for the model.
 */
public final class FocusChainPrompts {

    private FocusChainPrompts() {}

    /** Prompt for initial list creation (after switching from plan mode to act mode). */
    public static final String INITIAL =
            """

            # TodoWrite CREATION REQUIRED - ACT MODE ACTIVATED

            **You've just switched from PLAN MODE to ACT MODE!**

            ** IMMEDIATE ACTION REQUIRED:**
            1. Create a comprehensive todo list in your NEXT assistant message
            2. Use the TodoWrite tool to provide the list
            3. Use structured TodoWrite items with content, status, and activeForm
            4. Use status pending, in_progress, or completed

            **Your TodoWrite list should include:**
               - All major implementation steps
               - Testing and validation tasks
               - Documentation updates if needed
               - Final verification steps

            Keep exactly one item in_progress while work is underway whenever possible.

            **Remember:** Keeping the TodoWrite list updated helps track progress and ensures nothing is missed.""";

    /** For when recommending but not requiring a list. */
    public static final String RECOMMENDED_INSTRUCTIONS =
            """

            1. Create a todo list using the TodoWrite tool in your next assistant message
            2. Create a comprehensive checklist of all steps needed
            3. Use structured items with status pending, in_progress, or completed

            **Benefits of creating a TodoWrite list now:**
            \t- Clear roadmap for implementation
            \t- Progress tracking throughout the task
            \t- Nothing gets forgotten or missed
            \t- Users can see, monitor, and edit the plan

            **Example structure:**```
            - [ ] Analyze requirements
            - [ ] Set up necessary files
            - [ ] Implement main functionality
            - [ ] Handle edge cases
            - [ ] Test the implementation
            - [ ] Verify results```

            Keeping the TodoWrite list updated helps track progress and ensures nothing is missed.""";

    /** Prompt for reminders to update the list periodically. */
    public static final String REMINDER =
            """

            1. To create or update a todo list, use the TodoWrite tool in the next assistant message
            2. Review each item and update its status:
               - Mark completed items as completed
               - Keep remaining items as pending
               - Mark the current item as in_progress
               - Add new items if you discover additional steps
            3. Modify the list as needed:
            \t\t- Add any new steps you've discovered
            \t\t- Reorder if the sequence has changed
            4. Ensure the list accurately reflects the current state

            **Remember:** Keeping the TodoWrite list updated helps track progress and ensures nothing is missed.""";

    /**
     * Completion prompt template. Use {{totalItems}} and {{currentFocusChainChecklist}}
     * placeholders.
     */
    public static final String COMPLETED =
            """

            **All {0} items have been completed!**

            **Completed Items:**
            {1}

            **Next Steps:**
            - If the task is fully complete and meets all requirements, use attempt_completion
            - If you''ve discovered additional work that wasn''t in the original scope (new features, improvements, edge cases, etc.), create a new TodoWrite list with those items
            - If there are related tasks or follow-up items the user might want, you can suggest them in a new checklist

            **Remember:** Only use attempt_completion if you''re confident the task is truly finished. If there''s any remaining work, create a new focus chain list to track it.""";

    /** Plan mode reminder template. */
    public static final String PLAN_MODE_REMINDER =
            """

            # TodoWrite List (Optional - Plan Mode)

            While in PLAN MODE, if you''ve outlined concrete steps or requirements for the user, you may create a preliminary todo list using the TodoWrite tool.

            Reminder on how to use TodoWrite:
            """
                    + REMINDER;

    /** Recommended prompt template. */
    public static final String RECOMMENDED =
            """

            # TodoWrite RECOMMENDED

            When starting a new task, it is recommended to create a todo list using the TodoWrite tool.

            """
                    + RECOMMENDED_INSTRUCTIONS;

    /** API request count prompt template. Use {0} for apiRequestCount. */
    public static final String API_REQUEST_COUNT =
            """

            # TodoWrite

            You''ve made {0} API requests without updating the todo list. It is strongly recommended that you use TodoWrite to track remaining work.

            """
                    + REMINDER;
}
