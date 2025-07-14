package jDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class MultiThreadDownloadTask implements Runnable {
	private final DownloadItem item;
	private final DownloaderListener listener;
	private final int THREAD_COUNT = 8;
	private final int MAX_RETRIES = 3;
	private final List<File> tempFiles = new ArrayList<>();
	

	private static final HttpClient client = HttpClient.newBuilder()
			.followRedirects(Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(20))
			.build();
	
	private static class Chunk{
		final long startByte;
		final long endByte;
		final File tempFile;
		int retryCount = 0;
		
		Chunk(long startByte, long endByte, File tempFile){
			this.startByte = startByte;
			this.endByte = endByte;
			this.tempFile = tempFile;
		}
	}
	
	public MultiThreadDownloadTask(DownloadItem item, DownloaderListener listener) {
		this.item = item;
		this.listener = listener;
	}
	
	@Override
	public void run() {
		ExecutorService chunkExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
		try {
			final ConcurrentLinkedQueue<Chunk> chunkQueue = new ConcurrentLinkedQueue<>();
			long chunkSize = item.getTotalFileSize() / THREAD_COUNT;
			for(int i = 0; i < THREAD_COUNT; i++) {
				long startByte = i * chunkSize;
				long endByte = (i == THREAD_COUNT - 1) ? item.getTotalFileSize()-1 : startByte + chunkSize - 1;
				
				File tempFile = File.createTempFile(item.getSavePath().getName() + ".part" + i,  ".tmp");
				tempFiles.add(tempFile);
				chunkQueue.add(new Chunk(startByte, endByte, tempFile));
			}
			
			List<Callable<Void>> workers = new ArrayList<Callable<Void>>();
			for(int i = 0; i < THREAD_COUNT; i++) {
				workers.add(createWorker(chunkQueue));
			}
			
			List<Future<Void>> futures = chunkExecutor.invokeAll(workers);
			for(Future<Void> future : futures) {
				future.get();
			}
			
			combineChunks();
			
			listener.onCompleted(item);
			
		} catch (Exception e) {
			listener.onError(item);
		}finally {
			chunkExecutor.shutdownNow();
			cleanTempFiles();

		}
	}
	
	
	private Callable<Void> createWorker(final ConcurrentLinkedQueue<Chunk> queue){
		return () -> {
			Chunk chunk;
			while((chunk = queue.poll()) != null) {
				try {
					downloadChunk(chunk);
				} catch (Exception e) {
					System.err.println("區塊下載失敗，準備重試。起始位元組: " + chunk.startByte + ". 錯誤: " + e.getMessage());
					if(chunk.retryCount < MAX_RETRIES) {
						chunk.retryCount++;
						queue.add(chunk);
						Thread.sleep(500);
					}else {
						throw new IOException("區塊下載已達最大重試次數，任務失敗。起始位元組: " + chunk.startByte, e);
					}
				}
			}
			return null;
		};
	}
	
	private void downloadChunk(Chunk chunk) throws Exception{
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(item.getUrl()))
				.header("Range", "bytes=" + chunk.startByte + "-" + chunk.endByte)
				.GET()
				.build();
		
		HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		
		if(response.statusCode() != 206) {
			throw new Exception("伺服器回應錯誤 (非 206): " + response.statusCode());
		}
		try (InputStream in = response.body();
				FileOutputStream fos = new FileOutputStream(chunk.tempFile)){
			byte[] buffer = new byte[8*1024];
			int bytesRead;
			while((bytesRead = in.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
				item.addDownloadSize(bytesRead);
				listener.onProgress(item);
				
			}
		}
	}
	
	private void combineChunks() throws Exception{
		item.setStatus(DownloadStatus.MERGING);
		item.setStatusMessage("正在合併檔案...");
		listener.onStatusChanged(item);

		try (FileOutputStream fos = new FileOutputStream(item.getTempSavePath())){
			for(File tempFile: tempFiles) {
				try (FileInputStream fis = new FileInputStream(tempFile);){
					fis.transferTo(fos);
				} 
			}
			
		} finally {
			cleanTempFiles();
		}
	}
	private void cleanTempFiles() {
		for(File tempfile: tempFiles) {
			if(tempfile.exists()) {
				tempfile.delete();
			}
		}
	}
}
