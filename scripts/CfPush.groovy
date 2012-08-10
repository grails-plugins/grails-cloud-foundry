/* Copyright 2011-2012 SpringSource.
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

import org.cloudfoundry.client.lib.CloudApplication
import org.cloudfoundry.client.lib.CloudFoundryException
import org.cloudfoundry.client.lib.CloudService
import org.cloudfoundry.client.lib.ServiceConfiguration
import org.cloudfoundry.client.lib.UploadStatusCallback

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/CfStart.groovy")
includeTargets << grailsScript('_GrailsWar')

USAGE = '''
grails [environment] cf-push [--appname] [--url] [--memory] [--warfile] [--services] [--no-start]
'''

target(cfPush: '''Push and optionally start an application.
If the war file is not specified a temporary one will be created''') {

	depends cfInit, loadPlugins

	isPush = true

	doWithTryCatch {

		if (!checkDevelopmentEnvironment()) {
			return
		}

		File warfile = buildWar()

		boolean start = !validateBoolean('no-start')

		String memory = validateString('memory') ?: '512M'

		int megs = memoryToMegs(memory)

		String appName = getAppName()
		String url
		String domain = cfTarget.split('\\.')[1..-1].join('.') // cloudfoundry.com
		if (validateString('url')) {
			url = argsMap.url
		}
		else {
			String suggestUrl = appName + '.' + domain
			String response = ask("\nApplication Deployed URL: '$suggestUrl'? ", null, suggestUrl)
			response = (response ?: '').trim()
			if (!response || 'y'.equals(response.toLowerCase())) {
				url = suggestUrl
			}
			else {
				url = response.trim()
			}
		}
		if (!url.contains('.')) {
			url += '.' + domain
		}

		def serviceNames = validateString('services') ? argsMap.services.split(',')*.trim() : []

		List<ServiceConfiguration> serviceConfigurations = client.getServiceConfigurations()
		List<CloudService> services = client.getServices()

		def serviceInfo = [hibernate: ['mysql', 'postgresql'], mongodb: ['mongodb'],
		                   redis: ['redis'], cacheRedis: ['redis'], rabbitmq: ['rabbitmq']]
		serviceInfo.each { String pluginName, List<String> vendors ->
			if (pluginManager.hasGrailsPlugin(pluginName)) {
				if (!checkBindService(services, vendors, serviceNames)) {
					checkCreateService services, serviceConfigurations, vendors, serviceNames
				}
			}
		}

		displayStatusMsg "Creating application $appName at $url with ${megs}MB and ${serviceNames ? 'services ' + serviceNames : 'no bound services'}:"

		try {
			client.createApplication appName, CloudApplication.GRAILS, megs, [url], serviceNames ?: null, true
		}
		catch (CloudFoundryException e) {
			if (e.statusCode.value() == 400 && e.description.contains('has already been taken or reserved')) {
				event 'StatusError', [e.description]
				return
			}
			else {
				throw e
			}
		}
		displayStatusResult " OK"

		event 'StatusUpdate', ['Uploading Application:']
		displayStatusMsg '  Checking for available resources:'

		def callback = new UploadStatusCallback() {
			void onCheckResources() {
				displayStatusResult ' OK'
				displayStatusMsg '  Processing resources:'
			}

			void onMatchedFileNames(Set<String> matchedFileNames) {
				displayStatusResult ' OK'
				displayStatusMsg '  Packing application:'
			}

			void onProcessMatchedResources(int length) {
				displayStatusResult ' OK'
				displayStatusMsg "  Uploading (${prettySize(length, 0)}):"
			}
		}

		client.uploadApplication appName, warfile, callback
		displayStatusResult ' OK'

		if (start) {
			argsList.clear()
			argsMap.appname = appName
			cfStart()
		}
		else {
			event 'StatusFinal', 'Push Status: OK'
		}
	}
}

CloudService findService(List<CloudService> services, String vendor) {
	services.find { it.vendor == vendor }
}

boolean checkBindService(List<CloudService> services, List<String> vendors, List<String> serviceNames) {
	boolean atLeastOne = false
	for (String vendor in vendors) {
		CloudService service = findService(services, vendor)
		if (service && !serviceNames.contains(service.name)) {
			String answer = ask("\nWould you like to bind the '$service.name' service?", 'y,n', 'y')
			if ('y'.equalsIgnoreCase(answer)) {
				serviceNames << service.name
				atLeastOne = true
			}
		}
	}
	return atLeastOne
}

void checkCreateService(List<CloudService> services, List<ServiceConfiguration> serviceConfigurations,
                        List<String> vendors, List<String> serviceNames) {

	for (String vendor in vendors) {
		CloudService service = findService(services, vendor)
		if (service) {
			return
		}
	}

	for (String vendor in vendors) {
		String answer = ask("\nWould you like to create and bind a $vendor service?", 'y,n', 'y')
		if ('y'.equalsIgnoreCase(answer)) {
			serviceNames << createService(serviceConfigurations.find { it.vendor == vendor })
		}
	}
}

setDefaultTarget cfPush
