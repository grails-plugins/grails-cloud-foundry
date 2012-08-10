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

import org.cloudfoundry.client.lib.CloudFoundryException
import org.springframework.http.HttpStatus

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-add-user [--email] [--passwd]
'''

target(cfAddUser: 'Register a new user (requires admin privileges)') {
	depends cfInit

	doWithTryCatch {

		String email = validateString('email')
		String password = validateString('passwd')
		String passwordAgain = password

		if (!email) {
			email = askFor('Email: ')
		}

		if (!password) {
			password = askFor('Password: ')
			passwordAgain = askFor('Verify Password: ')
		}

		print '\nCreating New User: '
		try {
			client.register email, password
			println "OK\n\nIf necessary, update grails.plugin.cloudfoundry.username and grails.plugin.cloudfoundry.password with the new user email and password.\n"
		}
		catch (CloudFoundryException e) {
			if (e.statusCode == HttpStatus.FORBIDDEN) {
				println "\n\nSorry, the request was denied. Be sure your username and password are set and that the account has admin privileges\n"
			}
			else if (e.statusCode == HttpStatus.BAD_REQUEST) {
				println "\n\nSorry, there was a problem creating the user: $e.message\n"
			}
			else {
				throw e
			}
		}
	}
}

setDefaultTarget cfAddUser
