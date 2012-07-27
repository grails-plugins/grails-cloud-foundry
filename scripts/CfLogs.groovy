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

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-logs [destination] [--appname] [--instance] [--stderr] [--stdout] [--startup]
'''

target(cfLogs: 'Display logs for an instance') {
	depends cfInit

	doWithTryCatch {

		String destination = validateStringValue(argsList[0])
		int instanceIndex = (validateString('instance') ?: 0).toInteger()

		boolean stderr = true
		boolean stdout = true
		boolean startup = true

		if (validateBoolean('stderr')) {
			stderr = argsMap.stderr
		}
		if (validateBoolean('stdout')) {
			stdout = argsMap.stdout
		}
		if (validateBoolean('startup')) {
			startup = argsMap.startup
		}

		int count = 0
		if (stderr) count++
		if (stdout) count++
		if (startup) count++
		if (destination && count > 1) {
			// TODO check if destination is a directory and write with original names
			errorAndDie 'Destination file is specified but more than one log file was requested'
		}

		if (stderr) {
			displayLog 'logs/stderr.log', instanceIndex, true, destination
		}

		if (stdout) {
			displayLog 'logs/stdout.log', instanceIndex, true, destination
		}

		if (startup) {
			displayLog 'logs/startup.log', instanceIndex, true, destination
		}
	}
}

setDefaultTarget cfLogs
