package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import com.hhoa.kline.web.common.mybatis.core.type.EncryptTypeHandler;
import com.hhoa.kline.web.enums.AiPlatformEnum;
import lombok.*;

/**
 * AI API 秘钥 DO
 *
 */
@TableName("ai_api_key")
@KeySequence("ai_chat_conversation_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL
// 等数据库，可不写。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiApiKeyDO extends BaseDO {

    /** 编号 */
    @TableId private Long id;

    /** 名称 */
    private String name;

    /** 密钥 */
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String apiKey;

    /**
     * 平台
     *
     * <p>枚举 {@link AiPlatformEnum}
     */
    private String platform;

    /** API 地址 */
    private String url;

    /**
     * 状态
     *
     * <p>枚举 {@link CommonStatusEnum}
     */
    private Integer status;
}
