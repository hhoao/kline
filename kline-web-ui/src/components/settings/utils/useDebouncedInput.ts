import { ref, watch } from 'vue'

export function useDebouncedInput<T>(
  initialValue: T,
  onChange: (value: T) => void,
  debounceMs = 100
) {
  const localValue = ref(initialValue) as { value: T }
  let timer: ReturnType<typeof setTimeout> | null = null

  watch(localValue, (v) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      onChange(v)
      timer = null
    }, debounceMs)
  }, { deep: true })

  return [localValue, (v: T) => { localValue.value = v }] as const
}
