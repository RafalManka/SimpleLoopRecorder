package pl.rafalmanka.SimpleLoopRecorder;

import android.app.Application;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class LoopRecorderApp extends Application {

	private static final String TAG = "LoopRecorderApp"; 
	
	private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
	private static short[] audioFormatEncoding = new short[] {
			AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT };
	private static short[] audioFormatChannel = new short[] {
			AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
	private int bufferSize = 0;
	AudioRecord recorder;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreated");
		setAudioRecorder();
		Log.d(TAG, "recorder set");
	}
	
	public AudioRecord getAudioRecorder(){
		return recorder;
	}
	
	public int getBufferSize(){
		return bufferSize;
	}
	
	private void setAudioRecorder(){
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
	

}
