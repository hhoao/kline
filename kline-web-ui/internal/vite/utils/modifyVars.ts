import { resolve } from 'node:path';

/**
 * less global variable
 */
export function generateModifyVars() {
  const primaryColorObj: Record<string, string> = {};

  // const modifyVars = getThemeVariables();
  return {
    // reference:  Avoid repeated references
    hack: `true; @import (reference) "${resolve('src/design/config.sass')}";`,
    ...primaryColorObj,
    'success-color': '#55D187', //  Success color
    'error-color': '#ED6F6F', //  False color
    'warning-color': '#EFBD47', //   Warning color
    'font-size-base': '14px', //  Main font size
    'border-radius-base': '2px', //  Component/float fillet
  };
}
