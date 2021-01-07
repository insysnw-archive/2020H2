package lab2.dhcp.options.dhcp;

import lab2.dhcp.OptionDescription;

public enum IpAddressLeaseTimeOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 51;
    }

    @Override
    public IpAddressLeaseTimeOption produce() {
        return new IpAddressLeaseTimeOption();
    }
}
