package xmu.swordbearer.audio.sender;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xmu.swordbearer.audio.AudioCodec;
import xmu.swordbearer.audio.data.AudioData;
import android.os.Environment;
import android.util.Log;

public class AudioEncoder implements Runnable {
    String LOG = "AudioEncoder";

    private static AudioEncoder encoder;
    private boolean isEncoding = false;

    private List<AudioData> dataList = null;

    public static AudioEncoder getInstance() {
        if (encoder == null) {
            encoder = new AudioEncoder();
        }
        return encoder;
    }

    private AudioEncoder() {
        dataList = Collections.synchronizedList(new LinkedList<AudioData>());
    }

    public void addData(byte[] data, int size) {
        AudioData rawData = new AudioData();
        rawData.setSize(size);
        byte[] tempData = new byte[size];
        System.arraycopy(data, 0, tempData, 0, size);
        rawData.setRealData(tempData);
        dataList.add(rawData);
    }

    /*
     * start encoding
     */
    public void startEncoding() {
        System.out.println(LOG + "start encode thread");
        if (isEncoding) {
            Log.e(LOG, "encoder has been started  !!!");
            return;
        }
        dataList = new ArrayList<AudioData>();
        new Thread(this).start();
    }

    /*
     * end encoding
     */
    public void stopEncoding() {
        this.isEncoding = false;
    }

    public void run() {
        // start sender before encoder

        int encodeSize = 0;
        byte[] encodedData = new byte[256];

        // initialize audio encoder:mode is 30
        AudioCodec.audio_codec_init(30);

        isEncoding = true;
        while (isEncoding) {
            if (dataList.size() == 0) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            if (isEncoding) {
                AudioData rawData = dataList.remove(0);
                encodedData = new byte[rawData.getSize()];
                //
                encodeSize = AudioCodec.audio_encode(rawData.getRealData(), 0, rawData.getSize(), encodedData, 0);
                System.out.println();
                if (encodeSize > 0) {
                    // clear data
                    dataList.add(new AudioData(encodedData.length, encodedData));
                    encodedData = new byte[encodedData.length];
                }
            }
        }
        System.out.println(LOG + "end encoding");
        write2Amr();
    }

    /**
     * 根据byte数组，生成文件
     */
    public void getFile(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {// 判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 
     */
    private void write2Amr() {
        if (dataList != null) {

            int count = 0;
            for (int i = 0; i < dataList.size(); i++) {
                count += dataList.get(i).getSize();
            }
            System.out.println(" the count " + count);
            byte[] bData = new byte[count];
            for (int i = 0; i < dataList.size(); i++) {
                System.arraycopy(dataList.get(i).getRealData(), 0, bData, dataList.get(i).getSize()*i, dataList.get(i).getSize());
            }
            getFile(bData, Environment.getExternalStorageDirectory().getAbsolutePath(), "test.amr");
        }
    }
}
