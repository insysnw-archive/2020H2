package lab2.dhcp;

import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;

public interface Option {
    int MAX_SIZE = 255;

    OptionDescription getDescription();
    void encode(Encoder encoder);
    void decode(Decoder decoder);
}
