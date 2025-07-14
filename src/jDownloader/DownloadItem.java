package jDownloader;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadItem {
	private final String id;
	private final String url;
	private final File savePath;
	private final File tempSavePath;
	private String fileName;
	private long totalFileSize;
	private final AtomicLong downloadedSize = new AtomicLong(0);
	private volatile DownloadStatus status;
	private String statusMessage;
	private boolean supportsRange;
	
	public DownloadItem(String url, File savePath) {
		this.id = UUID.randomUUID().toString();
		this.url = url;
		this.savePath = savePath;
		this.tempSavePath = new File(savePath.getAbsolutePath() + ".downloading");
		this.status = DownloadStatus.PENDING;
		this.fileName = savePath.getName();
	}

	public DownloadStatus getStatus() {
		return status;
	}

	public void setStatus(DownloadStatus status) {
		this.status = status;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public boolean isSupportsRange() {
		return supportsRange;
	}

	public void setSupportsRange(boolean supportsRange) {
		this.supportsRange = supportsRange;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public File getSavePath() {
		return savePath;
	}

	public File getTempSavePath() {
		return tempSavePath;
	}

	public String getFileName() {
		return fileName;
	}

	public long getTotalFileSize() {
		return totalFileSize;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setTotalFileSize(long totalFileSize) {
		this.totalFileSize = totalFileSize;
	}

	public Long getDownloadedSize() {
		return downloadedSize.get();
	}
	
	public long addDownloadSize(long bytes) {
		return this.downloadedSize.addAndGet(bytes);
	}
	
	public int getProgress() {
		if(totalFileSize <= 0) {
			return 0;
		}
		return (int) ((getDownloadedSize() * 100) / totalFileSize);
	}
	
	
}
