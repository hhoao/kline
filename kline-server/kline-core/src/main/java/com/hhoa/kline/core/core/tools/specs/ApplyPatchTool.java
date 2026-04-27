package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * Apply Patch 工具规格 - V4A diff 格式的文件补丁工具
 *
 * @author hhoa
 */
public class ApplyPatchTool extends BaseToolSpec {

    private static final String DESCRIPTION =
            """
            This is a custom utility that makes it more convenient to add, remove, move, or edit code \
            in a single file. `apply_patch` effectively allows you to execute a diff/patch against a file, \
            but the format of the diff specification is unique to this task, so pay careful attention to these \
            instructions. To use the `apply_patch` command, you should pass a message of the following structure \
            as "input":

            %%bash
            apply_patch <<"EOF"
            *** Begin Patch
            [YOUR_PATCH]
            *** End Patch
            EOF

            Where [YOUR_PATCH] is the actual content of your patch, specified in the following V4A diff format.

            *** [ACTION] File: [path/to/file] -> ACTION can be one of Add, Update, or Delete.

            In a Add File section, every line of the new file (including blank/empty lines) MUST start with a \
            `+` prefix. Do not include any unprefixed lines inside an Add section
            In a Update/Delete section, repeat the following for each snippet of code that needs to be changed:
            [context_before] -> See below for further instructions on context.
            - [old_code] -> Precede the old code with a minus sign.
            + [new_code] -> Precede the new, replacement code with a plus sign.
            [context_after] -> See below for further instructions on context.

            For instructions on [context_before] and [context_after]:
            - By default, show 3 lines of code immediately above and 3 lines immediately below each change. \
            If a change is within 3 lines of a previous change, do NOT duplicate the first change's \
            [context_after] lines in the second change's [context_before] lines.
            - If 3 lines of context is insufficient to uniquely identify the snippet of code within the file, \
            use the @@ operator to indicate the class or function to which the snippet belongs. For instance, \
            we might have:
            @@ class BaseClass
            [3 lines of pre-context]
            - [old_code]
            + [new_code]
            [3 lines of post-context]

            - If a code block is repeated so many times in a class or function such that even a single @@ \
            statement and 3 lines of context cannot uniquely identify the snippet of code, you can use multiple \
            `@@` statements to jump to the right context. For instance:

            @@ class BaseClass
            @@ \tdef method():
            [3 lines of pre-context]
            - [old_code]
            + [new_code]
            [3 lines of post-context]

            Note, then, that we do not use line numbers in this diff format, as the context is enough to \
            uniquely identify code. An example of a message that you might pass as "input" to this function, \
            in order to apply a patch, is shown below.

            %%bash
            apply_patch <<"EOF"
            *** Begin Patch
            *** Update File: pygorithm/searching/binary_search.py
            @@ class BaseClass
            @@     def search():
            -          pass
            +          raise NotImplementedError()

            @@ class Subclass
            @@     def search():
            -          pass
            +          raise NotImplementedError()

            *** End Patch
            EOF""";

    public static ClineToolSpec create(ModelFamily modelFamily) {
        if (modelFamily != ModelFamily.NATIVE_GPT_5
                && modelFamily != ModelFamily.NATIVE_GPT_5_1
                && modelFamily != ModelFamily.GPT_5) {
            return null;
        }

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.APPLY_PATCH.getValue())
                .name(ClineDefaultTool.APPLY_PATCH.getValue())
                .description(DESCRIPTION)
                .contextRequirements(
                        context -> {
                            if (context.getProviderInfo() == null
                                    || context.getProviderInfo().getModel() == null) {
                                return false;
                            }
                            String modelId = context.getProviderInfo().getModel().getId();
                            return ModelFamily.isGPT5ModelFamily(modelId)
                                    || ModelFamily.isGptOssModelFamily(modelId);
                        })
                .parameters(
                        List.of(
                                createParameter(
                                        "input",
                                        true,
                                        "The apply_patch command that you wish to execute.",
                                        null),
                                createTaskProgressParameter()))
                .build();
    }
}
