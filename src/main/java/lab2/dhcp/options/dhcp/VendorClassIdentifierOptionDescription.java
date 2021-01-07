package lab2.dhcp.options.dhcp;

import lab2.dhcp.OptionDescription;

public enum VendorClassIdentifierOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 60;
    }

    @Override
    public VendorClassIdentifierOption produce() {
        return new VendorClassIdentifierOption();
    }
}
