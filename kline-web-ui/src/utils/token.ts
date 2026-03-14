/**
 * Token 工具函数
 * 用于获取和管理认证 token
 */

/**
 * 从 localStorage 获取 token
 * @param key - localStorage 中的 key（默认为 'satoken'）
 * @returns token 字符串或 null
 */
export function getTokenFromStorage(key: string = 'satoken'): string | null {
  if (typeof window === 'undefined') {
    return null
  }
  return localStorage.getItem(key)
}

/**
 * 设置 Cookie
 * @param name - Cookie 名称
 * @param value - Cookie 值
 * @param days - 过期天数（可选，默认 7 天）
 */
export function setCookie(name: string, value: string, days: number = 7): void {
  if (typeof document === 'undefined') {
    return
  }
  
  let cookie = `${name}=${encodeURIComponent(value)}`
  
  if (days) {
    const date = new Date()
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000)
    cookie += `; expires=${date.toUTCString()}`
  }
  
  // 设置 path 为根路径，确保所有请求都能携带
  cookie += '; path=/'
  
  document.cookie = cookie
}

/**
 * 将 token 保存到 localStorage 和 cookie
 * @param token - token 字符串
 * @param key - localStorage 和 cookie 中的 key（默认为 'satoken'）
 */
export function saveTokenToStorage(token: string, key: string = 'satoken'): void {
  if (typeof window === 'undefined') {
    return
  }
  localStorage.setItem(key, token)
  // 同时设置到 cookie
  setCookie(key, token, 7)
}

/**
 * 删除 Cookie
 * @param name - Cookie 名称
 */
export function deleteCookie(name: string): void {
  if (typeof document === 'undefined') {
    return
  }
  // 通过设置过期时间为过去来删除 cookie
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`
}

/**
 * 从 localStorage 和 cookie 删除 token
 * @param key - localStorage 和 cookie 中的 key（默认为 'satoken'）
 */
export function removeTokenFromStorage(key: string = 'satoken'): void {
  if (typeof window === 'undefined') {
    return
  }
  localStorage.removeItem(key)
  // 同时删除 cookie
  deleteCookie(key)
}

/**
 * 从 URL 查询参数获取 token
 * @param paramName - 查询参数名称（默认为 'satoken'）
 * @returns token 字符串或 null
 */
export function getTokenFromUrl(paramName: string = 'satoken'): string | null {
  if (typeof window === 'undefined') {
    return null
  }
  const urlParams = new URLSearchParams(window.location.search)
  return urlParams.get(paramName)
}

/**
 * 从 Cookie 获取 token
 * @param cookieName - Cookie 名称（默认为 'satoken'）
 * @returns token 字符串或 null
 */
export function getTokenFromCookie(cookieName: string = 'satoken'): string | null {
  if (typeof document === 'undefined') {
    return null
  }
  const cookies = document.cookie.split(';')
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=')
    if (name === cookieName) {
      return decodeURIComponent(value)
    }
  }
  return null
}

/**
 * 获取 token（按优先级：参数 > localStorage > cookie > URL）
 * @param providedToken - 直接提供的 token（优先级最高）
 * @param storageKey - localStorage 中的 key（默认为 'satoken'）
 * @returns token 字符串或 null
 */
export function getToken(
  providedToken?: string | null,
  storageKey: string = 'satoken'
): string | null {
  // 1. 优先使用直接提供的 token
  if (providedToken) {
    return providedToken
  }
  
  // 2. 尝试从 localStorage 获取
  const storageToken = getTokenFromStorage(storageKey)
  if (storageToken) {
    return storageToken
  }
  
  // 3. 尝试从 Cookie 获取
  const cookieToken = getTokenFromCookie(storageKey)
  if (cookieToken) {
    return cookieToken
  }
  
  // 4. 尝试从 URL 查询参数获取
  const urlToken = getTokenFromUrl(storageKey)
  if (urlToken) {
    return urlToken
  }
  
  return null
}

