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

import org.cloudfoundry.client.lib.CloudApplication
import org.cloudfoundry.client.lib.CloudFoundryException
import org.cloudfoundry.client.lib.CloudService
import org.springframework.http.HttpStatus

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-delete-user <email>
'''

target(cfDeleteUser: 'Delete a user and all apps and services (requires admin privileges)') {
	depends cfInit

	doWithTryCatch {

		String email = getRequiredArg()

		// Check to make sure all apps and services are deleted before deleting the user
		List<CloudApplication> applications = client.getApplications()
		if (applications) {
			String answer = ask("\nDeployed applications and associated services will be DELETED, continue?", 'y,n', 'n')
			if ('n'.equalsIgnoreCase(answer)) {
				println 'Aborted'
				return
			}
		}

		for (CloudApplication application in applications) {
			deleteApplication true, application.name
		}

		List<CloudService> services = client.getServices()
		if (services) {
			for (CloudService service in services) {
				client.deleteService service.name
			}
		}

		print 'Deleting User: '
		try {
	      client.unregister()
			println 'OK'
		}
		catch (CloudFoundryException e) {
			if (e.statusCode == HttpStatus.FORBIDDEN) {
				println "\n\nSorry, the request was denied.\n"
			}
			else {
				throw e
			}
		}
	}
}

setDefaultTarget cfDeleteUser

