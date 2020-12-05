# Dns resolver
This script resolves ip addresses of a domain name.

# Protocol
This realization is based on [RFC 1035](https://tools.ietf.org/html/rfc1035).
The client sends a packet over UDP with this structure:
```
    +---------------------+
    |        Header       |
    +---------------------+
    |       Questions     |
    +---------------------+
```
Then the client receives and parses a packet with this structure:
```
    +---------------------+
    |        Header       |
    +---------------------+
    |       Questions     |
    +---------------------+
    |        Answers      |
    +---------------------+
```
More info about each section composition, you may know from RFC.

# Usage
```bash
python3 dns_client.py <domain_name>
```
By default, dns servers are chosen from `/etc/resolv.conf`, but you may
define server explicitly using `-s` or `--server` argument.

To know more about received packet, you should add `-v` or `--verbose` flag.

For example:
```bash
python3 dns_client.py --server 1.0.0.1 --verbose github.com
```
