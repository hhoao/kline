package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.processor;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ErrorHandler;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.RetryPolicy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileStructureMode;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.MetadataManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.TableMetadataCache;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.util.EnumMap;
import java.util.Map;

public class FileProcessorFactory {

    private final Map<FileStructureMode, FileProcessor> processors =
            new EnumMap<>(FileStructureMode.class);

    public FileProcessorFactory(
            JdbcService jdbcService,
            JsonSerializer jsonSerializer,
            FileSystemHelper fileSystemHelper,
            TableMetadataCache tableMetadataCache,
            MetadataManager metadataManager,
            RetryPolicy retryPolicy,
            ErrorHandler errorHandler) {
        processors.put(
                FileStructureMode.SINGLE_JSON,
                new SingleJsonFileProcessor(
                        jdbcService,
                        jsonSerializer,
                        fileSystemHelper,
                        tableMetadataCache,
                        metadataManager,
                        retryPolicy,
                        errorHandler));
        processors.put(
                FileStructureMode.FIELD_FILES,
                new FieldFilesFileProcessor(
                        jdbcService,
                        jsonSerializer,
                        fileSystemHelper,
                        tableMetadataCache,
                        metadataManager,
                        retryPolicy,
                        errorHandler));
    }

    public FileProcessor getProcessor(FileStructureMode mode) {
        FileProcessor processor = processors.get(mode);
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported FileStructureMode: " + mode);
        }
        return processor;
    }
}
