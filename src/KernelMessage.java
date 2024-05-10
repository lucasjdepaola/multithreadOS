import java.util.Arrays;

class KernelMessage {
    int senderpid;
    int receiverpid;
    int typeofmessage;
    int whatmessage;
    private byte[] message;
    public byte[] getMessage() { return message; }
    public KernelMessage(byte[] message, int receiverpid, int typeofmessage, int whatmessage, int senderpid) {
        this.message = message;
        this.receiverpid = receiverpid;
        this.typeofmessage = typeofmessage;
        this.whatmessage = whatmessage;
        this.senderpid = senderpid;
    }

    public KernelMessage copy() {
        return new KernelMessage(getMessage().clone(), receiverpid, typeofmessage, whatmessage, senderpid);
    }

    public String toString() {
        String m = new String(message);
        return "KernelMessage: " + m + " from: " + senderpid + ", to: " + receiverpid + ", what: " + whatmessage;
    }
}