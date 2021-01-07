package lab2.dhcp.options.vendor;

import lab2.dhcp.ConfigurableOptionDescription;

public enum HostNameOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 12;
    }

    @Override
    public String getName() {
        return "hostname";
    }

    @Override
    public HostNameOption produce() {
        return new HostNameOption();
    }
}
