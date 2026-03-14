package com.hhoa.kline.web.service;

import com.hhoa.kline.web.controller.dto.CommandContextRequestDTO;

public interface CommandsService {

    void addToCline(CommandContextRequestDTO request);

    void fixWithCline(CommandContextRequestDTO request);

    void explainWithCline(CommandContextRequestDTO request);

    void improveWithCline(CommandContextRequestDTO request);
}
