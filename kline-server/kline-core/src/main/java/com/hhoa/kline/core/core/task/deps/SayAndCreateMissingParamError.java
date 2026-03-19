package com.hhoa.kline.core.core.task.deps;

@FunctionalInterface
public interface SayAndCreateMissingParamError {

    String apply(String toolName, String paramName, String relPath);
}
