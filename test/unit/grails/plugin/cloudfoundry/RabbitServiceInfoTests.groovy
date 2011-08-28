package grails.plugin.cloudfoundry

import grails.test.GrailsUnitTestCase

class RabbitServiceInfoTests extends GrailsUnitTestCase {

	private static final String USERNAME = 'haeuwgmk'
	private static final String PASSWORD = 'ygyGeOqIWu45OTPN'
	private static final String HOST = '172.30.48.106'
	private static final String VIRTUALHOST = 'ornhvqkp'
	private static final int PORT = 38032
	private static final String VALID_URL = 'amqp://' + USERNAME + ':' + PASSWORD + '@' + HOST + ':' + PORT + '/' + VIRTUALHOST
	private static final String PLAN = 'the plan'
	private static final String NAME = 'the name'

	void testValidUrl() {
		RabbitServiceInfo info = new RabbitServiceInfo([plan: PLAN, name: NAME, credentials: [url: VALID_URL]])
		assertEquals USERNAME, info.userName
		assertEquals VALID_URL, info.url
		assertEquals VIRTUALHOST, info.virtualHost
		assertEquals PLAN, info.plan
		assertEquals HOST, info.host
		assertEquals PASSWORD, info.password
		assertEquals PORT, info.port
		assertEquals NAME, info.serviceName
	}

	void testInvalidScheme() {
		shouldFail(IllegalArgumentException) {
			new RabbitServiceInfo([plan: PLAN, name: NAME, credentials: [url: 'http://grails.org']])
		}
	}
}
