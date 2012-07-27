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

import org.cloudfoundry.client.lib.CloudService
import org.cloudfoundry.client.lib.ServiceConfiguration

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-services
'''

target(cfServices: 'Displays a list of services available') {
	depends cfInit

	doWithTryCatch {

		println '\n============== System Services =============='

		displayInBanner(['Service', 'Version', 'Description'], client.getServiceConfigurations(),
			[{ it.vendor }, { it.version }, { it.description }], false)

		List<CloudService> services = client.getServices()

		println '=========== Provisioned Services ============'
		if (!services) {
			println 'None provisioned yet.\n'
			return
		}

		displayInBanner(['Name', 'Service'], services,
			[{ it.name }, { it.vendor }], false)
	}
}

setDefaultTarget cfServices
