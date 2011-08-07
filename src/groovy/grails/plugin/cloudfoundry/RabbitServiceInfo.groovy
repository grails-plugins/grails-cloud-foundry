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
package grails.plugin.cloudfoundry

/**
 * @author Burt Beckwith
 */
class RabbitServiceInfo extends AbstractServiceInfo {

	//amqp://haeuwgmk:ygyGeOqIWu45OTPN@172.30.48.106:38032/ornhvqkp
	static final String URL_REGEX = 'amqp://(\\w+):(\\w+)@([\\d\\.]+):(\\d+)/(\\w+)'

	final String url
	final String userName
	final String virtualHost

	RabbitServiceInfo(Map<String, Object> serviceInfo) {
		super(parseUrl(serviceInfo.credentials.url, serviceInfo.plan, serviceInfo.name))
		url = serviceInfo.credentials.url

		def matcher = url =~ URL_REGEX
		userName = matcher[0][1]
		virtualHost = matcher[0][5]
	}

	private static Map<String, Object> parseUrl(String url, String plan, String name) {
		def matcher = url =~ URL_REGEX
		[name: name, plan: plan, credentials:
			[hostname: matcher[0][3], port: matcher[0][4].toInteger(), password: matcher[0][2]]]
	}

	@Override
	String toString() {
		"${super.toString()}, virtualHost: $virtualHost, userName: $userName, url: $url"
	}
}
