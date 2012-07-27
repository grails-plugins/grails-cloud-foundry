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
package grails.plugin.cloudfoundry

/**
 * Utility methods.
 *
 * @author Burt Beckwith
 */
class CloudFoundryUtils {

	static String uptimeString(double delta) {
		int seconds = delta.toInteger()
		int days = seconds / (60 * 60 * 24)
		seconds -= days * (60 * 60 * 24)
		int hours = seconds / (60 * 60)
		seconds -= hours * (60 * 60)
		int minutes = seconds / 60
		seconds -= minutes * 60
		"${days}d:${hours}h:${minutes}m:${seconds}s"
	}
}
