# Windows (Desktop App)

Desktop app container for admin/operator workflows on Windows.

## Recommendation
- Prefer **Tauri** for lightweight bundle and better memory usage.
- Keep **Electron** as fallback if native Node APIs are required.

## Initial structure
- `tauri/`: Rust + webview shell
- `electron/`: Node + Chromium shell

