package grails.plugin.cloudfoundry

import grails.test.GroovyPagesTestCase

class CloudFoundryTagLibTests extends GroovyPagesTestCase {

	static transactional = false

	private realSystemMetaclass

	private static final String JSON = '''
{"redis-2.2":
   [{"name":"redis-1d8e28a",
     "label":"redis-2.2",
     "plan":"free",
     "credentials":{
         "node_id":"redis_node_3",
         "hostname":"172.30.48.42",
         "port":5004,
         "password":"1463d9d0-4e35-4f2e-be2f-01dc5536f183",
         "name":"redis-1a69a915-6522-496c-93d5-1271d2b3118e"}
     }],
 "mongodb-1.8":
   [{"name":"mongodb-3854dbe",
     "label":"mongodb-1.8",
     "plan":"free",
     "credentials":{
         "hostname":"172.30.48.63",
         "port":25003,
         "username":"b6877670-da98-4124-85ca-84357f042797",
         "password":"f53e6a4b-f4b8-497d-ac81-43cb22cf1e88",
         "name":"mongodb-9dda2cfb-9672-4d58-8786-98c3abcb21ec",
         "db":"db"}
   }],
 "mysql-5.1":
   [{"name":"mysql-service",
     "label":"mysql-5.1",
     "plan":"free",
     "credentials":{
         "node_id":"mysql_node_8",
         "hostname":"mysql_server",
         "port":4321,
         "password":"mysql_password",
         "name":"mysql_database",
         "user":"mysql_user"}
     }],
 "postgresql-9":
   [{"name":"postgres-service",
     "label":"postgres-9",
     "plan":"free",
     "credentials":{
         "node_id":"postgres_node_8",
         "hostname":"postgresql_server",
         "port":1234,
         "password":"postgresql_password",
         "name":"postgresql_database",
         "user":"postgresql_user"}
     }]
}
'''

	@Override
	protected void setUp() {
		super.setUp()

		//registerMetaClass isn't available
		realSystemMetaclass = System.metaClass
		def emc = new ExpandoMetaClass(System, true, true)
		emc.initialize()
		GroovySystem.metaClassRegistry.setMetaClass(System, emc)

		System.metaClass.static.getenv = { String name -> JSON }
	}

	@Override
	protected void tearDown() {
		super.tearDown()
		GroovySystem.metaClassRegistry.removeMetaClass(System)
		GroovySystem.metaClassRegistry.setMetaClass(System, realSystemMetaclass)
	}

	void testNoName() {
		String output = applyTemplate('<cf:dbconsoleLink>MySQL DbConsole</cf:dbconsoleLink>')

		assertTrue output.contains("<a href='javascript:void(0)' onclick='openDbConsole()'>MySQL DbConsole</a>")

		assertTrue output.contains(
			"location.href = '/dbconsole/login.do" +
			"?driver=com.mysql.jdbc.Driver" +
			"&url=jdbc%3Amysql%3A%2F%2Fmysql_server%3A4321%2Fmysql_database" +
			"&user=mysql_user" +
			"&password=mysql_password" +
			"&jsessionid=' + jsessionid;")
	}

	void testBadName() {
		assertOutputEquals('', '<cf:dbconsoleLink name="flongle">Foo</cf:dbconsoleLink>')
	}

	void testGoodName() {
		String output = applyTemplate('<cf:dbconsoleLink name="postgres-service">PostgreSQL DbConsole</cf:dbconsoleLink>')

		assertTrue output.contains("<a href='javascript:void(0)' onclick='openDbConsole()'>PostgreSQL DbConsole</a>")

		assertTrue output.contains(
			"location.href = '/dbconsole/login.do" +
			"?driver=org.postgresql.Driver" +
			"&url=jdbc%3Apostgresql%3A%2F%2Fpostgresql_server%3A1234%2Fpostgresql_database" +
			"&user=postgresql_user" +
			"&password=postgresql_password" +
			"&jsessionid=' + jsessionid;")
	}
}
