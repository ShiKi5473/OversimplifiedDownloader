package jDownloader;

import java.awt.BorderLayout;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;


import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;



public class JDownloadUI extends JFrame implements DownloaderListener{

	private JTextField  inputURL;
	private JButton getButton;
	private JProgressBar progressBar;
	private JLabel statusLabel;
	private DownloadManager downloadManager;
	

	
	public JDownloadUI() {
		setLayout(new BorderLayout(10, 10));
		JPanel contentPanel = (JPanel) getContentPane();
		contentPanel.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel top = new JPanel(new BorderLayout(5,5));

		
		inputURL = new JTextField(100);
		getButton = new JButton("Get");
		
		JPanel center = new JPanel(new BorderLayout());
		
		
//		get.setPreferredSize(new Dimension(100, 30));
		top.add(new JLabel("Enter URL："), BorderLayout.WEST);
		top.add(inputURL, BorderLayout.CENTER);
		top.add(getButton, BorderLayout.EAST);
		contentPanel.add(top, BorderLayout.NORTH);

		JPanel bottom = new JPanel(new BorderLayout(5,5));
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		statusLabel = new JLabel("請輸入url並開始下載");
		statusLabel.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
		bottom.add(progressBar, BorderLayout.CENTER);
		bottom.add(statusLabel, BorderLayout.SOUTH);
//		add(bottom, BorderLayout.CENTER);
//		add(top, BorderLayout.NORTH);
//		add(center, BorderLayout.CENTER);
		contentPanel.add(bottom, BorderLayout.CENTER);

		getButton.addActionListener(e -> handleDownloadRequest());
		
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowevent) {
				downloadManager.shutdown();
				System.exit(0);
			}
		});
		
		pack();
//		setSize(1280, 720);
		setVisible(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		

		
		

	}
	
	private void handleDownloadRequest() {
		String url = inputURL.getText().trim();
		if(url.isEmpty()) {
			JOptionPane.showMessageDialog(this,  "請輸入有效的 URL", "錯誤", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		 JFileChooser fileChooser = new JFileChooser();
	        fileChooser.setDialogTitle("儲存檔案");

	        String suggestedName = url.substring(url.lastIndexOf('/') + 1);
	        if (!suggestedName.isEmpty()) {
	            fileChooser.setSelectedFile(new File(suggestedName));
	        }

	        int userSelection = fileChooser.showSaveDialog(this);
	        if (userSelection == JFileChooser.APPROVE_OPTION) {
	            File saveLocation = fileChooser.getSelectedFile();

	            if (saveLocation.exists()) {
	                int response = JOptionPane.showConfirmDialog(this, "檔案已存在，是否覆蓋？", "確認儲存", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
	                if (response != JOptionPane.YES_OPTION) {
	                    updateStatusLabel("使用者取消操作");
	                    return;
	                }
	            }
	            

	            setUIEnabled(false);
	            progressBar.setValue(0);
	            downloadManager.startDownload(url, saveLocation, this);
	        } else {
	            updateStatusLabel("使用者取消操作");
	        }
		
	}
	

	

	
	public void updateStatusLabel(String text) {
		SwingUtilities.invokeLater(() -> statusLabel.setText(text));
	}
    public void setUIEnabled(boolean enabled) {
        getButton.setEnabled(enabled);
        inputURL.setEditable(enabled);
    }
    
    

	public static void main(String[] args) {
		 javax.swing.SwingUtilities.invokeLater(JDownloadUI::new);

	}

	@Override
	public void onStatusChanged(DownloadItem item) {
		updateStatusLabel(item.getStatusMessage());
		
	}

	@Override
	public void onProgress(DownloadItem item) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(item.getProgress());
			String statusText = String.format("已下載 %d KB / %d KB", 
					item.getDownloadedSize()/1024, 
					item.getTotalFileSize()/1024);
			statusLabel.setText(statusText);
			
		});
		
	}

	@Override
	public void onCompleted(DownloadItem item) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(100);
			updateStatusLabel("下載完成");
			JOptionPane.showMessageDialog(this, "已成功儲存到：\n" + item.getSavePath().getAbsolutePath());
			setUIEnabled(true);
		});
		
	}

	@Override
	public void onError(DownloadItem item, Exception e) {
		SwingUtilities.invokeLater(() -> {
			String errorMessage = "錯誤" + e.getMessage();
            updateStatusLabel(errorMessage);
            JOptionPane.showMessageDialog(this, errorMessage, "下載失敗", JOptionPane.ERROR_MESSAGE);
            setUIEnabled(true);
            e.printStackTrace();
		});
		
	}
	

}
