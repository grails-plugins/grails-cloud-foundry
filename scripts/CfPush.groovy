/* Copyright 2011 the original author or authors.
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

import com.vmware.appcloud.client.CloudApplication
import com.vmware.appcloud.client.CloudService
import com.vmware.appcloud.client.ServiceConfiguration
import com.vmware.appcloud.client.UploadStatusCallback

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

		boolean start = !argsMap['no-start']

		String memory = argsMap.memory ?: '512M'

		int megs = memoryToMegs(memory)
		checkValidSelection megs

		String appName = getAppName()
		String url
		if (argsMap.url) {
			url = argsMap.url
		}
		else {
			String suggestUrl = cfTarget.split('\\.')[1..-1].join('.') // cloudfoundry.com
			url = ask("\nApplication Deployed URL: '${appName}.$suggestUrl'? ", null, "${appName}.$suggestUrl")
		}

		def serviceNames = argsMap.services ? argsMap.services.split(',')*.trim() : []

		List<ServiceConfiguration> serviceConfigurations = client.serviceConfigurations
		List<CloudService> services = client.services

		[hibernate: 'mysql', mongodb: 'mongodb', redis: 'redis'].each { String pluginName, String vendor ->
			if (pluginManager.hasGrailsPlugin(pluginName)) {
				if (!checkBindService(services, vendor, serviceNames)) {
					checkCreateService services, serviceConfigurations, vendor, serviceNames
				}
			}
		}

		print "\nCreating application $appName at $url with ${megs}MB and ${serviceNames ? 'services ' + serviceNames : 'no bound services'}:"
		client.createApplication appName, CloudApplication.GRAILS, megs, [url], serviceNames ?: null, true
		println " OK\n"

		println 'Uploading Application:'
		print '  Checking for available resources:'

		def callback = new UploadStatusCallback() {
			void onCheckResources() {
				println ' OK'
				print '  Processing resources:'
			}

			void onMatchedFileNames(Set<String> matchedFileNames) {
				println ' OK'
				print '  Packing application:'
			}

			void onProcessMatchedResources(int length) {
				println ' OK'
				print "  Uploading (${prettySize(length, 0)}):"
			}
		}

		client.uploadApplication appName, warfile, callback
		println ' OK'

		if (start) {
			argsList.clear()
			argsMap.appname = appName
			cfStart()
		}
		else {
			println 'Push Status: OK'
		}
	}
}

CloudService findService(List<CloudService> services, String vendor) {
	services.find { it.vendor == vendor }
}

boolean checkBindService(List<CloudService> services, String vendor, List<String> serviceNames) {
	CloudService service = findService(services, vendor)
	if (service && !serviceNames.contains(service.name)) {
		String answer = ask("\nWould you like to bind the '$service.name' service?", 'y,n', 'y')
		if ('y'.equalsIgnoreCase(answer)) {
			serviceNames << service.name
			return true
		}
	}
	false
}

void checkCreateService(List<CloudService> services, List<ServiceConfiguration> serviceConfigurations,
                        String vendor, List<String> serviceNames) {

	CloudService service = findService(services, vendor)
	if (service) {
		return
	}

	String answer = ask("\nWould you like to create and bind a $vendor service?", 'y,n', 'y')
	if ('n'.equalsIgnoreCase(answer)) {
		return
	}

	serviceNames << createService(serviceConfigurations.find { it.vendor == vendor })
}

setDefaultTarget cfPush
