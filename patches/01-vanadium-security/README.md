# 01-vanadium-security

## Overview
This patch series ports security and anti-exploitation hardening from GrapheneOS's Vanadium browser to Helium Android:
1. `0001-disable-v8-jit-by-default.patch`: Disables V8 Just-In-Time compiler in renderer processes by default. Mitigates the vast majority of in-the-wild Chromium zero-days. Provides a switch in `HeliumPreferences` for users needing high JS benchmark performance on trusted sites.
2. `0002-webrtc-ip-handling-policy.patch`: Enforces `disable_non_proxied_udp` on WebRTC connections, guaranteeing private network IPs are never exposed to remote endpoints via STUN/TURN binding requests.
