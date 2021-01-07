package lab2.dhcp;

public interface OptionDescription {
    byte getType();
    Option produce();
}
