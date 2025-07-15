package jDownloader;

public interface DownloaderListener {
	void onStatusChanged(DownloadItem item);
	void onProgress(DownloadItem item);
	void onCompleted(DownloadItem item);
	void onError(DownloadItem item, Exception e);
}
