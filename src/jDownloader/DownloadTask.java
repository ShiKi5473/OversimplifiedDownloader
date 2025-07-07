package jDownloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class DownloadTask extends SwingWorker<Void, Integer>{
	private String url;
	private int progress = 0;
	private JDownloadUI ui;
	private long MEMORY_THRESHOLD = 50 * 1024 * 1024, totalFileSize;
	private File savePath;
	
	public DownloadTask(String url, JDownloadUI ui, long totalFileSize, File saveLocation) {
		this.url = url;
		this.ui = ui;
		this.totalFileSize = totalFileSize;
		this.savePath = saveLocation;
	}

	@Override
	protected Void doInBackground() throws Exception {
		URLConnection conn = null;
		File tempFile = null;
		try {
			URL address = new URL(url);
			conn = address.openConnection();
			if(conn instanceof HttpURLConnection) {
				HttpURLConnection httpconn = (HttpURLConnection) conn;
				httpconn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
				int responseCode = -1;
				responseCode = httpconn.getResponseCode();
				if(responseCode == HttpsURLConnection.HTTP_OK) {
//					totalFileSize = httpconn.getContentLengthLong();
					System.out.printf("檔案總大小%dKB\n", (totalFileSize/1024));
//					tempFile = File.createTempFile("download-", ".tmp");
//					tempFile.deleteOnExit();
//					publicStatus("請選擇儲存位置...");
//					savePath = askForSaveLocation();
//					
//					if (savePath == null) {
//						throw new Exception("使用者未選擇儲存位置，已取消下載");
//					}
					saveFile(httpconn);
					

				}else {
					System.out.println("Connect Failed");
					throw new Exception("伺服器回應錯誤: " + responseCode);
				}

			}
			
		} catch (Exception e) {
            if (savePath != null && savePath.exists()) {
                savePath.delete();
            }
			throw e;
		}finally {
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
		}
		return null;
	}
	@Override
	protected void process(List<Integer> chunks) {
		Integer downloadedKB = chunks.get(chunks.size()-1);
		ui.updateStatus(String.format("已下載%dKB/%dKB", downloadedKB, (totalFileSize/1024)));	
	}
	@Override
	protected void done() {
		try {
			get();
			ui.updateStatus("檔案已成功儲存！");
			JOptionPane.showMessageDialog(ui, "已成功儲存到：\n" + savePath.getAbsolutePath());
		} catch (Exception e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			ui.updateStatus("下載失敗");
			JOptionPane.showMessageDialog(ui, cause.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}finally {
			ui.setUIEnabled(true);
		}
	}
	
	private void publicStatus(String status) {
		SwingUtilities.invokeLater(() -> ui.updateStatus(status));
	}
	
//	private File askForSaveLocation() throws Exception{	
//		File result;
//		JFileChooser fileChooser = ui.createFileChooser();
//		int userSelection = fileChooser.showSaveDialog(ui);
//		if(userSelection == JFileChooser.APPROVE_OPTION) {
//			result = fileChooser.getSelectedFile();
//			
//		}else {
//			result = null;
//		}
//		return result;
//	}
	
	private void saveFile(HttpURLConnection httpconn) {
		try (InputStream inputStream = httpconn.getInputStream();
				FileOutputStream fos = new FileOutputStream(savePath)){
			long downloadByte = 0;
			byte[] buffer = new byte[1024*8];
			int bytesRead;
			while((bytesRead = inputStream.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
				downloadByte += bytesRead;
				if(totalFileSize > 0) {
					progress = (int) ((downloadByte * 100) / totalFileSize);
//					System.out.printf("Progress: %d %\n", progress);
					setProgress(progress);
				}
				publish((int)(downloadByte / 1024));
			}
			System.out.println("下載完成");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	


}
