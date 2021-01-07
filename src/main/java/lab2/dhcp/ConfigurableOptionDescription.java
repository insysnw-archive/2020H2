package lab2.dhcp;

public interface ConfigurableOptionDescription extends OptionDescription {
    String getName();

    @Override
    ConfigurableOption produce();
}
