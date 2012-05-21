grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()

		mavenLocal()
		mavenCentral()
	}

	dependencies {
		runtime('org.codehaus.jackson:jackson-core-asl:1.4.1')   { transitive = false }
		runtime('org.codehaus.jackson:jackson-mapper-asl:1.4.1') { transitive = false }
	}

	plugins {
		compile ':cloud-support:1.0.10'

		build(':release:2.0.2', ':rest-client-builder:1.0.2') {
			export = false
		}
	}
}
