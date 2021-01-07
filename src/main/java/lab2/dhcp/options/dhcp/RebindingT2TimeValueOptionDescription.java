package lab2.dhcp.options.dhcp;

import lab2.dhcp.Option;
import lab2.dhcp.OptionDescription;

public enum RebindingT2TimeValueOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 59;
    }

    @Override
    public Option produce() {
        return new RebindingT2TimeValueOption();
    }
}
