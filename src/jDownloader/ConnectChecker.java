package jDownloader;


import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;

import java.net.http.HttpResponse;
import java.util.Optional;


public class ConnectChecker {
	private static final HttpClient client = HttpClient.newBuilder()
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(java.time.Duration.ofSeconds(10))
			.build();

	
	public static class DownloadInfo{
		long fileSize;
		String contentType;
		String suggestFileName;
		boolean supportsRange;
		
		public DownloadInfo(long fileSize, String contentType, String suggestFileName, Boolean supportsRange) {
			this.fileSize = fileSize;
			this.contentType = contentType;
			this.suggestFileName = suggestFileName;
			this.supportsRange = supportsRange;
		}
	}
	
	public DownloadInfo check(String url) throws Exception{
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.method("HEAD", HttpRequest.BodyPublishers.noBody())
				.timeout(java.time.Duration.ofSeconds(10))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
				.build();
		
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
		

		if(response.statusCode() == 200) {
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
			
			Boolean supportsRange = response.headers()
					.firstValue("Accept-Ranges")
					.map(val -> val.equalsIgnoreCase("bytes"))
					.orElse(false);
			System.out.println("伺服器支援 Range 請求: " + supportsRange);
			
			return new DownloadInfo(fileSize, contentType, suggestFileNamString, supportsRange);
		}else if(response.statusCode() == 405) {
			System.out.print("伺服器不支援 HEAD (回應 405)，切換到備用模式取得下載資料。");
//			return create
			return null;
		}else{
			throw new Exception("無法取得連線，伺服器回應碼: " + response.statusCode());
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

