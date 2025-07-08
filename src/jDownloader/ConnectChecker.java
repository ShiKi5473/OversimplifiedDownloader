package jDownloader;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.swing.SwingWorker;

public class ConnectChecker extends SwingWorker<ConnectChecker.DownLoadInfo, Void>{
	private String url;
	private JDownloadUI ui;
	private static final HttpClient client = HttpClient.newBuilder()
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(java.time.Duration.ofSeconds(10))
			.build();
	
	public ConnectChecker(String url, JDownloadUI ui) {
		this.url = url;
		this.ui = ui;
	}
	
	public static class DownLoadInfo{
		long fileSize;
		String contentType;
		String suggestFileName;
		
		public DownLoadInfo(long fileSize, String contentType, String suggestFileName) {
			this.fileSize = fileSize;
			this.contentType = contentType;
			this.suggestFileName = suggestFileName;
		}
	}
	
		@Override
		protected DownLoadInfo doInBackground() throws Exception{
			ui.updateStatus("正在檢查連線並且獲取檔案資訊");
			
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.method("HEAD", HttpRequest.BodyPublishers.noBody())
					.timeout(java.time.Duration.ofSeconds(10))
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
					.build();
			
			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
			
			if(response.statusCode() != 200) {
				throw new Exception("無法取得連線，伺服器回應碼: " + response.statusCode());
			}
			
			long fileSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
			if(fileSize < 0) {
				throw new Exception("無法取得檔案大小，或伺服器不支援。");
			}
			
			String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
			
			String suggestFileNamString = response.headers().firstValue("Content-Disposition")
					.map(s -> this.parseFileName(s))
					.orElseGet(() ->{
						String path = response.uri().getPath();
	                    return path.substring(path.lastIndexOf('/') + 1);
					});
			return new DownLoadInfo(fileSize, contentType, suggestFileNamString);
		}
		@Override
		protected void done() {
			try {
				DownLoadInfo downLoadInfo = get();
				ui.startDownload(downLoadInfo);
			} catch (Exception e) {
				Throwable cause = e.getCause() != null ? e.getCause() : e;
	            ui.updateStatus("連線失敗：" + cause.getMessage());
	            ui.setUIEnabled(true);
			}

		}
		private String parseFileName(String contentDispositionHeader) {
			return Optional.of(contentDispositionHeader)
					.map(it -> it.toLowerCase())
					.filter(it -> it.contains("filename="))
					.map(it -> it.substring(it.indexOf("filename=") + 9)) 
					.map(it -> it.replaceAll("\"", ""))
					.map(it -> it.trim())
					.orElse("");
		}

		
	};

