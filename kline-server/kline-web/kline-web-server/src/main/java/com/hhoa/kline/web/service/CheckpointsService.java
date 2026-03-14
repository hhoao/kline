package com.hhoa.kline.web.service;

import com.hhoa.kline.web.controller.dto.CheckpointRestoreRequestDTO;
import java.util.List;

public interface CheckpointsService {

    void checkpointDiff(Long request);

    void checkpointRestore(CheckpointRestoreRequestDTO request);

    String getCwdHash(List<String> request);
}
