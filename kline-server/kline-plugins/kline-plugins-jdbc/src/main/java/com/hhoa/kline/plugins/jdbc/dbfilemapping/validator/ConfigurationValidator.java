package com.hhoa.kline.plugins.jdbc.dbfilemapping.validator;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ConflictStrategyDetector;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ConflictStrategy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.ValidationResult;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 配置验证器 Validates mapping configuration for correctness */
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final JdbcService jdbcService;
    private final ConflictStrategyDetector strategyDetector;

    public ConfigurationValidator(JdbcService jdbcService) {
        if (jdbcService == null) {
            throw new IllegalArgumentException("JdbcService cannot be null");
        }
        this.jdbcService = jdbcService;
        this.strategyDetector = new ConflictStrategyDetector(jdbcService);
    }

    /**
     * 验证映射配置 Validates a mapping configuration
     *
     * @param config the configuration to validate
     * @return validation result with errors and warnings
     */
    public ValidationResult validate(MappingConfiguration config) {
        ValidationResult result = new ValidationResult();

        if (config == null) {
            result.addError("Configuration cannot be null");
            return result;
        }

        // Validate required fields
        validateRequiredFields(config, result);

        // If basic validation fails, don't proceed with database/filesystem checks
        if (!result.isValid()) {
            return result;
        }

        // Validate database schema and table
        validateSchemaAndTable(config, result);

        // Validate primary key column
        validatePrimaryKeyColumn(config, result);

        // Validate target directory
        validateTargetDirectory(config, result);

        // Validate field configurations
        validateFieldConfiguration(config, result);

        // Validate numeric configurations
        validateNumericConfiguration(config, result);

        // Auto-detect conflict strategy if set to AUT
        if (config.getConflictStrategy() == ConflictStrategy.AUTO) {
            ConflictStrategyDetector.StrategyDetectionResult strategyDetectionResult =
                    strategyDetector.detectAndConfigure(config);
            config.setConflictStrategy(strategyDetectionResult.getDetectedStrategy());
            logger.info(
                    "Auto-detected conflict strategy for table {}: {}",
                    config.getTableName(),
                    strategyDetectionResult.getDetectedStrategy());
        }

        return result;
    }

    /** 验证必需字段 */
    private void validateRequiredFields(MappingConfiguration config, ValidationResult result) {
        if (config.getTableName() == null || config.getTableName().trim().isEmpty()) {
            result.addError("Table name is required");
        }

        if (config.getTargetDirectory() == null || config.getTargetDirectory().trim().isEmpty()) {
            result.addError("Target directory is required");
        }

        if (config.getPrimaryKeyColumn() == null || config.getPrimaryKeyColumn().trim().isEmpty()) {
            result.addError("Primary key column is required");
        }

        if (config.getSchemaName() == null || config.getSchemaName().trim().isEmpty()) {
            result.addWarning("Schema name is empty, will use default 'public'");
            config.setSchemaName("public");
        }
    }

    /** 验证schema和表是否存在 */
    private void validateSchemaAndTable(MappingConfiguration config, ValidationResult result) {
        try {
            // Check if schema exists
            boolean schemaExists = jdbcService.schemaExists(config.getSchemaName());
            if (!schemaExists) {
                result.addError(
                        String.format("Schema '%s' does not exist", config.getSchemaName()));
                return; // Don't check table if schema doesn't exist
            }

            // Check if table exists
            boolean tableExists =
                    jdbcService.tableExists(config.getSchemaName(), config.getTableName());
            if (!tableExists) {
                result.addError(
                        String.format(
                                "Table '%s.%s' does not exist",
                                config.getSchemaName(), config.getTableName()));
            }

        } catch (Exception e) {
            logger.error("Error validating schema and table", e);
            result.addError("Database error while validating schema and table: " + e.getMessage());
        }
    }

    /** 验证主键列是否存在 */
    private void validatePrimaryKeyColumn(MappingConfiguration config, ValidationResult result) {
        try {
            boolean columnExists =
                    jdbcService.columnExists(
                            config.getSchemaName(),
                            config.getTableName(),
                            config.getPrimaryKeyColumn());

            if (!columnExists) {
                result.addError(
                        String.format(
                                "Primary key column '%s' does not exist in table '%s.%s'",
                                config.getPrimaryKeyColumn(),
                                config.getSchemaName(),
                                config.getTableName()));
            }

        } catch (Exception e) {
            logger.error("Error validating primary key column", e);
            result.addError(
                    "Database error while validating primary key column: " + e.getMessage());
        }
    }

    /** 验证目标目录 */
    private void validateTargetDirectory(MappingConfiguration config, ValidationResult result) {
        String targetDir = config.getTargetDirectory();
        Path path = Paths.get(targetDir);

        // Check if path is absolute or relative
        if (!path.isAbsolute()) {
            result.addWarning("Target directory is relative path: " + targetDir);
        }

        File dir = path.toFile();

        // Check if directory exists
        if (!dir.exists()) {
            // Try to create it
            try {
                Files.createDirectories(path);
                result.addWarning("Target directory did not exist, created: " + targetDir);
            } catch (Exception e) {
                result.addError(
                        "Cannot create target directory: " + targetDir + " - " + e.getMessage());
                return;
            }
        }

        // Check if it's a directory
        if (!dir.isDirectory()) {
            result.addError("Target path is not a directory: " + targetDir);
            return;
        }

        // Check if directory is writable
        if (!Files.isWritable(path)) {
            result.addError("Target directory is not writable: " + targetDir);
        }

        // Check if directory is readable
        if (!Files.isReadable(path)) {
            result.addError("Target directory is not readable: " + targetDir);
        }
    }

    /** 验证字段配置 */
    private void validateFieldConfiguration(MappingConfiguration config, ValidationResult result) {
        // Check if both included and excluded fields are specified
        if (config.getIncludedFields() != null
                && !config.getIncludedFields().isEmpty()
                && config.getExcludedFields() != null
                && !config.getExcludedFields().isEmpty()) {
            result.addWarning(
                    "Both included and excluded fields are specified. "
                            + "Included fields will take precedence.");
        }

        // Check for empty lists
        if (config.getIncludedFields() != null && config.getIncludedFields().isEmpty()) {
            result.addWarning("Included fields list is empty, no fields will be synced");
        }
    }

    /** 验证数值配置 */
    private void validateNumericConfiguration(
            MappingConfiguration config, ValidationResult result) {
        if (config.getDebounceMillis() < 0) {
            result.addError("Debounce milliseconds cannot be negative");
        }

        if (config.getMaxBatchSize() <= 0) {
            result.addError("Max batch size must be positive");
        }

        if (config.getSyncIntervalSeconds() < 0) {
            result.addError("Sync interval seconds cannot be negative");
        }

        // Warnings for potentially problematic values
        if (config.getDebounceMillis() > 10000) {
            result.addWarning("Debounce delay is very high (>10s), may cause slow sync");
        }

        if (config.getMaxBatchSize() > 10000) {
            result.addWarning("Batch size is very large (>10000), may cause memory issues");
        }
    }
}
