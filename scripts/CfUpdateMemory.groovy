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
import org.cloudfoundry.client.lib.CloudApplication.AppState

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/CfRestart.groovy")

USAGE = '''
grails cf-update-memory <memsize> [--appname]
'''

target(cfUpdateMemory: 'Update the memory reservation for an application') {
	depends cfInit

	doWithTryCatch {

		String memory = getRequiredArg()

		CloudApplication application = getApplication()

		int requested = memoryToMegs(memory)

		if (requested - application.memory > 0) {
			checkHasCapacityFor requested - application.memory
		}

		if (requested == application.memory) {
			println "\nApplication '$application.name' has already allocated ${memory}.\n"
			return
		}

		client.updateApplicationMemory application.name, requested
		println "\nUpdated memory reservation to '${prettySize(requested * 1024 * 1024)}'."
		if (application.state == AppState.STARTED) {
			cfRestart()
		}
	}
}

setDefaultTarget cfUpdateMemory
