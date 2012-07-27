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
grails cf-env-add <variable> <value> [--appname]
'''

target(cfEnvAdd: 'Add an environment variable to an application') {
	depends cfInit

	doWithTryCatch {

		// can't use argsList/argsMap since it doesn't handle quoted args with spaces

		def parsed = args.split('\n').collect { it.trim() }
		if (parsed.size() < 2) {
			println "\nUsage (optionals in square brackets):\n$USAGE"
			throw new IllegalArgumentException()
		}

		String name = parsed.remove(0)
		String value = parsed.join(' ')

		CloudApplication application = getApplication()
		Map<String, String> env = application.envAsMap

		env[name] = value
		print "Adding Environment Variable [$name=$value]: "
		client.updateApplicationEnv application.name, env
		println 'OK'

		if (application.state == AppState.STARTED) {
			cfRestart()
		}
	}
}

setDefaultTarget cfEnvAdd
