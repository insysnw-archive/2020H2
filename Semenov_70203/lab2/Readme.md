# Dhcp Server

The Dynamic Host Configuration Protocol (DHCP) provides a framework
for passing configuration information to hosts on a TCPIP network.
Protocol is descripted in [RFC 2131](https://tools.ietf.org/html/rfc2131).

# Building
```bash
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build . -j4
```

If some problems happen during installation try to build within a docker container.
```bash
docker-compose up
```

# Running
You should assign a static IPv4 address to the interface you want to use.
As a root user:
```bash
ip link set up dev <device>
ip addr add <ip address>/<mask> dev <device>
```

You need the root privileges to use 67 port. The executable locates in `build/bin/dhcp_server`
```bash
./dhcp_server -i <assigned ip address>
```
You can specify some parameters such as dns server, gateway, mask and other.
To know more, run `./dhcp_server -h`
All ip addresses are written in \*\*\*.\*\*\*.\*\*\*.\*\*\* format including a subnet mask.
Range is presented as two ip addresses separated by semicolon.
