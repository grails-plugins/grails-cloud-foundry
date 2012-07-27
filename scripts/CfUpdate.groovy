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
import org.cloudfoundry.client.lib.UploadStatusCallback

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/CfRestart.groovy")
includeTargets << grailsScript('_GrailsWar')

USAGE = '''
grails [environment] cf-update [--appname] [--warfile]
'''

target(cfUpdate: '''Update an application.
If the war file is not specified a temporary one will be created''') {

	depends cfInit

	doWithTryCatch {

		if (!checkDevelopmentEnvironment()) {
			return
		}

		CloudApplication application = getApplication()

		File warfile = buildWar()

		event 'StatusUpdate', ['Updating Application:']
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

		client.uploadApplication application.name, warfile, callback
		displayStatusResult ' OK'

		if (application.state == AppState.STARTED) {
			argsList.clear()
			cfRestart()
		}
	}
}

setDefaultTarget cfUpdate
