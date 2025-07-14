package jDownloader;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager {
	private final ExecutorService executorService;
	private final Map<String, Future<?>> runningTask = new ConcurrentHashMap<>();
	private final Map<String, DownloaderListener> listeners = new ConcurrentHashMap<>();
	private final ConnectChecker connectChecker;
	
	public DownloadManager() {
		this.executorService = Executors.newCachedThreadPool();
		this.connectChecker = new ConnectChecker();
	}
	
	public void startDownload(String url, File savePath, DownloaderListener listener) {
		DownloadItem item = new DownloadItem(url, savePath);
		listeners.put(item.getId(), listener);
		
		Future<?> future = executorService.submit(() -> {
			try {
				updateStatus(item, DownloadStatus.CHECKING, "正在檢查連線...");
				ConnectChecker.DownloadInfo info = connectChecker.check(url);
				
				item.setTotalFileSize(info.fileSize);
				item.setSupportsRange(info.supportsRange);
				
				if(item.getSavePath().getName().isEmpty()) {
					item.setFileName(info.suggestFileName);
				}
				
				if(item.getTempSavePath().exists()) {
					item.getTempSavePath().delete();
				}
				
				Runnable downloadTask;
				if(item.isSupportsRange() && item.getTotalFileSize() > 0) {
					updateStatus(item, DownloadStatus.DOWNLOADING, "伺服器支援分塊下載，啟用多執行緒模式。");
					downloadTask = new MultiThreadDownloadTask(item, createInternalListener(item));
				}else {
					updateStatus(item, DownloadStatus.DOWNLOADING, "伺服器不支援分塊下載，使用單執行緒模式。");
                    downloadTask = new DownloadTask(item, createInternalListener(item));
				}
				
			} catch (Exception e) {
				handleError(item, e);
			}
		})
	}
	private void updateStatus(DownloadItem item, DownloadStatus status, String message) {
		item.setStatus(status);
		item.setStatusMessage(message);
		DownloaderListener listener = listeners.get(item.getId());
		if(listener != null) {
			listener.onStatusChanged(item);
		}
	}
}
