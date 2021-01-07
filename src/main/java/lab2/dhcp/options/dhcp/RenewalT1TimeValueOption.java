package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractTimeOption;

public final class RenewalT1TimeValueOption extends AbstractTimeOption {
    @Override
    public RenewalT1TimeValueOptionDescription getDescription() {
        return RenewalT1TimeValueOptionDescription.INSTANCE;
    }
}
