package lab2.dhcp.options.dhcp;

import lab2.dhcp.OptionDescription;

public enum MessageOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 56;
    }

    @Override
    public MessageOption produce() {
        return new MessageOption();
    }
}
