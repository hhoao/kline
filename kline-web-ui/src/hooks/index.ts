import type { GlobConfig } from '../types/config';

import { getAppEnvConfig } from '../utils/env';

export const useGlobSetting = (): Readonly<GlobConfig> => {
  const {
    VITE_GLOB_APP_TITLE,    VITE_GLOB_API_URL,
    VITE_GLOB_API_URL_PREFIX,
  } = getAppEnvConfig();

  // 验证环境变量格式（仅在开发环境）
  if (VITE_GLOB_APP_TITLE && !/[a-zA-Z\_]*/.test(VITE_GLOB_APP_TITLE)) {
    // 使用全局 console 对象，而不是从 console 模块导入
    if (typeof window !== 'undefined' && window.console) {
      window.console.warn(
        `VITE_GLOB_APP_SHORT_NAME Variables can only be characters/underscores, please modify in the environment variables and re-running.`
      );
    }
  }

  // Take global configuration
  const glob: Readonly<GlobConfig> = {
    title: VITE_GLOB_APP_TITLE,
    apiUrl: VITE_GLOB_API_URL,
    urlPrefix: VITE_GLOB_API_URL_PREFIX,
  };
  return glob as Readonly<GlobConfig>;
};
