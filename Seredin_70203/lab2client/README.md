# NTP client

``` bash 
$python3 ntp.py
```
---

## Description

**Package**
![Markdown Logo](https://circuits4you.com/wp-content/uploads/2018/01/ntp-udp-packet-format.png)
1. **Leap indicator** indicates whether the last minute of the current day is to have a leap second applied.
2. **Version number** the NTP version number.
3. **Mode** indicates the mode.
4. **Stratum** the stratum level of the system clock.
5. **Poll** the maximum interval between successive messages, in seconds to the nearest power of two.
6. **Precision** the precision of the system clock, in log2 seconds.
7. **Root delay** fixed-point number indicating the total round-trip delay to the reference clock.
8. **Root dispersion**  fixed-point number indicating the maximum error relative to the reference clock.
9. **Reference identifier** watch ID.
10. **Reference Timestamp** is the time the system clock was last set or corrected.
11. **Originate Timestamp** is the time at which the request departed the client for the server.
12. **Receive Timestamp**  is the time at which the client request arrived at the server.
13. **Transmit Timestamp** is the time at which the server reply departed the server.
The client generates an NTP-packet(version_number(1-4), mode(client=3) and transmit(client's local time)).
---
