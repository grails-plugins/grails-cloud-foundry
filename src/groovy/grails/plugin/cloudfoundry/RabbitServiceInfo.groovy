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
 * @author Burt Beckwith
 */
class RabbitServiceInfo extends AbstractServiceInfo {

	// amqp://haeuwgmk:ygyGeOqIWu45OTPN@172.30.48.106:38032/ornhvqkp

	final String url
	final String userName
	final String virtualHost

	RabbitServiceInfo(Map<String, Object> serviceInfo) {
		super(parseUrl(serviceInfo.credentials.url, serviceInfo.plan, serviceInfo.name))

		url = serviceInfo.credentials.url
		userName = extractUsername(url)
		virtualHost = extractVirtualHost(url)
	}

	private static Map<String, Object> parseUrl(String url, String plan, String name) {
		URI uri = new URI(url)
		if (!'amqp'.equals(uri.getScheme())) {
			throw new IllegalArgumentException("wrong scheme in amqp URI: $url")
		}

		String host = uri.getHost()
		if (host == null) {
			throw new IllegalArgumentException("missing host in amqp URI: $url")
		}

		int port = uri.getPort()
		if (port == -1) {
			port = 5672
		}

		String userInfo = uri.getRawUserInfo()
		String password
		if (userInfo) {
			String[] userPass = userInfo.split(':')
			if (userPass.length != 2) {
				throw new IllegalArgumentException("bad user info in amqp URI: $url")
			}

			password = uriDecode(userPass[1])
		}
		else {
			password = 'guest'
		}

		[name: name, plan: plan, credentials: [hostname: host, port: port, password: password]]
	}

	@Override
	String toString() {
		"${super.toString()}, virtualHost: $virtualHost, userName: $userName, url: $url"
	}

	private static String extractVirtualHost(String url) {
		URI uri = new URI(url)
		String path = uri.getRawPath()
		if (path) {
			// Check that the path only has a single segment.
			// As we have an authority component in the URI, paths always begin with a slash.
			if (path.indexOf('/', 1) != -1) {
				throw new IllegalArgumentException("multiple segemtns in path of amqp URI: $url")
			}

			return uri.getPath().substring(1)
		}

		// The RabbitMQ default vhost
		return '/'
	}

	private static String extractUsername(String url) {
		String userInfo = new URI(url).getRawUserInfo()
		String userName
		if (userInfo) {
			String[] userPass = userInfo.split(':')
			if (userPass.length != 2) {
				throw new IllegalArgumentException("bad user info in amqp URI: $url")
			}

			return uriDecode(userPass[0])
		}

		return 'guest'
	}

	private static String uriDecode(String s) {
		try {
			// URLDecode decodes '+' to a space, as for form encoding.  So protect plus signs.
			return URLDecoder.decode(s.replace('+', '%2B'), 'US-ASCII')
		}
		catch (UnsupportedEncodingException e) {
			// US-ASCII is always supported
			throw new RuntimeException(e)
		}
	}
}
