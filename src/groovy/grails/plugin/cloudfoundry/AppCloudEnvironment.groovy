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

import grails.util.GrailsUtil

import org.codehaus.jackson.map.ObjectMapper

/**
 * Provides access to the environment variables that are provided at runtime.
 *
 * @author Burt Beckwith
 */
class AppCloudEnvironment {

	private final ObjectMapper objectMapper = new ObjectMapper()
	private final Map<String, Class> infoClassesByVendor = [
		redis: RedisServiceInfo,
		mongodb: MongoServiceInfo,
		mysql: MysqlServiceInfo,
		postgresql: PostgresqlServiceInfo,
		rabbitmq: RabbitServiceInfo]

	boolean isAvailable() {
		System.getenv('VCAP_SERVICES') != null
	}

	String getHost() { System.getenv('VCAP_APP_HOST') }

	Integer getPort() { System.getenv('VCAP_APP_PORT')?.toInteger() }

	ApplicationInstanceInfo getInstanceInfo() {
		isAvailable() ? new ApplicationInstanceInfo(parseEnv('VCAP_APPLICATION', Map)) : null
	}

	AbstractServiceInfo getServiceByName(String name) {
		for (List<Map<String, Object>> services : getServices()) {
			for (Map<String, Object> service : services) {
				if (service.name.equals(name)) {
					return newService(service)
				}
			}
		}
	}

	AbstractServiceInfo getServiceByVendor(String vendor) {
		for (List<Map<String, Object>> services : getServices()) {
			for (Map<String, Object> service : services) {
				if (service.label.startsWith(vendor)) {
					return newService(service)
				}
			}
		}
	}

	private List<Map<String, Object>> getServices() {
		if (!isAvailable()) {
			return []
		}

		Map<String, List<Map<String, Object>>> rawServices = parseEnv('VCAP_SERVICES', Map)
		rawServices.values() as List
	}

	private AbstractServiceInfo newService(Map<String, Object> serviceData) {
		for (Map.Entry<String, Class> entry : infoClassesByVendor) {
			if (serviceData.label.startsWith(entry.key)) {
				return entry.value.newInstance(serviceData)
			}
		}
	}

	private <T> T parseEnv(String varName, Class<T> clazz) {
		try {
			objectMapper.readValue(System.getenv(varName), clazz)
		}
		catch (e) {
			println "Problem parsing env var $varName: $e.message"
			System.getenv().each { String key, String value ->
				if (key.startsWith('VCAP_') || key.startsWith('VMC_')) {
					println "$key: $value"
				}
			}
			GrailsUtil.deepSanitize e
			throw e
		}
	}
}
