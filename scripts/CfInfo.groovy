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

import org.cloudfoundry.client.lib.CloudInfo
import org.cloudfoundry.client.lib.CloudInfo.Limits
import org.cloudfoundry.client.lib.CloudInfo.Usage

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-info
'''

target(cfInfo: 'Show usage information') {
	depends cfInit

	doWithTryCatch {

		CloudInfo cloudInfo = client.getCloudInfo()

		println "\n$cloudInfo.description"
		println "For support visit $cloudInfo.support\n"
		println "Target:   $cloudControllerUrl (v$cloudInfo.version)"
		if (cloudInfo.user) {
			println "\nUser:     $cloudInfo.user"
		}
		Usage usage = cloudInfo.usage
		Limits limits = cloudInfo.limits
		if (usage && limits) {
			String totalMemory = prettySize(limits.maxTotalMemory.toLong() * 1024 * 1024)
			String usedMemory = prettySize(usage.totalMemory.toLong() * 1024 * 1024)

			println "Usage:    Memory   ($usedMemory of $totalMemory total)"
			println "          Services ($usage.services of $limits.maxServices total)"
			if (limits.maxApps) {
				println "          Apps     (${usage.apps ?: 0} of ${limits.maxApps ?: 0} total)"
			}
		}
		println ''
	}
}

setDefaultTarget cfInfo
