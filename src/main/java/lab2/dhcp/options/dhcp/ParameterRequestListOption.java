package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractBytesOption;

public final class ParameterRequestListOption extends AbstractBytesOption {
    @Override
    public ParameterRequestListOptionDescription getDescription() {
        return ParameterRequestListOptionDescription.INSTANCE;
    }
}
