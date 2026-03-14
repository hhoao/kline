declare type Recordable<T = any> = Record<string, T>;

interface ViteEnv {
  VITE_GLOB_APP_TITLE: string;
  VITE_GLOB_API_URL: string;
  VITE_GLOB_API_URL_PREFIX?: string;
  VITE_PUBLIC_PATH: string;
  VITE_PORT?: number;
  VITE_PROXY?: any;
}
