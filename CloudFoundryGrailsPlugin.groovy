/* Copyright 2011 the original author or authors.
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

import grails.plugin.cloudfoundry.AppCloudEnvironment
import grails.plugin.cloudfoundry.AppCloudServiceBeanPostprocessor
import grails.plugin.cloudfoundry.MongoServiceInfo

class CloudFoundryGrailsPlugin {

	String version = '1.0.0.M1'
	String grailsVersion = '1.3.3 > *'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String title = 'Cloud Foundry Integration'
	String description = 'Cloud Foundry Integration'
	String documentation = 'http://grails.org/plugin/cloudfoundry'
	List pluginExcludes = [
		'docs/**',
		'src/docs/**'
	]

	String license = 'Apache 2.0 License'
	def organization = [ name: 'SpringSource', url: 'http://www.springsource.org/' ]
	def developers = [
		 [ name: 'Burt Beckwith', email: 'beckwithb@vmware.com' ] ]
	def issueManagement = [ system: 'JIRA', url: 'http://jira.grails.org/browse/GPCLOUDFOUNDRY' ]
	def scm = [ url: 'https://github.com/grails-plugins/grails-cloud-foundry' ]

	def doWithSpring = {
		appCloudServiceBeanPostprocessor(AppCloudServiceBeanPostprocessor)

		AppCloudEnvironment env = new AppCloudEnvironment()
		if (env.isAvailable()) {
			updateConfForMongo env, application
		}
	}

	private void updateConfForMongo(AppCloudEnvironment env, application) {

		MongoServiceInfo serviceInfo = env.getServiceByVendor('mongodb')
		if (!serviceInfo) {
			return
		}

		def conf = application.config.grails.mongo
		conf.databaseName = serviceInfo.db
		conf.host = serviceInfo.host
		conf.port = serviceInfo.port
		conf.password = serviceInfo.password
		conf.username = serviceInfo.userName

		println "Updated Mongo from VCAP_SERVICES: $serviceInfo"
	}
}
