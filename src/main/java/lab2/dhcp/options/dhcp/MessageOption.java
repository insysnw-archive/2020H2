package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractStringOption;

public final class MessageOption extends AbstractStringOption {
    @Override
    public MessageOptionDescription getDescription() {
        return MessageOptionDescription.INSTANCE;
    }
}
