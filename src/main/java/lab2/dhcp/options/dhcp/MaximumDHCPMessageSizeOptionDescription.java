package lab2.dhcp.options.dhcp;

import lab2.dhcp.Option;
import lab2.dhcp.OptionDescription;

public enum MaximumDHCPMessageSizeOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 57;
    }

    @Override
    public Option produce() {
        return new MaximumDHCPMessageSizeOption();
    }
}
