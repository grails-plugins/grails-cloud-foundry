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

import org.cloudfoundry.caldecott.client.TunnelHelper
import org.cloudfoundry.client.lib.CloudApplication
import org.cloudfoundry.client.lib.CloudFoundryException
import org.cloudfoundry.client.lib.CloudService
import org.cloudfoundry.caldecott.TunnelException

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/CfTunnelDisconnect.groovy")

USAGE = '''
grails cf-tunnel [--service] [--port]
'''

target(cfTunnel: 'Creates a tunnel to a service using the Caldecott client') {
	depends cfInit

	doWithTryCatch {

		Caldecott = classLoader.loadClass('grails.plugin.cloudfoundry.Caldecott')

		if (Caldecott.instance) {
			error 'You already have an active tunnel'
			return
		}

		boolean interactive = 'true' == System.getProperty('grails.interactive.mode.enabled') // Environment.INTERACTIVE_MODE_ENABLED

		String serviceName = getServiceName()
		if (!serviceName) {
			return
		}

		deployCaldecottAppIfNecessary()

		int port = (argsMap.port ?: '10000').toInteger()

		def tunnel = Caldecott.newInstance(serviceName, realClient, port)

		Map<String, String> tunnelInfo
		try {
			tunnelInfo = tunnel.connect()
		}
		catch (TunnelException e) {
			if (e.cause instanceof BindException) {
				error "Port $port is already in use; re-run the script with the --port option specifying an unused port"
				return
			}
		}

		addShutdownHook {
			disconnect()
		}

		String serviceUsername = tunnelInfo.username
		String servicePassword = tunnelInfo.password
		String serviceDbname = tunnelInfo.db ?: tunnelInfo.name
		String databasePropertyName = tunnelInfo.db ? 'db' : 'name'
		String serviceVhost = tunnelInfo.vhost

		String start = "Tunnel is running on $Caldecott.LOCAL_HOST port $port\nConnect your client with username=$serviceUsername password=$servicePassword"
		if (serviceVhost) {
			displayPermanent "$start vhost=$serviceVhost"
		}
		else {
			displayPermanent """$start $databasePropertyName=$serviceDbname
Example mysql client usage:
   mysql -h 127.0.0.1 -P $port -u $serviceUsername -p$servicePassword -D $serviceDbname"""
		}

		if (interactive) {
			displayPermanent "Run cf-tunnel-disconnect to close the current tunnel"
		}
		else {
			displayPermanent "Use CTRL-C to close the tunnel"
			// spin and wait for CTRL-C
			while (true) {
				sleep 100
			}
		}
	}
}

getServiceName = { ->

	List<CloudService> services = client.services
	if (!services) {
		error "You don't have any services defined"
		exit 1 // TODO
	}

	String name = argsMap.service
	if (name) {
		if (!services*.name.contains(name)) {
			error "The specified service '$name' isn't valid"
			return null
		}
		return name
	}

	displayPermanent 'You have the following services defined:'
	int i = 0
	for (CloudService service in services) {
		displayPermanent "${++i}: $service.name"
	}

	String answer = ask("\nPlease select a service (or 0 to quit):", (0..services.size()).join(','))
	int index = answer.toInteger()
	0 == index ? null : services[index - 1].name
}

deployCaldecottAppIfNecessary = { ->
	CloudApplication serverApp
	try {
		serverApp = client.getApplication(TunnelHelper.tunnelAppName)
	}
	catch (CloudFoundryException ignored) {
		// ignored
	}

	if (!serverApp) {
		displayPermanent 'Caldecott server app not found, deploying ...'
		TunnelHelper.deployTunnelApp realClient
	}

	try {
		serverApp = client.getApplication(TunnelHelper.tunnelAppName)
	}
	catch (CloudFoundryException e) {
		error "Unable to deploy Caldecott server app: $e.message"
		throw e
	}

	if (serverApp.state != CloudApplication.AppState.STARTED) {
		displayPermanent 'Starting Caldecott server app'
		client.startApplication(serverApp.name)
	}
}

setDefaultTarget cfTunnel
