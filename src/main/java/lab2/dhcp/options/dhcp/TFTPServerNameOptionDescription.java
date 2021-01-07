package lab2.dhcp.options.dhcp;

import lab2.dhcp.ConfigurableOptionDescription;
import lab2.dhcp.Option;
import lab2.dhcp.OptionDescription;

public enum TFTPServerNameOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 66;
    }

    @Override
    public String getName() {
        return "tftp-server-name";
    }

    @Override
    public TFTPServerNameOption produce() {
        return new TFTPServerNameOption();
    }
}
