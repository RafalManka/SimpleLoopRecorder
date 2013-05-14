package pl.rafalmanka.SimpleLoopRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log; 
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "AudioRecorder";
	private static int RECORDER_BPP = 8; 
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private static final String TEMP_FOLDER = Environment
			.getExternalStorageDirectory().getPath() + "/AudioRecorder/temp/";
	private static final String FINAL_FOLDER = Environment
			.getExternalStorageDirectory().getPath() + "/AudioRecorder/final/";
	private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
	private static short[] audioFormatEncoding = new short[] {
			AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT };
	private static short[] audioFormatChannel = new short[] {
			AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
	private int bufferSize = 0;
	private int RECORDER_SAMPLERATE = 0;
	private AudioRecord recorder = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		
		getProperFormats();
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

								RECORDER_SAMPLERATE = rate;
								RECORDER_BPP = audioFormat;
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
	public void startRecording(View v) { 
		Intent intent = new Intent(this,RecordingService.class);
		startService(intent);
	}
	
	public void stopRecording(View v) {	
		Intent intent = new Intent(this,RecordingService.class);
		stopService(intent);
	}

	public void concatenate(View v) {
		Log.d(TAG, "preparing for joining of audio files");
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = 0;
		int channels = 2;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;
		byte[] data = new byte[bufferSize];
		try {
			File dir = new File(TEMP_FOLDER);
			
			Log.d(TAG, "name of temp directory: "
					+ Environment.getExternalStorageDirectory().getPath()
					+ "/AudioRecorder/");
			File[] files = dir.listFiles();
			Log.d(TAG, "got list of files");
			File file_tmp = new File(FINAL_FOLDER);
			if (!file_tmp.exists()) {
				file_tmp.mkdirs();
				Log.d(TAG, "FINAL_FOLDER has been ceated");
			}
			file_tmp=null;
			
			out = new FileOutputStream(FINAL_FOLDER + "/"
					+ System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
			Log.d(TAG, "got file output stream");
			Log.d(TAG, "mesuring lenght of every single file");
			for (File file : files) {
				Log.d(TAG, "lenght: " + file.length());
				FileInputStream fis = new FileInputStream(file);
				totalAudioLen += fis.getChannel().size();
			}
			Log.d(TAG, "totalAudioLen: " + totalAudioLen);
			totalDataLen = totalAudioLen + 42;
			Log.d(TAG, "totalDataLen: " + totalDataLen);
			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					RECORDER_SAMPLERATE, channels, byteRate);
			Log.d(TAG, "headers created ");
			for (File file : files) {
				FileInputStream fis = new FileInputStream(file);
				while (fis.read(data) != -1) {
					out.write(data);
				}
				Log.d(TAG, "writen content of file: " + file.getName());
			}
			out.close();
			Toast.makeText(this, "Done!!", Toast.LENGTH_LONG).show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "files combined together");	
	}

	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = (byte) RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/*private void copyWaveFile(String inFilename, String outFilename) {
	FileInputStream in = null;
	FileOutputStream out = null;
	long totalAudioLen = 0;
	long totalDataLen = totalAudioLen + 36;
	long longSampleRate = RECORDER_SAMPLERATE;
	int channels = 2;
	long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

	byte[] data = new byte[bufferSize];

	try {
		in = new FileInputStream(inFilename);
		out = new FileOutputStream(outFilename);
		totalAudioLen = in.getChannel().size();
		totalDataLen = totalAudioLen + 36;

		Log.d(TAG, "File size: " + totalDataLen);

		WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
				longSampleRate, channels, byteRate);

		while (in.read(data) != -1) {
			out.write(data);
		}

		in.close();
		out.close();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
}*/

}
