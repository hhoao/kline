package com.hhoa.kline.core.core.services.mcp;

import java.util.Optional;

public interface IInternalClientFactory {

    Optional<IMcpClient> createInternalClient();
}
