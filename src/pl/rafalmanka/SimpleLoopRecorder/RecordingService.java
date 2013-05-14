package pl.rafalmanka.SimpleLoopRecorder;
 
import java.io.File; 
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioRecord;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class RecordingService extends Service {

	private static final String TEMP_FOLDER = Environment
			.getExternalStorageDirectory().getPath() + "/AudioRecorder/temp/";
	private static final String TAG = "RecordingService";
	private int bufferSize = 0;	
	private Thread recordingThread = null;
	private boolean isRecording = false;
	AudioRecord recorder;

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.d(TAG, "onCreated");	
		recorder = ((LoopRecorderApp) getApplication()).getAudioRecorder();
		Log.d(TAG, "recorder set");	
		startRecording();
	}

	private void startRecording() {

		Log.d(TAG, "startRecording");
		Toast.makeText(RecordingService.this, "START recording",
				Toast.LENGTH_LONG).show();
		Log.d(TAG, "Audio recorder created");		
		int i = recorder.getState();
		if (i == 1)
			recorder.startRecording();
		
		Log.d(TAG, "recorder state is " + i);
		isRecording = true;
		recordingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				writeAudioDataToFile();
			}
		}, "AudioRecorder Thread");
		recordingThread.start();
	}

	private void writeAudioDataToFile() {
		FileOutputStream os = null;
		while (isRecording) {
			byte data[] = new byte[bufferSize];
			Log.d(TAG, "bufferSize: " + bufferSize);
			try {
				String filename = getTempFilename();
				os = new FileOutputStream(filename);
				Log.d(TAG, "file output stream created for file: " + filename);
			} catch (FileNotFoundException e) {
				Log.d(TAG, "file does not exist exception");
				// e.printStackTrace();
			}
			int read = 0;
			read = recorder.read(data, 0, bufferSize);
			Log.d(TAG, "chunk of audio have been ccaptured");
			if (AudioRecord.ERROR_INVALID_OPERATION != read) {
				try {
					os.write(data);
					Log.d(TAG, "chunk of audio have been written to file");
				} catch (IOException e) {
					Log.d(TAG, "Exception: no writable SD card");
					// e.printStackTrace();
				}
			}
			Log.d(TAG, "cleaning old files if any");
			cleanOldFiles();
			try {
				Thread.sleep(1000);
				Log.d(TAG, "sleep for a second");
			} catch (InterruptedException e1) {
				Log.e(TAG, "Sleeping has been interrupted exception");
				e1.printStackTrace();
			}
			try {
				os.close();
				Log.d(TAG, "close outputstream");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		cleanDirectory();
		isRecording = true;
	}

	private void cleanDirectory() {
		Log.d(TAG, "cleaning folder: " + TEMP_FOLDER);
		File dir = new File(TEMP_FOLDER);
		File[] files = dir.listFiles();
		for (File file : files) {
			Log.d(TAG, "deleting file: " + file.getName());
			file.delete();
		}
	}

	

	private void cleanOldFiles() {

		while (true) {
			File dir = new File(TEMP_FOLDER);
			File[] files = dir.listFiles();
			Log.d(TAG, "checking directory for old files");
			File lastModifiedFile = files[0];
			Log.d(TAG, "putting them into array");
			Log.d(TAG, "ordering files by date modified");
			for (int i = 1; i < files.length; i++) {
				if (lastModifiedFile.lastModified() > files[i].lastModified()) {
					lastModifiedFile = files[i];
				}
			}

			if (lastModifiedFile.lastModified() < (System.currentTimeMillis() - 300000)) {
				Log.d(TAG,
						"deleting old file: "
								+ lastModifiedFile.getAbsolutePath());
				if (!lastModifiedFile.exists()) {
					Log.d(TAG, "file does not exist");
				} else {
					boolean deleted = lastModifiedFile.delete();
					Log.d(TAG, "file deleted=" + deleted);
				}

			} else {
				Log.d(TAG, "no old files found");
				break;
			}
		}
	}

	private String getTempFilename() {
		File file = new File(TEMP_FOLDER);
		Log.d(TAG, "absolute path of file: " + file.getAbsolutePath());
		if (!file.exists()) {
			file.mkdirs();
		}
		String filename = TEMP_FOLDER + "/" + System.currentTimeMillis()
				+ ".wav";

		return filename;
	}

	

	@Override
	public void onDestroy() {

		super.onDestroy();

		Toast.makeText(RecordingService.this, "STOP recording",
				Toast.LENGTH_LONG).show();

		Log.d(TAG, "stopRecording");

		isRecording = false;
		recorder.release();
		Log.d(TAG, "recorder released ");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
