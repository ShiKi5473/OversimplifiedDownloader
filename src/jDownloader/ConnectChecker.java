package jDownloader;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConnectChecker {
	private static final HttpClient client = HttpClient.newBuilder()
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(java.time.Duration.ofSeconds(10))
			.build();
	
	private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile("bytes \\d+-\\d+/(//d+)");
	
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
		try {
			System.out.println("策略 1: 嘗試發送小範圍 GET 請求 (bytes=0-255)...");
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("RANGE", "bytes = 0-255")
					.GET()
					.timeout(java.time.Duration.ofSeconds(15))
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
					.build();
			
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			
			try(InputStream is = response.body()){
			}
			
			if(response.statusCode() == 206) {
				System.out.println("策略 1 成功：伺服器回應 206，支援斷點續傳。");
				long fileSize = parseTotalSize(response.headers().firstValue("Content-Range")); 
				
				if(fileSize <= 0) {
					throw new IOException("從 Content-Range 標頭中無法解析有效的檔案總大小。");
				}
				
				String contentType= response.headers().firstValue("Content-Type").orElse("application/octet-stream");
				String suggestFileName = parseFileNameFromHeader(response);
				
				return new DownloadInfo(fileSize, contentType, suggestFileName, true);
			}else {
				throw new IOException("伺服器對範圍請求的回應不是 206，而是 " + response.statusCode());
			}
			
		} catch (Exception e) {
			System.err.println("策略 1 失敗 (" + e.getMessage() + ")。切換至策略 2: 常規 GET 請求後備方案...");
			return checkWithGetRequestFallback(url);
		}
		
		
	
//		HttpRequest request = HttpRequest.newBuilder()
//				.uri(URI.create(url))
//				.method("HEAD", HttpRequest.BodyPublishers.noBody())
//				.timeout(java.time.Duration.ofSeconds(10))
//				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
//				.build();
//		
//		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
//		
//
//		if(response.statusCode() == 200) {
//			long fileSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
//			if(fileSize < 0) {
//				throw new Exception("無法取得檔案大小，或伺服器不支援。");
//			}
//			
//			String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
//			
//			String suggestFileNamString = response.headers().firstValue("Content-Disposition")
//					.map(s -> this.parseFileName(s))
//					.orElseGet(() ->{
//						String path = response.uri().getPath();
//	                    return path.substring(path.lastIndexOf('/') + 1);
//					});
//			
//			Boolean supportsRange = response.headers()
//					.firstValue("Accept-Ranges")
//					.map(val -> val.equalsIgnoreCase("bytes"))
//					.orElse(false);
//			System.out.println("伺服器支援 Range 請求: " + supportsRange);
//			
//			return new DownloadInfo(fileSize, contentType, suggestFileNamString, supportsRange);
//		}else if(response.statusCode() == 405) {
//			System.out.print("伺服器不支援 HEAD (回應 405)，切換到備用模式取得下載資料。");
//			return create
//			return null;
//		}else{
//			throw new Exception("無法取得連線，伺服器回應碼: " + response.statusCode());
//		}
	}
	
	private DownloadInfo checkWithGetRequestFallback(String url) throws Exception{
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.timeout(java.time.Duration.ofSeconds(15))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
				.build();
		
		HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
		
		try(InputStream is = response.body()){
			
		}
		
		if(response.statusCode() == 200) {
			System.out.println("策略 2 成功：GET 請求後備方案回應 200。");
			long fileSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
			
			if (fileSize < 0) {
				throw new IOException("後備GET方案無法從 Content-Length 標頭中取得有效的檔案大小。");
			}
			
			boolean supportRange = response.headers()
					.firstValue("Accept-Ranges")
					.map(val -> val.equalsIgnoreCase("bytes"))
					.orElse(false);
			
			System.out.println("伺服器 'Accept-Ranges' 標頭顯示支援斷點續傳: " + supportRange);
			
			String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
			String suggestFileName = parseFileNameFromHeader(response);
			
			return new DownloadInfo(fileSize, contentType, suggestFileName, supportRange);
		}else {
			throw new Exception("後備方案 GET 請求失敗，伺服器回應碼: " + response.statusCode());
		}
	}
	
	private long parseTotalSize(Optional<String> contentRange) {
		if(contentRange.isEmpty()) {
			return -1;
		}
		Matcher matcher = CONTENT_RANGE_PATTERN.matcher(contentRange.get());
		if(matcher.find()) {
			return Long.parseLong(matcher.group(1));
		}
		return -1;
	}
	
	private String parseFileNameFromHeader(HttpResponse<?> response) {
		return response.headers().firstValue("Content-Disposition")
				.map(this::parseFileName)
				.orElseGet(() ->{
					String path = response.uri().getPath();
					if((path == null || path.isEmpty() || !path.contains("/"))) {
						return "download_file";
					}
					return path.substring(path.lastIndexOf('/' + 1)); 
				});

		
	};
	
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

