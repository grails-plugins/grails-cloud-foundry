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

/**
 * @author Burt Beckwith
 */

includeTargets << new File("$cloudFoundryPluginDir/scripts/_CfCommon.groovy")

USAGE = '''
grails cf-change-password
'''

target(cfChangePassword: 'Change the password for the current user') {
	depends cfInit

	doWithTryCatch {

		println "\nChanging password for '${client.getCloudInfo().user}'"

		String password = (askFor('New Password: ') ?: '').trim()
		String passwordAgain = (askFor('Verify Password: ') ?: '').trim()
		if (!password.equals(passwordAgain)) {
			println "\nError: Passwords did not match, try again"
			return
		}

		client.updatePassword password

		println '\nSuccessfully changed password'
		println "\nIf necessary, update grails.plugin.cloudfoundry.password with the new password.\n"
	}
}

setDefaultTarget cfChangePassword
