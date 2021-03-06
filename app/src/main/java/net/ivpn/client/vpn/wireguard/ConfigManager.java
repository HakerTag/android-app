package net.ivpn.client.vpn.wireguard;

/*
 IVPN Android app
 https://github.com/ivpn/android-app
 <p>
 Created by Oleksandr Mykhailenko.
 Copyright (c) 2020 Privatus Limited.
 <p>
 This file is part of the IVPN Android app.
 <p>
 The IVPN Android app is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 3 of the License, or (at your option) any later version.
 <p>
 The IVPN Android app is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 details.
 <p>
 You should have received a copy of the GNU General Public License
 along with the IVPN Android app. If not, see <https://www.gnu.org/licenses/>.
*/

import com.wireguard.android.config.Config;
import com.wireguard.android.config.Peer;
import com.wireguard.android.model.Tunnel;

import net.ivpn.client.common.dagger.ApplicationScope;
import net.ivpn.client.common.prefs.ServerType;
import net.ivpn.client.common.prefs.ServersRepository;
import net.ivpn.client.common.prefs.Settings;
import net.ivpn.client.rest.data.model.Host;
import net.ivpn.client.rest.data.model.Server;
import net.ivpn.client.ui.protocol.port.Port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import javax.inject.Inject;

@ApplicationScope
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final String WIREGUARD_TUNNEL_NAME = "IVPN";
    private static final String DEFAULT_DNS = "172.16.0.1";

    private Tunnel tunnel;

    private Settings settings;
    private ServersRepository serversRepository;

    @Inject
    public ConfigManager(Settings settings, ServersRepository serversRepository) {
        this.settings = settings;
        this.serversRepository = serversRepository;
    }

    public void init() {
        LOGGER.info("init");
    }

    public void startWireGuard() {
        applyConfigToTunnel(generateConfig());
        tunnel.setState(Tunnel.State.UP);
    }

    public void stopWireGuard() {
        if (tunnel == null) {
            return;
        }

        tunnel.setState(Tunnel.State.DOWN);
    }

    private void applyConfigToTunnel(Config config) {
        tunnel = new Tunnel(WIREGUARD_TUNNEL_NAME, config, Tunnel.State.DOWN);
    }

    private Config generateConfig() {
        Port port = settings.getWireGuardPort();
        Server server = serversRepository.getCurrentServer(ServerType.ENTRY);
        return generateConfig(server, port);
    }

    private Config generateConfig(Server server, Port port) {
        Config config = new Config();
        String privateKey = settings.getWireGuardPrivateKey();
        String publicKey = settings.getWireGuardPublicKey();
        String ipAddress = settings.getWireGuardIpAddress();

        LOGGER.info("Generating config:");
        LOGGER.info("publicKey: = " + publicKey);
        LOGGER.info("ipAddress: = " + ipAddress);
        LOGGER.info("Server = " + server);
        if (server.getHosts() == null) {
            return null;
        }
        if (config.getInterface().getPublicKey() == null) {
            config.getInterface().setPrivateKey(privateKey);
        }
        String dnsString = getDNS(server);
        config.getInterface().setAddressString(ipAddress);
        config.getInterface().setDnsString(dnsString);

        ArrayList<Peer> peers = new ArrayList<>();
        Peer peer;
        for (Host host : server.getHosts()) {
            peer = new Peer();
            peer.setAllowedIPsString("0.0.0.0/0, ::/0");
            peer.setEndpointString(host.getHost() + ":" + port.getPortNumber());
            peer.setPublicKey(host.getPublicKey());
            peers.add(peer);
        }
        config.setPeers(peers);

        return config;
    }

    private String getDNS(Server server) {
        String dns = settings.getDNS();
        if (dns != null) {
            return dns;
        }
        if (server.getHosts() == null || server.getHosts().get(0) == null
                || server.getHosts().get(0).getLocalIp() == null) {
            return DEFAULT_DNS;
        }

        return server.getHosts().get(0).getLocalIp().split("/")[0];
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void onTunnelStateChanged(Tunnel.State state) {
        if (tunnel != null) {
            tunnel.setState(state);
        }
    }
}