package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.processor;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.SyncException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.SerializationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface FileProcessor {

    void syncFileToDatabase(Path filePath, MappingConfiguration config) throws SyncException;

    void handleFileDeletion(Path filePath, MappingConfiguration config) throws SyncException;

    void createFileFromRecord(Map<String, Object> record, MappingConfiguration config)
            throws SerializationException, IOException;

    void handleDatabaseDeletion(String tableName, Object primaryKey, MappingConfiguration config)
            throws SyncException;

    int getSyncIncrementCount(MappingConfiguration config, Object primaryKey);
}
