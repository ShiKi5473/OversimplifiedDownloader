package jDownloader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.SwingWorker;

public class ConnectChecker extends SwingWorker<Long, Void>{
	private String url;
	private JDownloadUI ui;
//	private long totalFileSize;
	public ConnectChecker(String url, JDownloadUI ui) {
		this.url = url;
		this.ui = ui;
	}
		@Override
		protected Long doInBackground() throws Exception{
			URL address = new URL(url);
			URLConnection conn = address.openConnection();
			if(conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				httpConn.setRequestMethod("HEAD");
				httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
				
				long fileSize = httpConn.getContentLengthLong();
				httpConn.disconnect();
				if(fileSize < 0) {
					throw new Exception("無法取得連線，或是檔案不存在");
				}
				return fileSize;
			}else {
				throw new Exception("不支援的協定，僅支援 HTTP 和 HTTPS。");
			}
		}
		@Override
		protected void done() {
			try {
				long totalFileSize = get();
				ui.startDownload(totalFileSize); 
			} catch (Exception e) {
				Throwable cause = e.getCause() != null ? e.getCause():e;
				ui.updateStatus("連線失敗：" + cause.getMessage());
				ui.setUIEnabled(true);
			}

		}
		
	};

