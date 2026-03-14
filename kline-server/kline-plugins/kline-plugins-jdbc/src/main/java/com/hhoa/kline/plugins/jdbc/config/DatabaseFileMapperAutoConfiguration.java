package com.hhoa.kline.plugins.jdbc.config;

import com.hhoa.kline.core.core.storage.GlobalFileNames;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.DatabaseFileMapper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.DatabaseFileMapperException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.DatabaseFileMapperFactory;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库文件映射器自动配置 在应用启动时自动初始化并创建 DatabaseFileMapper Bean
 *
 * @author hhoa
 */
@Configuration
@Slf4j
public class DatabaseFileMapperAutoConfiguration {

    @Autowired private JdbcService jdbcService;

    @Bean
    public DatabaseFileMapper databaseFileMapper() throws DatabaseFileMapperException {
        DatabaseFileMapperFactory factory =
                new DatabaseFileMapperFactory(jdbcService, GlobalFileNames.DB_MAPPING_METADATA_DIR);

        return factory.create(List.of());
    }
}
