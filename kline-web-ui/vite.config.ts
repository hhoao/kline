import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { ConfigEnv, loadEnv, UserConfig } from "vite";
import { wrapperEnv } from "./internal/utils";
import { createVitePlugins } from "./internal/vite/plugin";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function pathResolve(dir: string) {
  return resolve(process.cwd(), ".", dir);
}

export default ({ command, mode }: ConfigEnv): UserConfig => {
  const root = process.cwd();

  const env = loadEnv(mode, root);

  // The boolean type read by loadEnv is a string. This function can be converted to boolean type
  const viteEnv = wrapperEnv(env);

  const { VITE_PUBLIC_PATH } =
    viteEnv;

  const isBuild = command === 'build';

  return {
    plugins: createVitePlugins(viteEnv, isBuild),
    base: VITE_PUBLIC_PATH,
    build: {
      outDir: resolve(__dirname, "dist"),
      assetsDir: "assets",
      emptyOutDir: true,
    },
    server: {
      port: 3000,
      proxy: {
        "^/ai/": {
          target: "http://localhost:8080",
          changeOrigin: true,
          secure: false,
        },
      },
    },
    resolve: {
      alias: [
        // @/xxxx => src/xxxx
        {
          find: /@\//,
          replacement: pathResolve("src") + "/",
        },
        // /#/xxxx => types/xxxx
        {
          find: /\/#\//,
          replacement: pathResolve("types") + "/",
        },
      ],
    },
  };
};
