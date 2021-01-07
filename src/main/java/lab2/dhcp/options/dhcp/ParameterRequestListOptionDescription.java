package lab2.dhcp.options.dhcp;

import lab2.dhcp.OptionDescription;

public enum ParameterRequestListOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 55;
    }

    @Override
    public ParameterRequestListOption produce() {
        return new ParameterRequestListOption();
    }
}
