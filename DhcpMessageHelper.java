public class DhcpMessageHelper {

    public static short interpret(byte[] buf) {
        int i = 236;
        for (; i < 240; i++) {
            if (buf[i] != DHCPMessage.magic_cookie[i - 236]) return 0;
        }
        while (buf[i] != 53) {
            i++;
            i += buf[i];
            i++;
        }
        i += 2;
        return buf[i];
    }
}
