package jDownloader;

/**
 * 表示下載任務狀態的列舉。
 * 用於清晰地管理和傳遞下載任務的目前狀態。
 */
public enum DownloadStatus {
    /**
     * 等待開始。
     */
    PENDING,
    /**
     * 正在檢查連線並獲取檔案資訊。
     */
    CHECKING,
    /**
     * 正在下載。
     */
    DOWNLOADING,
    /**
     * 正在合併檔案分塊 (僅多執行緒)。
     */
    MERGING,
    /**
     * 已暫停。
     */
    PAUSED,
    /**
     * 下載成功完成。
     */
    COMPLETED,
    /**
     * 使用者取消。
     */
    CANCELLED,
    /**
     * 發生錯誤。
     */
    ERROR
}
