package com.hhoa.kline.core.core.shared;

import java.util.regex.Pattern;

/**
 * 提及正则表达式： - 目的：识别和突出显示以 '@' 开头的特定提及 - 这些提及可以是文件路径、URL 或确切的单词 'problems' -
 * 确保尾随标点符号（如逗号、句号等）不包含在匹配中，允许标点符号跟随提及而不成为其一部分
 */
public class ContextMentions {
    /**
     * 匹配： - 以 '/' 开头的文件或文件夹路径，包含任何非空白字符（包括路径中的句点） - 以协议（如 'http://'）开头的 URL，后跟任何非空白字符（包括查询参数） -
     * 确切的单词 'problems' - 确切的单词 'terminal' - 确切的单词 'git-changes' - 工作区前缀的文件路径：@workspace:name/path -
     * 工作区前缀的引号文件路径 - 引号文件路径（可以包含空格） - Git 提交哈希（7-40 个十六进制字符）
     *
     * <p>确保任何尾随标点符号（如 ','、'.'、'!' 等）不包含在匹配的提及中，允许标点符号在文本中自然地跟随提及
     */
    public static final Pattern MENTION_REGEX =
            Pattern.compile(
                    "@("
                            + "[\\w-]+:/[^\\s]*?"
                            + // 工作区前缀的文件路径：@workspace:name/path
                            "|[\\w-]+:\"\\/[^\"]*?\""
                            + // 工作区前缀的引号文件路径
                            "|/[^\\s]*?"
                            + // 简单文件路径（不能包含）
                            "|\"\\/[^\"]*?\""
                            + // 可以包含空格的引号文件路径
                            "|(?:\\w+:\\/\\/)[^\\s]+?"
                            + // URL
                            "|[a-f0-9]{7,40}\\b"
                            + // Git 提交哈希
                            "|problems\\b"
                            + // 确切的单词 'problems'
                            "|terminal\\b"
                            + // 确切的单词 'terminal'
                            "|git-changes\\b"
                            + // 确切的单词 'git-changes'
                            ")"
                            + "(?=[.,;:!?()]*(?=[\\s\\r\\n]|$))" // 尾随标点的前瞻（允许多个）
                    );

    public static final Pattern MENTION_REGEX_GLOBAL = Pattern.compile(MENTION_REGEX.pattern());
}
