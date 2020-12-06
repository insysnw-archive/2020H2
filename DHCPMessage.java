

import java.util.Arrays;
import java.util.Random;


public class DHCPMessage {

    private final byte op = 1;
    private final byte hType = 1;
    private final byte hLen = 6;
    private final byte hops = 0;
    private final byte[] xId = new byte[4];
    private final byte[] secs = new byte[2];
    private final byte[] flags = new byte[2];
    private final byte[] ciAddr = new byte[4];
    private final byte[] yiAddr = new byte[4];
    private final byte[] siAddr = new byte[4];
    private final byte[] giAddr = new byte[4];
    private final byte[] sName = new byte[64];
    public static final byte[] magic_cookie = {99, (byte) 130, 83, 99};

    private final byte[] file = new byte[128];
    private byte[] options;

    public DHCPMessage() {
    }


    public byte[] createDiscover(byte[] MAC) {
        new Random().nextBytes(xId);
        options = new byte[4];
        options[0] = (byte) 53;
        options[1] = (byte) 1;
        options[2] = (byte) 1;
        options[3] = (byte) 255;

        Arrays.fill(secs, (byte) 0);
        Arrays.fill(flags, (byte) 0);
        Arrays.fill(ciAddr, (byte) 0);
        Arrays.fill(yiAddr, (byte) 0);
        Arrays.fill(siAddr, (byte) 0);
        Arrays.fill(giAddr, (byte) 0);
        Arrays.fill(sName, (byte) 0);
        Arrays.fill(file, (byte) 0);

        byte[] ret = new byte[244];
        ret[0] = op;
        ret[1] = hType;
        ret[2] = hLen;
        ret[3] = hops;
        int j = 4;
        j = copyArrayPart(xId, ret, j);
        j = copyArrayPart(secs, ret, j);
        j = copyArrayPart(flags, ret, j);
        j = copyArrayPart(ciAddr, ret, j);
        j = copyArrayPart(yiAddr, ret, j);
        j = copyArrayPart(siAddr, ret, j);
        j = copyArrayPart(giAddr, ret, j);

        for (int i = 0; i < 16; i++, j++) {

            if (i < hLen)
                ret[j] = MAC[i];
            else ret[j] = 0;
        }
        j = copyArrayPart(sName, ret, j);
        j = copyArrayPart(file, ret, j);
        j = copyArrayPart(magic_cookie, ret, j);
        j = copyArrayPart(options, ret, j);

        return ret;
    }

    public byte[] createRequest(byte[] response, byte[] secs) {
        byte[] ret = new byte[246];


        ret[0] = op;
        System.arraycopy(response, 1, ret, 1, 235);
        int j = 236;
        for (int i = 0; i < magic_cookie.length; i++, j++) {
            ret[j] = magic_cookie[i];
        }
        for (int i = 0; i < 2; i++, j++) {
            ret[j] = response[j];
        }
        ret[j] = (byte) 3;
        j++;

        ret[j] = (byte) 255;
        return ret;
    }

    private int copyArrayPart(byte[] src, byte[] dest, int counter) {
        System.arraycopy(src, 0, dest, counter, src.length);
        counter += src.length;
        return counter;
    }
}
