package com.hhoa.kline.core.service;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.TestApplication;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionMessage;
import com.hhoa.kline.core.subscription.message.PartialMessage;
import com.hhoa.kline.core.subscription.message.StateMessage;
import com.hhoa.kline.web.common.pojo.UserInfo;
import com.hhoa.kline.web.controller.dto.NewTaskRequestDTO;
import com.hhoa.kline.web.core.UserInfoContextHolder;
import com.hhoa.kline.web.service.impl.TaskServiceImpl;
import com.hhoa.kline.web.service.impl.UiServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

/**
 * TaskServiceImpl 集成测试
 *
 * @author hhoa
 * @since 2025/11/12
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@Slf4j
class TestTaskService {

    @Autowired private TaskServiceImpl taskService;
    @Autowired private UiServiceImpl uiService;

    @BeforeEach
    public void setUp() {
        UserInfoContextHolder.setUserInfo(new UserInfo(1L));
    }

    @Test
    public void start() throws InterruptedException {
        TimeUnit.DAYS.sleep(1);
    }

    @Test
    void testSubscribeStateCreateTaskSendMultipleMessages() {
        Flux<SubscriptionMessage> stateFlux = uiService.subscribe();
        AtomicInteger partialMessageCount = new AtomicInteger(0);

        stateFlux.subscribe(
                subMsg -> {
                    MessageType type = subMsg.getType();
                    if (type == MessageType.STATE) {
                        StateMessage extensionState =
                                JsonUtils.convertValue(subMsg, StateMessage.class);
                        List<ClineMessage> clineMessages =
                                extensionState.getState().getClineMessages();
                        List<String> list =
                                clineMessages.stream()
                                        .map(
                                                message ->
                                                        String.format(
                                                                "消息type: %s, text: %s: ",
                                                                message.getType(),
                                                                message.getText()))
                                        .toList();
                        log.info("收到状态更新: {}", list);
                    } else if (type == MessageType.PARTIAL_MESSAGE) {
                        PartialMessage partialMessage =
                                JsonUtils.convertValue(subMsg, PartialMessage.class);
                        String incrementContent = partialMessage.getIncrementContent();

                        int count = partialMessageCount.incrementAndGet();
                        log.info(
                                "[部分消息订阅] 收到第 {} 条部分消息: type={}, ask={}, text={}",
                                count,
                                partialMessage.getType(),
                                partialMessage.getAsk(),
                                incrementContent);
                    } else if (type == MessageType.CHAT_BUTTON_CLICKED) {
                        log.info("[状态订阅] 收到按钮点击: {}", subMsg);
                    }
                },
                error -> {
                    log.error("[状态订阅] 发生错误", error);
                    Assertions.fail("状态订阅不应该有错误: " + error.getMessage());
                });

        NewTaskRequestDTO newTaskRequest =
                NewTaskRequestDTO.builder()
                        //                                  .text("创建一个简单的Java HelloWorld程序")
                        //                                    .text("使用命令行pwd命令看看当前在哪个目录")
                        .text("你好啊")
                        .images(new ArrayList<>())
                        .files(new ArrayList<>())
                        .taskSettings(null)
                        .build();

        String taskId = taskService.newTask(newTaskRequest);
        Assertions.assertNotNull(taskId, "任务ID不应该为空");
        log.info("创建任务成功，任务ID: {}", taskId);

        log.info("测试完成");
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
