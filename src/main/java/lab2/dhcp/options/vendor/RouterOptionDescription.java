package lab2.dhcp.options.vendor;

import lab2.dhcp.ConfigurableOptionDescription;

public enum RouterOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 3;
    }

    @Override
    public String getName() {
        return "router";
    }

    @Override
    public RouterOption produce() {
        return new RouterOption();
    }
}
