package lab2.dns.model;

/**
 * {@link #A} query for IPv4 address
 * {@link #MX} check for the mail exchanger of a domain
 * {@link #NS} query for the name servers responsible for a domain
 * {@link #CNAME} check if the looked up domain is an alias
 * {@link #OTHER} query for IPv4 address
 */
public enum QueryType {
    A,
    MX,
    NS,
    CNAME,
    OTHER
}
