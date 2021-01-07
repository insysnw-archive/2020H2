package lab2.dhcp.options.dhcp;

import lab2.dhcp.OptionDescription;

public enum DHCPMessageTypeOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 53;
    }

    @Override
    public DHCPMessageTypeOption produce() {
        return new DHCPMessageTypeOption();
    }
}
