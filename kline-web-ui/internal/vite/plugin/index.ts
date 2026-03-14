import { PluginOption } from 'vite';
import vue from '@vitejs/plugin-vue';
import { configHtmlPlugin } from './html';
import DevTools from 'vite-plugin-vue-devtools';
import presetIcons from '@unocss/preset-icons';
import UnoCSS from 'unocss/vite';
import { presetWind3 } from 'unocss';

export function createVitePlugins(viteEnv: ViteEnv, isBuild: boolean) {
  const vitePlugins: (PluginOption | PluginOption[])[] = [
    vue(),
    DevTools(),
    UnoCSS({
      presets: [
        presetWind3(),
        presetIcons(),
      ],
      rules: [['m-1', { margin: '1px' }]],
      theme: {
        boxShadow: {
          xl: [
            'var(--un-shadow-inset) 0px 0px 8px var(--un-shadow-color, rgb(0 0 0 / 0.1))',
            'var(--un-shadow-inset) 0px 0px 8px var(--un-shadow-color, rgb(0 0 0 / 0.1))',
          ].join(', '),
        },
      },
    }),
  ];


  // vite-plugin-html
  vitePlugins.push(configHtmlPlugin(viteEnv, isBuild));

  return vitePlugins;
}
