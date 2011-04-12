package grails.plugin.cloudfoundry

import grails.test.GrailsUnitTestCase

class AppCloudEnvironmentTests extends GrailsUnitTestCase {

	private AppCloudEnvironment env = new AppCloudEnvironment()

	private Map environment = [
		VCAP_APPLICATION: '{"instance_id":"549c04e33c8668ca122afff7a6091d5a","instance_index":0,"name":"grailstest","uris":["grailstest.cloudfoundry.com"],"users":["beckwithb@vmware.com"],"version":"3404634afe7a21b3d666a6e48d32213a302b935a-1","start":"2011-04-08 17:42:12 +0000","runtime":"java","state_timestamp":1302284532,"port":40954,"limits":{"fds":256,"mem":536870912,"disk":2147483648},"host":"172.30.49.73"}',
		VCAP_SERVICES: '{"mysql-5.1":[{"name":"mysql-d742a80","label":"mysql-5.1","plan":"free","credentials":{"node_id":"mysql_node_4","hostname":"172.30.48.23","port":3306,"password":"pnQpbTF6uLKjM","name":"d42c20658098f46bc8727ebefc17d9b05","user":"uQ26c5VyHEoEc"}}],"mongodb-1.6":[{"name":"mongodb-f26df25","label":"mongodb-1.6","plan":"free","credentials":{"node_id":"mongodb_node_2","hostname":"172.30.48.61","port":25003,"password":"fe5fda8b-6ba5-4165-a67b-c84b6c66425c","name":"mongodb-528d271f-f0c4-4bbc-84c3-6c4246ad9e6a"}}]}',
		VCAP_APP_HOST: '172.30.49.73',
		VCAP_APP_PORT: '40954',
	]

	@Override
	protected void setUp() {
		super.setUp()
		registerMetaClass System
	}

	void testIsAvailable() {
		assertFalse env.isAvailable()
		enable()
		assertTrue env.isAvailable()
	}

	void testGetHost() {
		assertNull env.host
		enable()
		assertEquals '172.30.49.73', env.host
	}

	void testGetPort() {
		assertNull env.port
		enable()
		assertEquals 40954, env.port
	}

	void testGetInstanceInfo() {
		assertNull env.instanceInfo
		enable()
		assertNotNull env.instanceInfo
		assertEquals 'grailstest', env.instanceInfo.name
		assertEquals 0, env.instanceInfo.instanceIndex
		assertEquals(['grailstest.cloudfoundry.com'], env.instanceInfo.uris)
		assertEquals '172.30.49.73', env.instanceInfo.host
		assertEquals 40954, env.instanceInfo.port
	}

	void testGetServiceByVendor() {
		assertNull env.getServiceByVendor('mysql')
		assertNull env.getServiceByVendor('mongo')

		enable()

		AbstractServiceInfo info = env.getServiceByVendor('mysql')
		assertTrue info instanceof MysqlServiceInfo
		assertEquals 'd42c20658098f46bc8727ebefc17d9b05', info.database
		assertEquals 'uQ26c5VyHEoEc', info.userName
		assertEquals 'mysql-d742a80', info.serviceName
		assertEquals 'free', info.plan
		assertEquals '172.30.48.23', info.host
		assertEquals 3306, info.port
		assertEquals 'pnQpbTF6uLKjM', info.password

		info = env.getServiceByVendor('mongo')
		assertTrue info instanceof MongoServiceInfo
		assertEquals 'mongodb-f26df25', info.serviceName
		assertEquals 'free', info.plan
		assertEquals '172.30.48.61', info.host
		assertEquals 25003, info.port
		assertEquals 'fe5fda8b-6ba5-4165-a67b-c84b6c66425c', info.password
	}
	
	void testGetServiceByName() {
		assertNull env.getServiceByName('mysql-d742a80')
		assertNull env.getServiceByName('mongodb-f26df25')
		assertNull env.getServiceByName('not_a_service')

		enable()

		AbstractServiceInfo info = env.getServiceByName('mysql-d742a80')
		assertTrue info instanceof MysqlServiceInfo
		assertNull env.getServiceByName('not_a_service')

		info = env.getServiceByName('mongodb-f26df25')
		assertTrue info instanceof MongoServiceInfo

		assertNull env.getServiceByName('not_a_service')
	}
	
	private void enable() {
		System.metaClass.static.getenv = { String name -> environment[name] }
	}
}
