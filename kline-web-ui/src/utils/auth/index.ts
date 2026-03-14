import { Persistent } from '../cache/persistent';
import type { BasicKeys } from '../cache/persistent';
import { ACCESS_TOKEN_KEY } from '../../enums/cacheEnum';

export function getToken() {
  return getAuthCache(ACCESS_TOKEN_KEY);
}

export function getAuthCache<T>(key: BasicKeys) {
  return Persistent.getSession(key) as T;
}

export function setAuthCache(key: BasicKeys, value: any) {
  return Persistent.setSession(key, value, false);
}

export function clearAuthCache(immediate = true) {
  return Persistent.clearSession(immediate);
}

export const formatToken = (token: string): string => {
  return token;
};
