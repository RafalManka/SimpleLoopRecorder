package pl.rafalmanka.SimpleLoopRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class RecordingService extends Service {

	private static final String TEMP_FOLDER = Environment
			.getExternalStorageDirectory().getPath() + "/AudioRecorder/temp/";
	private AudioRecord recorder = null;
	private static final String TAG = "RecordingService";
	private int bufferSize = 0;
	private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
	private static short[] audioFormatEncoding = new short[] {
			AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT };
	private static short[] audioFormatChannel = new short[] {
			AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
	private Thread recordingThread = null;
	private boolean isRecording = false;

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.d(TAG, "onCreated");
		if(recorder==null){
			Log.d(TAG, "audio session: ");
			getProperFormats();
		}

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
		Log.d(TAG, "recorder state is " + recorder.getState());
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
				Log.d(TAG, "Sleeping has been interrupted exception");
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
			Log.e(TAG, "checking directory for old files");
			File lastModifiedFile = files[0];
			Log.e(TAG, "putting them into array");
			Log.e(TAG, "ordering files by date modified");
			for (int i = 1; i < files.length; i++) {
				if (lastModifiedFile.lastModified() > files[i].lastModified()) {
					lastModifiedFile = files[i];
				}
			}

			if (lastModifiedFile.lastModified() < (System.currentTimeMillis() - 300000)) {
				Log.e(TAG,
						"deleting old file: "
								+ lastModifiedFile.getAbsolutePath());
				if (!lastModifiedFile.exists()) {
					Log.e(TAG, "file does not exist");
				} else {
					boolean deleted = lastModifiedFile.delete();
					Log.e(TAG, "file deleted=" + deleted);
				}

			} else {
				Log.e(TAG, "no old files found");
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

	private void getProperFormats() {
		for (int rate : mSampleRates) {
			for (short audioFormat : audioFormatEncoding) {
				for (short channelConfig : audioFormatChannel) {
					try {
						Log.d(TAG, "Attempting rate " + rate + "Hz, bits: "
								+ audioFormat + ", channel: " + channelConfig);
						bufferSize = AudioRecord.getMinBufferSize(rate,
								channelConfig, audioFormat);

						if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {

							recorder = new AudioRecord(AudioSource.DEFAULT,
									rate, channelConfig, audioFormat,
									bufferSize);

							if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {

								Log.d(TAG, "rate: " + rate);
								Log.d(TAG, "audioFormatEncoding: "
										+ audioFormat);
								Log.d(TAG, "audioFormatChannel: "
										+ channelConfig);
								return;

							}

						}
					} catch (Exception e) {
						Log.d(TAG, rate + "Exception, keep trying.", e);
					}
				}
			}
		}

		Log.d(TAG, "recorder state: " + recorder.getState());

		Log.d(TAG, "bufferSize: " + bufferSize);
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
