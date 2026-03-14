package com.hhoa.kline.plugins.jdbc;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileStructureMode;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.DatabaseFileMapper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.DatabaseFileMapperException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.DatabaseFileMapperFactory;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Unit tests for ConfigurationManager */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@Slf4j
public class ConfigurationManagerTest {

    @Autowired private JdbcService jdbcService;

    String tempDir = "test/workspace";
    private DatabaseFileMapperFactory databaseFileMapperFactory;

    @BeforeEach
    public void setUp() {
        databaseFileMapperFactory = new DatabaseFileMapperFactory(jdbcService, tempDir);
    }

    @Test
    public void test() throws DatabaseFileMapperException, InterruptedException {
        MappingConfiguration validConfig = createValidConfig("public", "cpt_lowcode_config");
        DatabaseFileMapper databaseFileMapper = databaseFileMapperFactory.create(validConfig);
        databaseFileMapper.startAll();
        Thread.sleep(10000000);
    }

    private MappingConfiguration createValidConfig(String schema, String table) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSchemaName(schema);
        config.setTableName(table);
        config.setTargetDirectory(tempDir);
        config.setFileStructureMode(FileStructureMode.FIELD_FILES);
        config.setPrimaryKeyColumn("id");
        return config;
    }
}
