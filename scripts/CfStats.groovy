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
import org.cloudfoundry.client.lib.InstanceStats

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-stats [--appname]
'''

target(cfStats: 'Report resource usage for an application') {
	depends cfInit

	doWithTryCatch {

		CloudApplication application = getApplication()

		def uptimeString = { double delta ->
			int seconds = delta.toInteger()
			int days = seconds / (60 * 60 * 24)
			seconds -= days * (60 * 60 * 24)
			int hours = seconds / (60 * 60)
			seconds -= hours * (60 * 60)
			int minutes = seconds / 60
			seconds -= minutes * 60
			"${days}d:${hours}h:${minutes}m:${seconds}s"
		}

		displayInBanner(['Instance', 'CPU (Cores)', 'Memory (limit)', 'Disk (limit)', 'Uptime'],
			client.getApplicationStats(application.name).records,
			[{ it.id }, { "${it.usage.cpu ?: 'NA'}% (${it.cores})" },
			 { "${prettySize(it.usage.mem.toLong() * 1024)} (${prettySize(it.memQuota, 0)})" },
			 { "${prettySize(it.usage.disk)} (${prettySize(it.diskQuota, 0)})" },
			 { uptimeString(it.uptime) }],
			false)
	}
}

setDefaultTarget cfStats
