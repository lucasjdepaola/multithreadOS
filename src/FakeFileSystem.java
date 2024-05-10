import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

class FakeFileSystem implements Device {
    class RandomFile {
        RandomAccessFile file;
        String name;
        String mode;

        /*
        Custom file class that saves the file name, used for potential duplication of files, or potential future usage
         */
        public RandomFile(String name, String mode) throws FileNotFoundException {
            this.name = name;
            this.mode = mode;
            file = new RandomAccessFile(name, mode);
        }
    }

    private final RandomFile[] rfs = new RandomFile[10];
    private final String mode = "rw";

    @Override
    public int Open(String s) throws IOException {
        System.out.println("Attempting to open a file with the name" + s);
        if (s == null || s.equals("")) return -1;
        int containInt = contains(s); // if file exists, return index
        if (containInt != -1) return containInt;
        else {
            for (int i = 0; i < 10; i++) {
                if (rfs[i] == null) {
                    System.out.println("File has not been created, creating file, ID: " + i);
                    rfs[i] = new RandomFile(s, mode);
                    return i;
                }
            }
        }
        return -1;
    }

    public int contains(String s) {
        for (int i = 0; i < 10; i++) { // iterate over my class, if not null and name matches, return index
            if (rfs[i] != null && rfs[i].name.equals(s)) return i;
        }
        return -1;
    }


    @Override
    public void close(int id) throws IOException {
        if (rfs[id] != null)
            rfs[id].file.close(); // native method, also nullifying to be sure
        rfs[id] = null;
    }

    @Override
    public byte[] Read(int id, int size) throws IOException {
        byte[] bytes = new byte[size];
        // native method
        rfs[id].file.read(bytes);
        System.out.println("Successfully read file, here are the bytes that have been read on id: " + id + Arrays.toString(bytes));
        return bytes;
    }

    @Override
    public void Seek(int id, int to) throws IOException {
        // native method
        rfs[id].file.seek(to);
    }

    @Override
    public int Write(int id, byte[] data) throws IOException {
        System.out.println("Writing data to the file, ID: " + id);
        // native method
        rfs[id].file.write(data);
        return 0;
    }
}