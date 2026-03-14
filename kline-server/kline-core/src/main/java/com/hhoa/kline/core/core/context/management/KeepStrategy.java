package com.hhoa.kline.core.core.context.management;

public enum KeepStrategy {
    // 删除第一个核心用户/助手消息对之外的所有消息
    NONE,
    // 除了第一个核心用户/助手消息对外，还保留最后一个用户-助手对
    LAST_TWO,
    // 删除剩余用户-助手对的一半
    // 我们首先计算消息的一半，然后除以 2 得到对数。
    // 向下取整后，乘以 2 得到消息数。
    // 注意这也总是偶数。
    HALF,
    // 删除剩余用户-助手对的 3/4
    // 我们计算消息的 3/4，然后除以 2 得到对数。
    // 向下取整后，乘以 2 得到消息数。
    // 注意这也总是偶数。
    QUARTER;
}
