/**
 * File Service API
 * 文件相关服务
 */
import { FileSearchRequest, FileSearchResults, GitCommits, RelativePaths, RelativePathsRequest } from '@/shared/proto/cline/file'
import { defHttp } from '@/utils/http/axios'

export const fileService = {
  async copyToClipboard(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/file/copy-to-clipboard',
      method: 'POST',
      data: { value: request }
    })
  },

  async openFile(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/file/open',
      method: 'POST',
      data: { value: request }
    })
  },

  async openImage(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/file/open-image',
      method: 'POST',
      data: { value: request }
    })
  },

  async openMention(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/file/open-mention',
      method: 'POST',
      data: { value: request }
    })
  },

  async searchCommits(request: string): Promise<GitCommits> {
    return await defHttp.post<GitCommits>({
      url: '/api/cline/file/search-commits',
      method: 'POST',
      data: { value: request }
    })
  },

  async selectFiles(request: boolean): Promise<string[]> {
    return await defHttp.post<string[]>({
      url: '/api/cline/file/select-files',
      method: 'POST',
      data: { value: request }
    })
  },

  async getRelativePaths(request: RelativePathsRequest): Promise<RelativePaths> {
    return await defHttp.post<RelativePaths>({
      url: '/api/cline/file/relative-paths',
      method: 'POST',
      data: request
    })
  },

  async searchFiles(request: FileSearchRequest): Promise<FileSearchResults> {
    return await defHttp.post<FileSearchResults>({
      url: '/api/cline/file/search-files',
      method: 'POST',
      data: request
    })
  },

  async openDiskConversationHistory(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/file/open-disk-conversation-history',
      method: 'POST',
      data: { value: request }
    })
  },

  async openFocusChainFile(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/file/open-focus-chain-file',
      method: 'POST',
      data: { value: request }
    })
  }
}
