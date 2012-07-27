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

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-start [--appname]
'''

target(cfStart: 'Start an application') {
	depends cfInit

	doWithTryCatch {

		CloudApplication application = getApplication()

		if (application.state == AppState.STARTED) {
			event 'StatusFinal', ["Application '$application.name' is already running."]
			return
		}

		client.startApplication application.name

		int count = 0
		int logLinesDisplayed = 0

		event 'StatusUpdate', ["Trying to start Application: '$application.name'"]

		while (true) {
			displayPeriod()
			sleep 500
			try {
				if (appStartedProperly(count > 6)) {
					break
				}

				if (client.getCrashes(application.name).crashes) {
					event 'StatusError', ["ERROR - Application '$application.name' failed to start, logs information below."]

					for (log in CRASH_LOG_NAMES) {
						displayLog log, 0, false
					}

					if (isPush) {
						println ''
						if ('y'.equalsIgnoreCase(ask('Should I delete the application?', 'y,n', 'n'))) {
							deleteApplication false
						}
					}

					return
				}

				if (count > 29) {
					logLinesDisplayed = grabStartupTail(logLinesDisplayed)
				}
			}
			catch (IllegalArgumentException e) {
				throw e
			}
			catch (e) {
				print e.message
			}

			if (++count > 600) { // 5 minutes
				errorAndDie "Application is taking too long to start, check your logs"
			}
		}

		def urls = new StringBuilder()
		String delimiter = ''
		for (String uri in application.uris) {
			urls.append delimiter
			urls.append 'http://'
			urls.append uri
			delimiter = ', '
		}
		event 'StatusFinal', ["Application '$application.name' started at $urls"]
	}
}

boolean appStartedProperly(boolean errorOnHealth) {
	CloudApplication application = getApplication(getAppName(), true)
	if (!application) {
		errorAndDie "Application '${getAppName()}'s state is undetermined, not enough information available at this time."
	}

	String health = describeHealth(application)
	if ('RUNNING'.equals(health)) {
		boolean test = cfConfig.testStartWithGet instanceof Boolean ? cfConfig.testStartWithGet : true
		if (!test) {
			return true
		}

		String url = cfConfig.testStartGetUrl ?: 'http://' + application.uris[0]
		try {
			new URL(url).text
			return true
		}
		catch (IOException e) {
			return false
		}
	}

	if ('N/A'.equals(health)) {
		// Health manager not running.
		if (errorOnHealth) {
			errorAndDie "\nApplication '$application.name's state is undetermined, not enough information available at this time."
		}
	}

	false
}

int grabStartupTail(int since) {
	int newLines = 0
	try {
		String content = getFile(0, 'logs/startup.log')
		if (content) {
			if (since == 0) {
				println "\n==== displaying startup log ====\n\n"
			}
			def lines = content.readLines()
			def tail = lines[since, lines.size()]
			newLines = tail.size()
			tail.each { println it }
		}
	}
	catch (e) {
		log.warn "Problem retrieving startup.log: $e.message"
	}
	since + newLines
}

setDefaultTarget cfStart
