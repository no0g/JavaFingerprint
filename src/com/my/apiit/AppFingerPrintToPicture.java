package com.my.apiit;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.digitalpersona.onetouch.DPFPFingerIndex;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.DPFPCapturePriority;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPDataListener;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;


public class AppFingerPrintToPicture {
	public static void main(String[] args) throws IOException {
		
		AppFingerPrintToPicture fingerPrintToImage = new AppFingerPrintToPicture();

		//-- register bio metric reader;
		String readerId = fingerPrintToImage.selectReader();
		
		//-- convert Finger print to Awt Image format		
		Image data = fingerPrintToImage.convertFingerPrintToImage(readerId);
		
		//--- write Image to file
		fingerPrintToImage.writeToFile(data,"E:\\Users\\REZA\\Documents\\BIOM\\lab 3 BIOM\\output.png");
	}
	
	public String selectReader() throws IndexOutOfBoundsException {
		DPFPReadersCollection readers = DPFPGlobal.getReadersFactory().getReaders();
		if (readers == null || readers.size() == 0)
			throw new IndexOutOfBoundsException("There are no readers available");

		int res = 0; // is default reader 
		return readers.get(res).getSerialNumber();            
	}
	private Image convertFingerPrintToImage(String activeReader) {
		System.out.printf("Performing fingerprint enrollment...\n");
		try {
			DPFPFingerIndex finger = DPFPFingerIndex.values()[1];

			DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
			DPFPEnrollment enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();

			while (enrollment.getFeaturesNeeded() > 0)
			{
				DPFPSample sample = getSample(activeReader, 
						String.format("Scan your finger (%d remaining)\n",  enrollment.getFeaturesNeeded()));
				if (sample == null)
					continue;

				return convertSampleToBitmap(sample);
			}			
		} catch(Exception e){
			throw new RuntimeException(e);
		}
		return null;            
	}
	private DPFPSample getSample(String activeReader, String prompt)
			throws InterruptedException
	{
		final LinkedBlockingQueue<DPFPSample> samples = new LinkedBlockingQueue<DPFPSample>();
		DPFPCapture capture = DPFPGlobal.getCaptureFactory().createCapture();
		capture.setReaderSerialNumber(activeReader);
		capture.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_LOW);
		capture.addDataListener(new DPFPDataListener()
		{
			public void dataAcquired(DPFPDataEvent e) {
				if (e != null && e.getSample() != null) {
					try {
						samples.put(e.getSample());
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		capture.addReaderStatusListener(new DPFPReaderStatusAdapter()
		{
			int lastStatus = DPFPReaderStatusEvent.READER_CONNECTED;
			public void readerConnected(DPFPReaderStatusEvent e) {
				if (lastStatus != e.getReaderStatus())
					System.out.println("Reader is connected");
				lastStatus = e.getReaderStatus();
			}
			public void readerDisconnected(DPFPReaderStatusEvent e) {
				if (lastStatus != e.getReaderStatus())
					System.out.println("Reader is disconnected");
				lastStatus = e.getReaderStatus();
			}

		});
		try {
			capture.startCapture();
			System.out.print(prompt);
			return samples.take();
		} catch (RuntimeException e) {
			System.out.printf("Failed to start capture. Check that reader is not used by another application.\n");
			throw e;
		} finally {
			capture.stopCapture();
		}
	}

	private String ShowDialog(String prompt) {
		System.out.printf(prompt);
		try {
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			return stdin.readLine();
		} catch (IOException e) {
			return "";
		}
	}
	protected Image convertSampleToBitmap(DPFPSample sample) {
		return DPFPGlobal.getSampleConversionFactory().createImage(sample);
	}
	

	private void writeToFile(Image img, String filePath){
		try{
			BufferedImage bi = (BufferedImage)img;
			File f = new File(filePath);
			ImageIO.write(bi, "png", f);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	
}
