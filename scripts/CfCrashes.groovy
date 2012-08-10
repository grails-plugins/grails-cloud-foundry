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
import org.cloudfoundry.client.lib.CrashInfo

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-crashes [--appname]
'''

target(cfCrashes: 'List recent application crashes') {
	depends cfInit

	doWithTryCatch {

		CloudApplication application = getApplication()

		List<CrashInfo> crashes = client.getCrashes(application.name).crashes

		if (!crashes) {
			println "\nNo crashed instances for [${application.name}]\n"
			return
		}

		crashes = ([] + crashes).sort { it.since }

		def data = []
		crashes.eachWithIndex { CrashInfo crash, index ->
			data << [name: "$application.name-${index + 1}",
			         instance: crash.instance,
			         date: formatDate(crash.since)]
		}

		displayInBanner(['Name', 'Instance ID', 'Crashed Time'], data,
			[{ it.name }, { it.instance }, { it.date }], false)
	}
}

setDefaultTarget cfCrashes
