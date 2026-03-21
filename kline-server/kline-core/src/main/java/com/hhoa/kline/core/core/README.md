# Core Architecture

entry point -> controller -> task

```tree
core/
├── controller/   # Handles webview messages and task management
├── task/         # Executes API requests and tool operations
└── ...           # Additional components to help with context, parsing user/assistant messages, etc.
```
