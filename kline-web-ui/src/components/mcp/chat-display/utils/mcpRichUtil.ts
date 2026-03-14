export interface UrlMatch {
  url: string
  fullMatch: string
  index: number
  isImage: boolean
  isProcessed: boolean
}

export interface DisplaySegment {
  type: 'text' | 'url' | 'image' | 'link' | 'error'
  content: string
  url?: string
  key: string
}

export function safeCreateUrl(url: string): URL | null {
  try {
    if (url.startsWith('http://')) {
      url = url.replace('http://', 'https://')
    }
    return new URL(url)
  } catch {
    if (!url.startsWith('https://')) {
      try {
        return new URL(`https://${url}`)
      } catch {
        return null
      }
    }
    return null
  }
}

export function isUrl(str: string): boolean {
  return safeCreateUrl(str) !== null
}

export function getSafeHostname(url: string): string {
  try {
    const urlObj = safeCreateUrl(url)
    return urlObj ? urlObj.hostname : 'unknown-host'
  } catch {
    return 'unknown-host'
  }
}

export function isLocalhostUrl(url: string): boolean {
  try {
    const hostname = getSafeHostname(url)
    return (
      hostname === 'localhost' ||
      hostname === '127.0.0.1' ||
      hostname === '0.0.0.0' ||
      hostname.startsWith('192.168.') ||
      hostname.startsWith('10.') ||
      hostname.endsWith('.local')
    )
  } catch {
    return false
  }
}

export function normalizeRelativeUrl(relativeUrl: string, baseUrl: string): string {
  if (
    relativeUrl.startsWith('http://') ||
    relativeUrl.startsWith('https://') ||
    relativeUrl.startsWith('data:')
  ) {
    return relativeUrl
  }
  try {
    const baseUrlObj = safeCreateUrl(baseUrl)
    if (!baseUrlObj) return relativeUrl
    if (relativeUrl.startsWith('//')) {
      return `${baseUrlObj.protocol}${relativeUrl}`
    }
    if (relativeUrl.startsWith('/')) {
      return `${baseUrlObj.protocol}//${baseUrlObj.host}${relativeUrl}`
    }
    let basePath = baseUrlObj.pathname
    if (!basePath.endsWith('/')) {
      basePath = basePath.substring(0, basePath.lastIndexOf('/') + 1)
    }
    return `${baseUrlObj.protocol}//${baseUrlObj.host}${basePath}${relativeUrl}`
  } catch {
    return relativeUrl
  }
}

export function formatUrlForOpening(url: string): string {
  if (url.startsWith('data:image/')) return url
  const urlObj = safeCreateUrl(url)
  if (urlObj) return urlObj.href
  return 'about:blank'
}

export async function checkIfImageUrl(url: string): Promise<boolean> {
  if (url.startsWith('data:image/')) return true
  return false
}

export function extractUrlsFromText(text: string, maxUrls = 50): UrlMatch[] {
  const matches: UrlMatch[] = []
  const urlRegex = /(?:https?:\/\/|data:image)[^\s<>"']+/g
  let urlMatch: RegExpExecArray | null
  let urlCount = 0
  while ((urlMatch = urlRegex.exec(text)) !== null && urlCount < maxUrls) {
    const url = urlMatch[0]
    if (!isUrl(url) || isLocalhostUrl(url)) continue
    matches.push({
      url,
      fullMatch: url,
      index: urlMatch.index,
      isImage: false,
      isProcessed: false,
    })
    urlCount++
  }
  return matches.sort((a, b) => a.index - b.index)
}

export async function processUrlTypes(
  matches: UrlMatch[],
  onProgress: (updated: UrlMatch[]) => void,
  cancellationToken: { cancelled: boolean }
): Promise<void> {
  for (let i = 0; i < matches.length; i++) {
    if (matches[i].isProcessed || cancellationToken.cancelled) continue
    const match = matches[i]
    try {
      match.isImage = await checkIfImageUrl(match.url)
      match.isProcessed = true
      onProgress([...matches])
    } catch {
      match.isProcessed = true
      if (!cancellationToken.cancelled) onProgress([...matches])
    }
    if (!cancellationToken.cancelled && i < matches.length - 1) {
      await new Promise((r) => setTimeout(r, 100))
    }
  }
}

export function processResponseUrls(
  text: string,
  maxUrls: number,
  onMatchesFound: (matches: UrlMatch[]) => void,
  onMatchesUpdated: (matches: UrlMatch[]) => void,
  onError: (error: string) => void
): () => void {
  const cancellationToken = { cancelled: false }
  const run = async () => {
    try {
      const matches = extractUrlsFromText(text, maxUrls)
      onMatchesFound(matches)
      await processUrlTypes(matches, onMatchesUpdated, cancellationToken)
    } catch {
      onError('Failed to process response content. Switch to plain text mode to view safely.')
    }
  }
  run()
  return () => {
    cancellationToken.cancelled = true
  }
}

export function buildDisplaySegments(
  responseText: string,
  urlMatches: UrlMatch[]
): DisplaySegment[] {
  const segments: DisplaySegment[] = []
  let lastIndex = 0
  let segmentIndex = 0
  if (urlMatches.length === 0) {
    return [{ type: 'text', content: responseText, key: 'segment-0' }]
  }
  for (let i = 0; i < urlMatches.length; i++) {
    const match = urlMatches[i]
    const { url, fullMatch, index } = match
    if (index > lastIndex) {
      segments.push({
        type: 'text',
        content: responseText.substring(lastIndex, index),
        key: `segment-${segmentIndex++}`,
      })
    }
    segments.push({ type: 'url', content: fullMatch, key: `url-${segmentIndex++}` })
    if (match.isImage) {
      segments.push({
        type: 'image',
        content: url,
        url: formatUrlForOpening(url),
        key: `embed-image-${url}-${segmentIndex++}`,
      })
    } else if (match.isProcessed && !isLocalhostUrl(url)) {
      segments.push({
        type: 'link',
        content: url,
        url: formatUrlForOpening(url),
        key: `embed-${url}-${segmentIndex++}`,
      })
    }
    lastIndex = index + fullMatch.length
  }
  if (lastIndex < responseText.length) {
    segments.push({
      type: 'text',
      content: responseText.substring(lastIndex),
      key: `segment-${segmentIndex++}`,
    })
  }
  return segments
}
