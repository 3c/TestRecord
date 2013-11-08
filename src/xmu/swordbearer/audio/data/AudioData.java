package xmu.swordbearer.audio.data;

public class AudioData {
    int size;
    byte[] realData;

    public AudioData() {

    }

    public AudioData(int size, byte[] realData) {
        super();
        this.size = size;
        this.realData = realData;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte[] getRealData() {
        return realData;
    }

    public void setRealData(byte[] realData) {
        this.realData = realData;
    }

}
