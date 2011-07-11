/* Copyright 2011 SpringSource.
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

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/CfRestart.groovy")

USAGE = '''
grails cf-env-del <variable> [--appname]
'''

target(cfEnvDel: 'Delete an environment variable from an application') {
	depends cfInit

	doWithTryCatch {

		String name = getRequiredArg()

		CloudApplication application = getApplication()
		Map<String, String> env = application.env()

		String old = env.remove(name)
		print "\nDeleting Environment Variable [$name]: "
		if (old) {
			client.updateApplicationEnv application.name, env
		}
		println 'OK\n'

		if (application.state == AppState.STARTED) {
			cfRestart()
		}
	}
}

setDefaultTarget cfEnvDel
