import { ResultEnum } from "../http/axios/httpEnum";
import type { SubscriptionMessage, SSEClientOptions, EventType, EventCallback, MessageHandler, ConnectionState, ResponseVO } from "./types";

export class SSEClient<T extends SubscriptionMessage = SubscriptionMessage> {
  private url: string;
  private eventSource: EventSource | null = null;
  private readonly reconnectDelay: number;
  private readonly autoReconnect: boolean;
  private readonly maxReconnectAttempts: number;
  private reconnectAttempts: number = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  // 心跳相关
  private readonly heartbeatInterval: number;
  private readonly heartbeatTimeout: number;
  private heartbeatTimer: ReturnType<typeof setTimeout> | null = null;
  private lastHeartbeatTime: number = 0;

  // 调试模式
  private readonly debug: boolean;

  // 事件监听器 - 使用 Set 避免重复
  private listeners: Record<EventType, Set<EventCallback>> = {
    open: new Set(),
    message: new Set(),
    error: new Set(),
    close: new Set(),
    reconnecting: new Set(),
  };

  // 消息类型处理器
  private messageHandlers: Map<string, Set<MessageHandler<T>>> = new Map();

  // 连接状态
  private _isManualDisconnect: boolean = false;

  constructor(options: SSEClientOptions = {}) {
    this.url = options.url || "";
    this.reconnectDelay = options.reconnectDelay || 3000;
    this.autoReconnect = options.autoReconnect !== false;
    this.maxReconnectAttempts = options.maxReconnectAttempts || Infinity;
    this.heartbeatInterval = options.heartbeatInterval || 30000;
    this.heartbeatTimeout = options.heartbeatTimeout || 5000;
    this.debug = options.debug || false;
  }

  /**
   * 日志输出
   */
  private _log(level: "log" | "warn" | "error", ...args: any[]): void {
    if (this.debug || level === "error") {
      console[level]("[SSEClient]", ...args);
    }
  }

  /**
   * 连接到 SSE 服务器
   * @param url - SSE 服务器地址
   */
  connect(url?: string): void {
    if (url) this.url = url;

    if (!this.url) {
      throw new Error("SSE URL is required");
    }

    this._isManualDisconnect = false;
    this.disconnect();

    try {
      this._log("log", "Connecting to:", this.url);
      this.eventSource = new EventSource(this.url);

      this.eventSource.onopen = this._handleOpen.bind(this);
      this.eventSource.onmessage = this._handleMessage.bind(this);
      this.eventSource.onerror = this._handleError.bind(this);

      // 启动心跳检测
      if (this.heartbeatInterval > 0) {
        this._startHeartbeat();
      }
    } catch (error) {
      this._log("error", "Failed to connect:", error);
      this._emit("error", error);
    }
  }

  /**
   * 处理连接打开事件
   */
  private _handleOpen(event: Event): void {
    this._log("log", "Connected to:", this.url);
    this.reconnectAttempts = 0;
    this.lastHeartbeatTime = Date.now();
    this._emit("open", event);
  }

  /**
   * 处理接收到的消息
   */
  private _handleMessage(event: MessageEvent): void {
    try {
      this.lastHeartbeatTime = Date.now();

      // 解析 ResponseVO<SubscriptionMessage> 格式
      const response: ResponseVO<T> = JSON.parse(event.data);

      // 检查响应状态
      if (response.code !== ResultEnum.SUCCESS) {
        this._log("error", "Error response:", response.msg);
        this._emit("error", new Error(response.msg));
        return;
      }

      const message = response.data;

      // 触发通用消息监听器
      this._emit("message", message);

      // 根据消息类型触发特定处理器
      if (message?.type) {
        const handlers = this.messageHandlers.get(message.type);
        if (handlers && handlers.size > 0) {
          handlers.forEach((handler) => {
            try {
              handler(message);
            } catch (error) {
              this._log(
                "error",
                `Error in message handler for type ${message.type}:`,
                error
              );
            }
          });
        }
      }
    } catch (error) {
      this._log("error", "Failed to parse message:", error);
      this._emit("error", error);
    }
  }

  /**
   * 处理连接错误事件
   */
  private _handleError(error: Event): void {
    this._log("error", "Connection error:", error);
    this._emit("error", error);

    // 停止心跳
    this._stopHeartbeat();

    // 只在非手动断开且允许自动重连时进行重连
    if (
      !this._isManualDisconnect &&
      this.autoReconnect &&
      this.reconnectAttempts < this.maxReconnectAttempts
    ) {
      this._scheduleReconnect();
    }
  }

  /**
   * 启动心跳检测
   */
  private _startHeartbeat(): void {
    this._stopHeartbeat();

    this.heartbeatTimer = setInterval(() => {
      const now = Date.now();
      const timeSinceLastHeartbeat = now - this.lastHeartbeatTime;

      if (
        timeSinceLastHeartbeat >
        this.heartbeatInterval + this.heartbeatTimeout
      ) {
        this._log("warn", "Heartbeat timeout, reconnecting...");
        this._handleError(new Event("heartbeat-timeout"));
      }
    }, this.heartbeatInterval);
  }

  /**
   * 停止心跳检测
   */
  private _stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * 注册消息类型处理器（支持多个处理器）
   * @param messageType - 消息类型
   * @param handler - 处理函数
   */
  onMessageType<M extends T = T>(
    messageType: string,
    handler: MessageHandler<M>
  ): () => void {
    if (typeof handler !== "function") {
      throw new Error("Handler must be a function");
    }

    if (!this.messageHandlers.has(messageType)) {
      this.messageHandlers.set(messageType, new Set());
    }

    this.messageHandlers.get(messageType)!.add(handler as MessageHandler<T>);

    // 返回取消订阅函数
    return () => this.offMessageType(messageType, handler as MessageHandler<T>);
  }

  /**
   * 移除消息类型处理器
   * @param messageType - 消息类型
   * @param handler - 处理函数（可选，不传则移除该类型的所有处理器）
   */
  offMessageType(messageType: string, handler?: MessageHandler<T>): void {
    const handlers = this.messageHandlers.get(messageType);
    if (!handlers) return;

    if (handler) {
      handlers.delete(handler);
      if (handlers.size === 0) {
        this.messageHandlers.delete(messageType);
      }
    } else {
      this.messageHandlers.delete(messageType);
    }
  }

  /**
   * 注册事件监听器
   * @param event - 事件名称
   * @param callback - 回调函数
   * @returns 取消订阅函数
   */
  on(event: EventType, callback: EventCallback): () => void {
    if (this.listeners[event]) {
      this.listeners[event].add(callback);
    }

    // 返回取消订阅函数
    return () => this.off(event, callback);
  }

  /**
   * 一次性事件监听器
   * @param event - 事件名称
   * @param callback - 回调函数
   */
  once(event: EventType, callback: EventCallback): void {
    const wrappedCallback = (data: any) => {
      callback(data);
      this.off(event, wrappedCallback);
    };
    this.on(event, wrappedCallback);
  }

  /**
   * 移除事件监听器
   * @param event - 事件名称
   * @param callback - 回调函数（可选，不传则移除该事件的所有监听器）
   */
  off(event: EventType, callback?: EventCallback): void {
    if (this.listeners[event]) {
      if (callback) {
        this.listeners[event].delete(callback);
      } else {
        this.listeners[event].clear();
      }
    }
  }

  /**
   * 触发事件
   */
  private _emit(event: EventType, data: any): void {
    if (this.listeners[event]) {
      this.listeners[event].forEach((callback) => {
        try {
          callback(data);
        } catch (error) {
          this._log("error", `Error in ${event} listener:`, error);
        }
      });
    }
  }

  /**
   * 安排重连
   */
  private _scheduleReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    this.reconnectAttempts++;
    // 指数退避，最大不超过 30 秒
    const delay = Math.min(
      this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1),
      30000
    );

    this._log(
      "log",
      `Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`
    );
    this._emit("reconnecting", { attempt: this.reconnectAttempts, delay });

    this.reconnectTimer = setTimeout(() => {
      this._log("log", "Attempting to reconnect...");
      this.connect();
    }, delay);
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this._isManualDisconnect = true;

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    this._stopHeartbeat();

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this._log("log", "Disconnected");
      this._emit("close", null);
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return (
      this.eventSource !== null &&
      this.eventSource.readyState === EventSource.OPEN
    );
  }

  /**
   * 获取连接状态
   */
  getState(): ConnectionState {
    if (!this.eventSource) return "CLOSED";

    switch (this.eventSource.readyState) {
      case EventSource.CONNECTING:
        return "CONNECTING";
      case EventSource.OPEN:
        return "OPEN";
      case EventSource.CLOSED:
        return "CLOSED";
      default:
        return "CLOSED";
    }
  }

  /**
   * 获取当前重连次数
   */
  getReconnectAttempts(): number {
    return this.reconnectAttempts;
  }

  /**
   * 清除所有监听器和处理器
   */
  destroy(): void {
    this.disconnect();

    // 清除所有监听器
    Object.keys(this.listeners).forEach((event) => {
      this.listeners[event as EventType].clear();
    });

    // 清除所有消息处理器
    this.messageHandlers.clear();
  }
}
