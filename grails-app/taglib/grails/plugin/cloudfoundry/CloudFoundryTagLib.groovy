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

import grails.converters.JSON
import grails.plugin.cloudsupport.AbstractCloudTagLib

/**
 * @author Burt Beckwith
 */
class CloudFoundryTagLib extends AbstractCloudTagLib {

	static namespace = 'cf'

	@Override
	protected Map findDbConnectInfo(String name) {
		// put in TreeMap to make order predictable for testing
		def servicesMap = new TreeMap(JSON.parse(System.getenv('VCAP_SERVICES')))

		for (entry in servicesMap) {
			String key = entry.key
			def services = entry.value
			String type
			String driver
			if (key.startsWith('mysql')) {
				type = 'mysql'
				driver = 'com.mysql.jdbc.Driver'
			}
			else if (key.startsWith('postgresql')) {
				type = 'postgresql'
				driver = 'org.postgresql.Driver'
			}
			else {
				continue
			}

			for (service in services) {
				if (name == null || name.equals(service.name)) {
					return [
						url: "jdbc:$type://$service.credentials.hostname:$service.credentials.port/$service.credentials.name",
						userName: service.credentials.user,
						password: service.credentials.password,
						driver: driver]
				}
			}
		}

		null
	}
}
