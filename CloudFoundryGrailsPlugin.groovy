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

import grails.plugin.cloudfoundry.AppCloudEnvironment
import grails.plugin.cloudfoundry.AppCloudServiceBeanPostprocessor
import grails.plugin.cloudfoundry.CloudFoundryMongoBeanConfigurer

class CloudFoundryGrailsPlugin {

	String version = '1.2.3'
	String grailsVersion = '1.3.3 > *'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String title = 'Cloud Foundry Integration'
	String description = 'Cloud Foundry Integration'
	String documentation = 'http://grails-plugins.github.com/grails-cloud-foundry/'
	List pluginExcludes = [
		'docs/**',
		'src/docs/**'
	]

	String license = 'APACHE'
	def organization = [name: 'SpringSource', url: 'http://www.springsource.org/']
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPCLOUDFOUNDRY']
	def scm = [url: 'https://github.com/grails-plugins/grails-cloud-foundry']

	def doWithSpring = {
		appCloudServiceBeanPostprocessor(AppCloudServiceBeanPostprocessor)

		if (new AppCloudEnvironment().isAvailable()) {
			new CloudFoundryMongoBeanConfigurer().fixMongo(application)
		}
	}
}
