import java.io.IOException;

class VFS implements Device {

    private Device[] devices = new Device[10];
    private int[] ids = new int[10];

    @Override
    public int Open(String s) throws IOException {
        System.out.println("Open syscall inside the VFS, String: " + s);
        String[] parsedOpen = s.split(" "); // splitting the device name from the device function
        switch (parsedOpen[0]) { // string should look like "random 112", splitting by space should give an array of ["random", "112"]
            case "random" -> {
                System.out.println("Random case, seeding the string listed above.");
                for (int i = 0; i < devices.length; i++) { // iterate through devices, find a null device, initialize
                    if (devices[i] == null) {
                        devices[i] = new RandomDevice();
                        int open = devices[i].Open(parsedOpen[1]);
                        ids[i] = open;
                        System.out.println(open + " is the return value of VFS random open");
                        return i;
                    }
                }
                return -1;
            }
            case "file" -> {
                for (int i = 0; i < 10; i++) { // iterate through devices, if it's a file, check to see if name matches, else initialize
                    if (devices[i] instanceof FakeFileSystem) {
                        int contains = ((FakeFileSystem) devices[i]).contains(parsedOpen[1]);
                        if (contains != -1) {
                            ids[i] = contains; // contains returned the appropriate index
                            return i;
                        }
                    } else if (devices[i] == null) {
                        devices[i] = new FakeFileSystem(); // initialize
                        int open = devices[i].Open(parsedOpen[1]);
                        ids[i] = open;
                        System.out.println(open + " is the return value of VFS file open");
                        return i;
                    }
                }
                return -1;
            }
            default -> {
                System.out.println("name: " + parsedOpen[0] + " is not a device");
                return -1; // if neither file nor random, it's not a proper device
            }
        }
    }

    @Override
    public void close(int id) throws IOException {
        //remove the device and id entries
        if (id == -1) return;
        if (devices[id] == null || ids[id] == -1) return; // attempting to close an unopened device is not expensive
        devices[id].close(ids[id]);
        devices[id] = null;
        ids[id] = -1;
    }

    @Override
    public byte[] Read(int id, int size) throws IOException {
        // get mapped device and id from the 2 arrays, pass to appropriate function
        return devices[id].Read(ids[id], size);
    }

    @Override
    public void Seek(int id, int to) throws IOException {
        devices[id].Seek(ids[id], to);
    }

    @Override
    public int Write(int id, byte[] data) throws IOException {
        return devices[id].Write(ids[id], data);
    }
}