package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractStringOption;

public final class VendorClassIdentifierOption extends AbstractStringOption {
    @Override
    public VendorClassIdentifierOptionDescription getDescription() {
        return VendorClassIdentifierOptionDescription.INSTANCE;
    }
}
