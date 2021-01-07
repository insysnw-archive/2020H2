package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractTimeOption;

public final class RebindingT2TimeValueOption extends AbstractTimeOption {
    @Override
    public RebindingT2TimeValueOptionDescription getDescription() {
        return RebindingT2TimeValueOptionDescription.INSTANCE;
    }
}
