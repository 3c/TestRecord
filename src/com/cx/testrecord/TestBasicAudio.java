/**
 * Filename : TestBasicAudio.java Author : CX Date : 2013-11-8
 * 
 * Copyright(c) 2011-2013 Mobitide Android Team. All Rights Reserved.
 */
package com.cx.testrecord;

/**
 * @author CX
 * 
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import xmu.swordbearer.audio.AudioCodec;
import xmu.swordbearer.audio.AudioWrapper;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

/**
 * class name：TestBasicAudio<BR>
 * class description：Basic Record Audio Demo<BR>
 * 
 * @version 1.00 2011/12/01
 * @author CODYY)peijiangping
 */
public class TestBasicAudio extends Activity {
    private Button button_start;
    private Button button_stop;
    private MediaRecorder recorder;
    private AudioWrapper audioWrapper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioWrapper = AudioWrapper.getInstance();
        getWindow().setFormat(PixelFormat.TRANSLUCENT);// 让界面横屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉界面标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 重新设置界面大小
        setContentView(R.layout.main);


        File f=new File(Environment.getExternalStorageDirectory() + "/a.amr");
        System.out.println(f.exists());
        byte[] bSource=readFile();
        byte[] bData=new byte[bSource.length];
        int size = AudioCodec.audio_encode(bSource, 0, bSource.length, bData, 0);
        System.out.println(size);
        


        init();
    }


    private byte[] readFile() {
        try {
            BufferedInputStream in =
                    new BufferedInputStream(new FileInputStream(Environment.getExternalStorageDirectory() + "/a.amr"));
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

            System.out.println("Available bytes:" + in.available());

            byte[] temp = new byte[1024];
            int size = 0;
            while ((size = in.read(temp)) != -1) {
                out.write(temp, 0, size);
            }
            in.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    private void init() {
        button_start = (Button) this.findViewById(R.id.start);
        button_stop = (Button) this.findViewById(R.id.stop);
        button_stop.setOnClickListener(new AudioListerner());
        button_start.setOnClickListener(new AudioListerner());
    }

    class AudioListerner implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == button_start) {
                // audioWrapper.startRecord();
                VoiceApi.startRecordVoice();
                // initializeAudio();
            }
            if (v == button_stop) {
                // audioWrapper.stopRecord();
                VoiceApi.stopRecordVoice();
                // recorder.stop();// 停止刻录
                // // recorder.reset(); // 重新启动MediaRecorder.
                // recorder.release(); // 刻录完成一定要释放资源
                // // recorder = null;
            }
        }



        private void initializeAudio() {
            recorder = new MediaRecorder();// new出MediaRecorder对象
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 设置MediaRecorder的音频源为麦克风
            recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            // 设置MediaRecorder录制的音频格式
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // 设置MediaRecorder录制音频的编码为amr.
            recorder.setOutputFile("/sdcard/peipei.amr");
            // 设置录制好的音频文件保存路径
            try {
                recorder.prepare();// 准备录制
                recorder.start();// 开始录制
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
