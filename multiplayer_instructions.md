# Crazy Eights Multiplayer Guide

Crazy Eights supports multiplayer and allows players to host and join rooms and play together online. This section explains how to create and join a room.

You have two options. You could **play together under the same Local Area Network (LAN)**, or **play together over the Internet**.

## Playing Together Under the Same LAN (i.e. same WiFi)

If all players are playing under the same LAN (i.e. connected to the same WiFi router), then choose a host to create a room and other players can simply join the room using the same room code.

## Playing Together Over the Internet

To play together over the Internet, more setup is needed. You could set up port forwarding, but a fast and easy way to connect is by using ZeroTier. ZeroTier allows you to join networks that act just like LANs. I have set up a public ZeroTier network that you can join for convenience. However, if you are concerned with privacy, you could create and join your own ZeroTier networks, but that will not be covered in this section.

Every player including the host must have ZeroTier installed and have joined the same network.

### Windows, macOS and Linux
1) Go to https://www.zerotier.com/download/ to download ZeroTier for your operating system.
2) Follow the instructions in https://zerotier.atlassian.net/wiki/spaces/SD/pages/6848513/Join+a+Network. If you are on Windows or macOS, enter **e4da7455b207a747** in the network ID field and hit "Join". If you are on Linux, use the following command\
`sudo zerotier-cli join e4da7455b207a747`

### Android 
1) Download and install ZeroTier One from the Google Play Store: https://play.google.com/store/apps/details?id=com.zerotier.one
2) Tap the + button on the top right
3) Enter **e4da7455b207a747** in the network ID field and tap "Add Network"

After all players have installed ZeroTier and connected to the same ZeroTier network, the host can now create a room and have other players join using the same room code.
