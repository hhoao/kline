package com.hhoa.kline.web.service.impl;

import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionMessage;
import com.hhoa.kline.web.service.UiService;
import com.hhoa.kline.web.utils.LoginUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class UiServiceImpl implements UiService {

    private final SubscriptionManager subscriptionManager =
            DefaultSubscriptionManager.getInstance();

    @Override
    public Flux<SubscriptionMessage> subscribe() {
        String userId = getUserId();
        return subscriptionManager.subscribe(userId);
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果无法获取则返回 "default"
     */
    private String getUserId() {
        Long loginId = LoginUserUtil.getLoginIdDefaultNull();
        return loginId != null ? String.valueOf(loginId) : "default";
    }
}
