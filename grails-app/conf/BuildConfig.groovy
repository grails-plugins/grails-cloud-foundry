grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()

		mavenRepo 'http://maven.springframework.org/milestone'

		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile('org.cloudfoundry:cloudfoundry-client-lib:0.7.5') {
			excludes 'commons-io', 'hamcrest-all', 'jackson-core-asl', 'jackson-mapper-asl',
			         'junit', 'log4j', 'mockito-core', 'spring-test', 'spring-web'
		}

		compile('org.cloudfoundry:cloudfoundry-caldecott-lib:0.1.1') {
			excludes 'cloudfoundry-client-lib', 'commons-io', 'jackson-core-asl', 'jackson-mapper-asl',
			         'junit', 'log4j', 'mockito-all', 'spring-test', 'spring-web'
		}

		runtime 'org.codehaus.jackson:jackson-mapper-asl:1.6.2'
	}

	plugins {
		compile ':cloud-support:1.0.11'

		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}
	}
}
