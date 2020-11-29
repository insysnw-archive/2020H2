import java.io.FileInputStream;
import java.security.MessageDigest;


public class SumHelper {
    static String getChecksum(String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(fileName);
            byte[] dataBytes = new byte[1024];

            int nread = 0;

            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            fis.close();

            byte[] mdBytes = md.digest();

            for (byte mdByte : mdBytes) {
                sb.append(Integer.toString((mdByte & 0xff) + 0x100, 16).substring(1));
            }

        } catch (Exception e) {
            System.out.println("Generate Checksum Failed: " + e.getMessage());
        }

        return sb.toString();
    }
}
