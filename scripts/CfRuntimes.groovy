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

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-runtimes
'''

target(cfRuntimes: 'Display the supported runtimes of the target system') {
	depends cfInit

	doWithTryCatch {

		CloudInfo cloudInfo = client.getCloudInfo()

		displayInBanner(['Name', 'Description', 'Version'], cloudInfo.runtimes.sort { it.name },
			[{ it.name }, { it.description }, { it.version }], false)
	}
}

setDefaultTarget cfRuntimes
