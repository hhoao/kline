package com.hhoa.kline.core.core.ignore;

public interface ClineIgnoreController {
    /**
     * 验证是否允许访问指定路径
     *
     * @param relPath 相对路径
     * @return 是否允许访问
     */
    boolean validateAccess(String relPath);

    /**
     * 验证命令是否访问了被忽略的文件
     *
     * @param command 命令字符串
     * @return 如果访问了被忽略的文件，返回文件路径；否则返回 null
     */
    String validateCommand(String command);
}
