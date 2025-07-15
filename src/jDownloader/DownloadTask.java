package jDownloader;


import java.io.File;

import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;


public class DownloadTask implements Runnable{
	private DownloadItem item;
	private DownloaderListener listener;
	
	public DownloadTask(DownloadItem item, DownloaderListener listener) {
		this.item = item;
		this.listener = listener;
	}
	
	private static final HttpClient client  = HttpClient.newBuilder()
												.followRedirects(Redirect.NORMAL)
												.connectTimeout(Duration.ofSeconds(20))
												.build();

	
	@Override
	public void run() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(item.getUrl()))
					.GET()
//					.timeout(Duration.ofMinutes(30))
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
					.build();
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			

			if(response.statusCode() == 200) {
				if(item.getTotalFileSize() <= 0) {
					long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
					item.setTotalFileSize(contentLength);
				}
				saveFile(response.body());
			}else {
				throw new Exception("伺服器回應錯誤： " + response.statusCode());
			}
		} catch (Exception e) {
			listener.onError(item, e);
		}

	}

	
	
	private void saveFile(InputStream inputStream) throws Exception {
		try (InputStream is = inputStream;
				FileOutputStream fos = new FileOutputStream(item.getTempSavePath())){
			byte[] buffer = new byte[1024*8];
			int bytesRead;
			while((bytesRead = is.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
				item.addDownloadSize(bytesRead);
				listener.onProgress(item);
			}
			System.out.println("下載完成");
		}
	}
	


}
