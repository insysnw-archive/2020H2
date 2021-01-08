package com.github.lexcorp3439.net.lab2.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.util.Decoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;

public interface Option {
    int MAX_SIZE = 255;

    OptionDescription getDescription();
    void encode(Encoder encoder);
    void decode(Decoder decoder);
}
