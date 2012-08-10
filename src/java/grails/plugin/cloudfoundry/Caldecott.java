/* Copyright 2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.cloudfoundry;

import java.net.InetSocketAddress;
import java.util.Map;

import org.cloudfoundry.caldecott.client.HttpTunnelFactory;
import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.caldecott.client.TunnelServer;
import org.cloudfoundry.client.lib.CloudFoundryClient;

/**
 * @author Thomas Risberg
 * @author Burt Beckwith
 */
public class Caldecott {

	private static final String LOCAL_HOST = "localhost";

	private final String serviceName;
	private final int port;
	private final CloudFoundryClient client;

	private TunnelServer server;

	/**
	 * Singleton instance (only used in interactive mode).
	 */
	public static Caldecott instance;

	/**
	 * Constructor.
	 * @param serviceName the name of the service to tunnel to
	 * @param client a connected client
	 * @param port the requested local port
	 */
	public Caldecott(final String serviceName, final CloudFoundryClient client, final Integer port) {
		this.client = client;
		this.serviceName = serviceName;
		this.port = port;
	}

	/**
	 * Connect to the remote service.
	 * @return the tunnel service info
	 */
	protected Map<String, String> connect() {

		TunnelHelper.bindServiceToTunnelApp(client, serviceName);

		Map<String, String> info = TunnelHelper.getTunnelServiceInfo(client, serviceName);
		String tunnelAuth = TunnelHelper.getTunnelAuth(client);

		server = new TunnelServer(new InetSocketAddress(LOCAL_HOST, port),
				new HttpTunnelFactory(TunnelHelper.getTunnelUri(client), info.get("hostname"),
						Integer.valueOf(info.get("port")), tunnelAuth));
		server.start();

		info.put("tunnelAuth", tunnelAuth);

		instance = this;

		return info;
	}

	/**
	 * Disconnect.
	 */
	public void close() {

		instance = null;

		if (server != null) {
			try { server.stop(); }
			catch (Exception ignored) { /*ignored*/ }
		}

		if (client != null) {
			try { client.logout(); }
			catch (Exception ignored) { /*ignored*/ }
		}
	}
}
