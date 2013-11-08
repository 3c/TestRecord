package com.cx.testrecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class VoiceApi {
	private static final String TAG = VoiceApi.class.getSimpleName();

	/**
	 * To Hold The Context Instance.
	 */
	private Context currentContext = null;

	private static MediaRecorder recorder = null;
	static MediaPlayer player;
	public static Handler handler_voice;

	private static int audioSource = MediaRecorder.AudioSource.MIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private static int sampleRateInHz = 44100;
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	// 缓冲区字节大小
	private static int bufferSizeInBytes = 0;
	private static AudioRecord audioRecord;
	private static boolean isRecord = false;// 设置正在录制的状态
	// AudioName裸音频数据文件
	private static final String AudioName = Environment
			.getExternalStorageDirectory() + "/test/recorder/temp.raw";
	// NewAudioName可播放的音频文件
	private static String NewAudioName = null;


	/**
	 * @return audio path 这个是用audioRecord实现的录音，录音文件较大，几百K.需要压缩。
	 */
    public static String startRecordVoice() {
        File storageDir = new File(Environment.getExternalStorageDirectory(), "test/recorder");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        NewAudioName =
                Environment.getExternalStorageDirectory() + "/test/recorder/" + System.currentTimeMillis() + ".amr";
        startRecord();
        return NewAudioName;
    }

    /**
     * @param date audioRecord 停止录音。
     */
    public synchronized static void stopRecordVoice() {
        close();
        // 可以在这里处理音频，转码成小文件。

        // 发送message，告诉主界面文件已处理完，可以发送。
        // Message msg = new Message();
        // msg.obj = date;
        // handler_voice.sendMessage(msg);
    }

	/**
	 * 使用mediaRecord录音
	 * 
	 * @return 录音文件路径
	 */
//	public static String startRecordVoice() {
//		File storageDir = null;
//		if (android.os.Environment.getExternalStorageState().equals(
//				android.os.Environment.MEDIA_MOUNTED)) {
//			if (Environment.getExternalStorageDirectory() != null) {
//				storageDir = new File(
//						Environment.getExternalStorageDirectory(),
//						"test/recorder");
//			}
//		} else {
//			storageDir = new File("data/data/com.mobitide.guoxin/GuoXinInfo",
//					"test/recorder");
//		}
//
//		if (!storageDir.exists()) {
//			storageDir.mkdirs();
//		}
//		try {
//			initRecord();
//			// File outFile =
//			// File.createTempFile(String.valueOf(System.currentTimeMillis()),
//			// ".3gp", storageDir);
//			File outFile = new File(storageDir + "/"
//					+ System.currentTimeMillis() + ".amr");
//			recorder.setOutputFile(outFile.getAbsolutePath());
//			recorder.prepare();
//			recorder.start();
//			return outFile.getAbsolutePath();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	public synchronized static void stopRecordVoice(final String date,
			final boolean result) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					// 因为用MediaRecord录音，结尾处会丢失几秒钟，这里故意延迟2秒，再进行音频的生成
					Thread.sleep(2000);

					recorder.stop();
					// recorder.reset();
					recorder.release();

					// result==true 意味着录音成功
					if (result) {
						Message msg = new Message();
						msg.obj = date;
						msg.what = 0x01;

						// 这里的handler_voice 发出的消息在 聊天界面有接收（这里可以改为广播）
						handler_voice.sendMessageDelayed(msg, 1000);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

	}

	public static void playVoice(String path) {
		playVoice(path, null);
	}
	
	public static void playVoice(String path,final IOnVoicePlayFinishListener mIOnVoicePlayFinishListener) {
		
		try {
			if (player != null) {
				player.reset();
				player.release();
				player = null;
			}
			player = new MediaPlayer();
			player.setDataSource(path);
			player.prepare();
			if(mIOnVoicePlayFinishListener!=null){
				mIOnVoicePlayFinishListener.onStart();
			}
			player.start();
			player.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					if(mIOnVoicePlayFinishListener!=null){
						mIOnVoicePlayFinishListener.onFinish();
					}
					player.stop();
					player.reset();
					player.release();
					player = null;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void creatAudioRecord() {
		// 获得缓冲区字节大小
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);
		// 创建AudioRecord对象
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes);
	}

	private static void startRecord() {
		creatAudioRecord();
		audioRecord.startRecording();
		// 让录制状态为true
		isRecord = true;
		// 开启音频文件写入线程
		new Thread(new AudioRecordThread()).start();
	}

	private static void stopRecord() {
		close();
	}

	private static void close() {
		if (audioRecord != null) {
			System.out.println("stopRecord");
			isRecord = false;// 停止文件写入
			audioRecord.stop();
			audioRecord.release();// 释放资源
			audioRecord = null;
		}
	}

	static class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateTOFile();// 往文件中写入裸数据
			copyWaveFile(AudioName, NewAudioName);// 给裸数据加上头文件
		}
	}

	/**
	 * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
	 * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
	 * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
	 */
	private static void writeDateTOFile() {
		// new一个byte数组用来存一些字节数据，大小为缓冲区大小
		byte[] audiodata = new byte[bufferSizeInBytes];
		FileOutputStream fos = null;
		int readsize = 0;
		try {
			File file = new File(AudioName);
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(file);// 建立一个可存取字节的文件
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (isRecord == true) {
			readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
			System.out.println(readsize);
			if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
				try {
					fos.write(audiodata);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			fos.close();// 关闭写入流
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 这里得到可播放的音频文件
	private static void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = sampleRateInHz;
		int channels = 2;
		long byteRate = 16 * sampleRateInHz * channels / 8;
		byte[] data = new byte[bufferSizeInBytes];
		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
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
	}

	/**
	 * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
	 * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有 自己特有的头文件。
	 */
	private static void WriteWaveFileHeader(FileOutputStream out,
			long totalAudioLen, long totalDataLen, long longSampleRate,
			int channels, long byteRate) throws IOException {
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
		header[34] = 16; // bits per sample
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

	public static void initRecord() {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		recorder.setOnErrorListener(new OnErrorListener() {

			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				// TODO Auto-generated method stub
				recorder = null;
				initRecord();
			}
		});
	}

	/**
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @param pattern
	 *            an array of longs of times for which to turn the vibrator on
	 *            or off.
	 * @param repeat
	 *            the index into pattern at which to repeat, or -1 if you don't
	 *            want to repeat.
	 */
	public void vibrate(Context context, long[] pattern, int repeat) {
		this.currentContext = context;
		Vibrator mVibrator = (Vibrator) context
				.getSystemService(android.content.Context.VIBRATOR_SERVICE);
		mVibrator.vibrate(pattern, repeat);
	}

	/**
	 * Make a standard toast that just contains a text view.
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @param message
	 *            The text to show. Can be formatted text.
	 * @param duration
	 *            How long to display the message. Either {@link #LENGTH_SHORT}
	 *            or {@link #LENGTH_LONG}
	 */
	public static void makeToast(Context context, String message, int duration) {
		Toast.makeText(context, message, duration).show();
	}

	/**
	 * Make a standard toast that just contains a text view with
	 * {@link #LENGTH_SHORT}.
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @param message
	 *            The text to show. Can be formatted text.
	 */
	public static void makeToastShort(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Make a standard toast that just contains a text view with
	 * {@link #LENGTH_LONG}.
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @param message
	 *            The text to show. Can be formatted text.
	 */
	public static void makeToastLong(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	public void setNetworkConnection(Context context) {
		this.currentContext = context;

		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		if (networkInfo == null) {
			VoiceApi.makeToastShort(currentContext, "无网络连接");
			return;
		}
		switch (networkInfo.getType()) {
		case ConnectivityManager.TYPE_WIFI:
			VoiceApi.makeToastShort(context, "WIFI Connection");
			break;
		case ConnectivityManager.TYPE_MOBILE:
			VoiceApi.makeToastShort(context, "MOBILE Connection");
			break;
		default:
			VoiceApi.makeToastShort(context, "Unknown Connection");
			break;
		}

		boolean available = networkInfo.isAvailable();
		if (available) {
			VoiceApi.makeToastShort(context,
					"Current Connectivity is Available");
		} else {
			VoiceApi.makeToastShort(context,
					"Current Connectivity is not Available");
		}

		State state = connectivityManager.getNetworkInfo(
				ConnectivityManager.TYPE_MOBILE).getState();
		if (State.CONNECTED == state) {
			VoiceApi.makeToastShort(context, "GPRS Connected");
		} else if (State.CONNECTING == state) {
			VoiceApi.makeToastShort(context, "GPRS Connecting");
		} else if (State.DISCONNECTING == state) {
			VoiceApi.makeToastShort(context, "GPRS Disconnecting");
		} else if (State.DISCONNECTED == state) {
			VoiceApi.makeToastShort(context, "GPRS Disconnected");
		} else {
			VoiceApi.makeToastShort(context, "Other GPRS State");
		}

		state = connectivityManager.getNetworkInfo(
				ConnectivityManager.TYPE_WIFI).getState();
		if (State.CONNECTED == state) {
			VoiceApi.makeToastShort(context, "WIFI Connected");
		} else if (State.CONNECTING == state) {
			VoiceApi.makeToastShort(context, "WIFI Connecting");
		} else if (State.DISCONNECTING == state) {
			VoiceApi.makeToastShort(context, "WIFI Disconnecting");
		} else if (State.DISCONNECTED == state) {
			VoiceApi.makeToastShort(context, "WIFI Disconnected");
		} else {
			VoiceApi.makeToastShort(context, "Other WIFI State");
		}

		context.startActivity(new Intent(
				android.provider.Settings.ACTION_WIRELESS_SETTINGS));
	}

	/**
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @return true if voice notification is enabled else false
	 */
	public static boolean isMessageNotificationVoiceEnabled(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"setting", Context.MODE_WORLD_READABLE);
		if (sharedPreferences.contains("MESSAGE_NOTIFICATION_VOICE")) {
			Log.d(TAG, "in is Message Notification Voice Enabled");
			return sharedPreferences.getBoolean("MESSAGE_NOTIFICATION_VOICE",
					false);
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @return true if vibrate notification is enabled else false
	 */
	public static boolean isMessageNotificationVibrateEnabled(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"setting", Context.MODE_WORLD_READABLE);
		if (sharedPreferences.contains("MESSAGE_NOTIFICATION_VIBRATE")) {
			Log.d(TAG, "in is Message Notification Vibrate Enabled");
			return sharedPreferences.getBoolean("MESSAGE_NOTIFICATION_VIBRATE",
					false);
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @return true if allow upload location information else false
	 */
	public static boolean isUploadLocationStatusEnabled(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"setting", Context.MODE_WORLD_READABLE);
		if (sharedPreferences.contains("UPLOAD_LOCATION_STATUS")) {
			Log.d(TAG, "in is Upload Location Status Enabled");
			return sharedPreferences
					.getBoolean("UPLOAD_LOCATION_STATUS", false);
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 *            The context to use. Usually your
	 *            {@link android.app.Application} or
	 *            {@link android.app.Activity} object.
	 * @return true if allow upload location information else false
	 */
	public static boolean isSystemAutomatedUpdateEnabled(Context context) {
		Log.e("hjw", "is very");
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"setting", Context.MODE_WORLD_READABLE);
		if (sharedPreferences.contains("SYSTEM_AUTOMATED_UPDATE")) {
			Log.d(TAG, "in is System Automated Update Enabled");
			return sharedPreferences.getBoolean("SYSTEM_AUTOMATED_UPDATE",
					false);
		}
		return false;
	}
}
