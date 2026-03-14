/**
 * ResponseVO 响应结构
 */
export interface ResponseVO<T> {
  /** 返回 0 表示成功，否则为错误码 */
  code: number;
  /** 提示信息 */
  msg: string;
  /** 返回的数据 */
  data: T;
}

/**
 * 订阅消息基础接口
 */
export interface SubscriptionMessage {
  /** 消息类型 */
  type: string;
  [key: string]: any;
}

/**
 * SSE 客户端配置
 */
export interface SSEClientOptions {
  /** SSE 服务器地址 */
  url?: string;
  /** 重连延迟时间(毫秒) */
  reconnectDelay?: number;
  /** 是否自动重连 */
  autoReconnect?: boolean;
  /** 最大重连次数 */
  maxReconnectAttempts?: number;
  /** 心跳检测间隔(毫秒)，0 表示禁用 */
  heartbeatInterval?: number;
  /** 心跳超时时间(毫秒) */
  heartbeatTimeout?: number;
  /** 是否启用调试日志 */
  debug?: boolean;
}

/**
 * 事件类型
 */
export type EventType = "open" | "message" | "error" | "close" | "reconnecting";

/**
 * 事件回调函数类型
 */
export type EventCallback<T = any> = (data: T) => void;

/**
 * 消息处理器类型
 */
export type MessageHandler<T extends SubscriptionMessage = SubscriptionMessage> = (
  message: T
) => void;

/**
 * 连接状态
 */
export type ConnectionState = "CONNECTING" | "OPEN" | "CLOSED";
