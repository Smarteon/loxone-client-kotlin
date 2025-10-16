# Ports and Domains

> **Note:** This document is automatically generated from the official Loxone PDF documentation.
> Last updated: 2025-10-16 11:06:00 UTC
> Source: https://www.loxone.com/wp-content/uploads/datasheets/Loxone_PortsDomains.pdf

---

Date:

Authors:



2025-06-03

Loxone


# Network-Communication: Required Ports & Domains

## Miniserver


   - DNS


      - UDP Port 53

   - mDNS


      - UDP 224.0.0.251:5353

   - Search/Discovery


      - mDNS

      - Upnp

      - UDP Port 7070-7071

   - Communication with App & Config


      - TCP Port 443

      - TCP Port [HTTP Port set in Miniserver settings]

   - FTP


      - TCP Port 21 (Control)

      - TCP Port 20 (Data)

   - Cloud - Outgoing connections


      - Weather Service


            - TCP/HTTP weather.loxone.com:6066

            - TCP/HTTPS weahter.loxonecloud.com:443

      - Caller Service

            - TCP caller.loxone.com:80/443

      - Cloud-Mailer


            - TCP mail.loxonecloud.com:443

      - Cloud-DNS


            - UDP dns.loxonecloud.com:7700

      - Push


            - TCP push.loxonecloud.com:443

      - Remote Connect


            - MQTT Connection


           - TCP *.ccbroker.loxonecloud.com:8443

            - SSH Tunneling Connection


           - SSH *.loxonecloud.com:22

           - Example: eu.ccd2.loxonecloud.com:22

            - Connections to Miniservers via Remote Connect


           - TCP *.dyndns.loxonecloud.com:[Port]

           - **Attention** : Port is assigned randomly! (20000-65000)


Date:

Authors:


        - Example: *.dyndns.loxonecloud.com:30357

        - Data Center Request


        - TCP api.loxonecloud.com:443

        - TCP secureapi.loxonecloud.com:443

        - HTTPS Certificate Request


        - ca.loxonecloud.com:443

   - Provisioning & Cloud Service Status


        - TCP api.loxonecloud.com:443

        - TCP secureapi.loxonecloud.com:443

- Debug-Monitor


   - UDP Port 7777 (Default)

   - Port can be set in Config

- Auto-Update


   - TCP update.loxone.com:80/443

   - TCP updatefiles.loxone.com:80/443

- Crash-Log Server


   - UDP log.loxone.com:7707

- BacNET


   - UDP+TCP Port 47808

 Client/Gateway


   - UDP 7070 – 7077

   - Broadcasts must be allowed

 KNX-IP (only Miniserver Gen.1)


   - IGMP Multicasts

 Blink synchronization of Network Devices


   - UDP Broadcast to 255.255.255.255:7079

- Online Check



2025-06-03

Loxone




      - The Miniserver regularly checks if he has internet by pinging addresses

      - ICMP

      - The addresses are checked sequentially: if the first one succeeds, the Miniserver assumes it has

internet, and no further addresses are checked. If the first one fails, the second is then checked.

      - Addresses:


            - dns.loxonecloud.com

            - icann.org

            - w3c.org

## Loxone Config


   - Auto-Update


      - TCP update.loxone.com:80/443

      - TCP updatefiles.loxone.com:80/443

   - Project Planning


      - Price List updates

      - TCP shop.loxone.com:443


Date:

Authors:



2025-06-03

Loxone


## App




- Local Connection to Miniserver


   - TCP Port 443

   - TCP Port [Defined HTTP Port]

- Remote Connection to Miniserver


   - TCP dns.loxonecloud.com:80/443

   - TCP *.dyndns.loxonecloud.com:[Port]


        - **Attention** : Port is assigned randomly! (20000-65000)

- Geo-Coordinates


   - TCP geo.loxone.com:443

- Loxone-Library


   - Update of Templates and Template-Index

   - TCP api.library.loxone.com:443

- Data Center Request


   - Determination which loxonecloud.com datacenter the config should use for connecting to the

Miniserver


        - TCP api.loxonecloud.com:443

        - TCP secureapi.loxonecloud.com:443

- Documentation


   - TCP loxone.com:80/443


- Local Connection to Miniserver


   - TCP Port 443

   - TCP Port [Defined HTTP Port]

- Detecting Remote Connection to Miniserver


   - TCP dns.loxonecloud.com:80/443

- Remote connection via Remote Connect


   - TCP *.dyndns.loxonecloud.com:[Port]

   - **Attention** : Port is assigned randomly! (20000-65000)

   - Firewalls may not allow domain based rules and the IPs of the servers behind the domain are not

fixed per MS, they are assigned to the MS on the connection attempt:


        - [195.201.222.243](http://195.201.222.243/) / 2a01:4f8:1c1c:57c8::1

        - [168.119.185.175](http://168.119.185.175/) / 2a01:4f8:c010:2f7::1

        - **please beware:** they will change at some point in 2023 to the following


        - [5.75.128.138](http://5.75.128.138/) / 2a01:4f8:1c1c:cbb4::1

        - [88.99.85.148](http://88.99.85.148/) / 2a01:4f8:c012:494f::1

- Remote connection via CloudDNS


   - TCP [IP]:[Port]

   - **Attention** : Both the IP and the Port are dynamically provided by dns.loxonecloud.com, as

configured on the Miniserver.

- Miniserver Search


   - UDP Port 7070-7071

   - Upnp


Date:

Authors:



2025-06-03

Loxone




   - Auto-Update


      - TCP update.loxone.com:80/443

      - TCP updatefiles.loxone.com:80/443

   - News/Infos/Device-Documentation


      - TCP loxone.com:80/443

   - Air/Tree Pairing- and Battery-Device-Information


      - TCP extended-app-content.s3.eu-central-1.amazonaws.com:443

   - Push Service


      - TCP push.loxonecloud.com:443

   - SIP (Intercom Audio)


      - UDP Ports


            - 5060 (default)

## Audioserver


   - Search/Discovery


      - mDNS

      - Upnp

      - UDP Ports 7070-7071

   - Communication with Miniserver


      - TCP Port 7095

      - TCP Port 80

      - TCP Port 443

   - Communication with App


      - TCP Port 7091

   - Communication AudioStreams Audioserver <-> Audioserver


      - UDP Port 7788

      - UDP Ports 14000 - 14999

   - General Communication Audioserver <-> Audio Extensions


      - Boot & NFS TCP + UDP 111,

      - Boot II TCP + UDP 2049

   - Exception for internet radio streams:


      - All listening and request ports from / to radio stream stations are assigned dynamically,

Radio station provider assigns the http / TCP streaming ports


   - Apple AirPlay


      - TCP Ports 7000-7004 AirPlay streaming data

      - TCP/UDP Ports 49152-65535 Random high ports for dynamic sessions during streaming.

## Loxone Intercom


   - Search/Discovery


      - mDNS


Date:

Authors:



2025-06-03

Loxone




      - Upnp

      - UDP Port 7070-7071

   - Communication with Miniserver


      - TCP Port 7091

   - Communication with App


      - TCP Port 7091

      - Video/Audio: UDP Ports are assigned dynamically

      - External Video/Audio: UDP stun.loxonecloud.com:3478


            - The Loxone STUN server is used to reflect the public IP to the Intercom which then can be


used as connection candidate for video/audio connection.

   - Auto-Update


      - TCP update.loxone.com:80/443

      - TCP updatefiles.loxone.com:80/443

## Intercom Gen.1


   - Search/Discovery


      - UDP Port 5000

      - UDP Port 8110 (Video-Module)

      - UDP Port 8112 (Audio-Module)

   - Communication with Miniserver


      - UDP Port 8110 (Video-Module)

      - UDP Port 8111 (Video-Module)

      - UDP Port 8112 (Audio-Module)

      - UDP Port 8113 (Audio-Module)

   - Communication with App


      - TCP Port 80


