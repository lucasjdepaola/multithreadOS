import java.util.Arrays;
import java.util.Random;

class RandomDevice implements Device {

    final Random[] items = new Random[10];


    @Override
    public int Open(String s) {
        System.out.println(s + " is the string passed to RandomDevice open");
        if (s == null || s.isEmpty()) return -1;
        for (int i = 0; i < 10; i++) { // iterate over random devices, if null, create new device with proper seeding
            if (items[i] == null) {
                items[i] = new Random(Long.parseLong(s));
                System.out.println("Successfully seeded " + s + " inside of random array index " + i);
                return i;
            }
        }
        return -1;
    }

    @Override
    public void close(int id) {
        items[id] = null; // nullify
    }

    @Override
    public byte[] Read(int id, int size) {
        System.out.println("Attempting to read on index" + id + " of size " + size);
        byte[] returnBytes = new byte[size];
        items[id].nextBytes(returnBytes); // native method
        System.out.println("Inside read, bytes are: " + Arrays.toString(returnBytes));
        return returnBytes;
    }

    @Override
    public void Seek(int id, int to) {
        for (int i = 0; i < to; i++) {
            Read(id, 1);
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        return 0;
    }
}