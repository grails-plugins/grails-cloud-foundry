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

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-update-instances <number> [--appname]
'''

target(cfUpdateInstances: 'Scale up or down the number of instances') {
	depends cfInit

	doWithTryCatch {

		int instances = getRequiredArg().toInteger()

		CloudApplication application = getApplication()

		if (instances < 1) {
			errorAndDie "There must be at least 1 instance."
		}

		def displayInstances = { -> instances == 1 ? '1 instance' : "$instances instances" }

		if (application.instances == instances) {
			println "\nApplication '$application.name' is already running ${displayInstances}.\n"
			return
		}

		int currentInstances = application.instances

		client.updateApplicationInstances application.name, instances

		println "\nScaled '$application.name' ${instances > currentInstances ? 'up' : 'down'} to ${displayInstances}.\n"
	}
}

setDefaultTarget cfUpdateInstances
