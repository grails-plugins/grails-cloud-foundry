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
import com.vmware.appcloud.client.CloudApplication.AppState
import com.vmware.appcloud.client.UploadStatusCallback

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

		println '\nUpdating Application:'
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

		client.uploadApplication application.name, warfile, callback
		println ' OK'

		if (application.state == AppState.STARTED) {
			argsList.clear()
			cfRestart()
		}
	}
}

setDefaultTarget cfUpdate