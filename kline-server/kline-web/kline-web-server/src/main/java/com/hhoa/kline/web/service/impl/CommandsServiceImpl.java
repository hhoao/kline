package com.hhoa.kline.web.service.impl;

import com.hhoa.kline.web.controller.dto.CommandContextRequestDTO;
import com.hhoa.kline.web.service.CommandsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandsServiceImpl implements CommandsService {

    @Override
    public void addToCline(CommandContextRequestDTO request) {
        // TODO: 实现添加到 Cline 逻辑
    }

    @Override
    public void fixWithCline(CommandContextRequestDTO request) {
        // TODO: 实现使用 Cline 修复逻辑
    }

    @Override
    public void explainWithCline(CommandContextRequestDTO request) {
        // TODO: 实现使用 Cline 解释逻辑
    }

    @Override
    public void improveWithCline(CommandContextRequestDTO request) {
        // TODO: 实现使用 Cline 改进逻辑
    }
}
