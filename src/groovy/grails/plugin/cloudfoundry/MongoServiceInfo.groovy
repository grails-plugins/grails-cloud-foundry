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
package grails.plugin.cloudfoundry

/**
 * @author Burt Beckwith
 */
class MongoServiceInfo extends AbstractServiceInfo {

	final String db
	final String name
	final String userName

	MongoServiceInfo(Map<String, Object> serviceInfo) {
		super(serviceInfo)

		db = serviceInfo.credentials.db
		name = serviceInfo.credentials.name // not sure what this is, but it's not used
		userName = serviceInfo.credentials.username
	}

	@Override
	String toString() {
		"${super.toString()}, db: $db, name: $name, userName: $userName"
	}
}
