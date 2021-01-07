package lab2.dhcp.options.dhcp;

import lab2.dhcp.Option;
import lab2.dhcp.util.DHCPIllegalFormatException;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;

public final class OptionOverloadOption implements Option {
    public enum Variants {
        NotSet, OnlyFile, OnlyServerName, Both
    }
    private Variants code = Variants.NotSet;

    public Variants getCode() {
        return code;
    }

    public void setCode(Variants value) {
        code = value;
    }

    @Override
    public OptionOverloadOptionDescription getDescription() {
        return OptionOverloadOptionDescription.INSTANCE;
    }

    private static Variants select(byte c) {
        switch (c) {
            case 1: return Variants.OnlyFile;
            case 2: return Variants.OnlyServerName;
            case 3: return Variants.Both;
            default: throw new DHCPIllegalFormatException("invalid option overload code: " + c);
        }
    }

    @Override
    public void encode(Encoder encoder) {
        encoder.putByte((byte) code.ordinal());
    }

    @Override
    public void decode(Decoder decoder) {
        code = select(decoder.getByte());
    }
}
