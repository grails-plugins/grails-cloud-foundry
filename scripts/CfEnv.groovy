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

import org.cloudfoundry.client.lib.CloudApplication

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-env [--appname]
'''

target(cfEnv: 'List application environment variables') {
	depends cfInit

	doWithTryCatch {

		CloudApplication application = getApplication()
		if (!application.env()) {
			println '\nNo Environment Variables\n'
			return
		}

//		Map<String, String> env = application.env()
		// temp workaround for client jar bug until it's fixed
		Map<String, String> env = [:]
		for (String nameAndValue : application.env) {
			int index = nameAndValue.indexOf("=")
			if (index != -1) {
				String envName = nameAndValue.substring(0, index)
				String envValue = nameAndValue.substring(index + 1)
				env[envName] = envValue
			}
		}

		displayInBanner(['Variable', 'Value'], env,
			[{ it.key }, { it.value }], false)
	}
}

setDefaultTarget cfEnv
