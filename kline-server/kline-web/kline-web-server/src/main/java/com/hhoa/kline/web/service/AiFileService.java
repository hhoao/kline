package com.hhoa.kline.web.service;

import java.util.List;

public interface AiFileService {

    void copyToClipboard(String request);

    void openFile(String request);

    void openImage(String request);

    void openMention(String request);

    String deleteRuleFile();

    String createRuleFile();

    String searchCommits(String request);

    List<String> selectFiles(Boolean request);

    String getRelativePaths();

    String searchFiles();

    String toggleClineRule();

    String toggleCursorRule();

    String toggleWindsurfRule();

    String refreshRules();

    void openDiskConversationHistory(String request);

    String toggleWorkflow();

    Boolean ifFileExistsRelativePath(String request);

    void openFileRelativePath(String request);

    void openFocusChainFile(String request);
}
