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
package grails.plugin.cloudfoundry

/**
 * @author Burt Beckwith
 */
class MysqlServiceInfo extends AbstractServiceInfo {

	final String database
	final String nodeId
	final String userName

	MysqlServiceInfo(Map<String, Object> serviceInfo) {
		super(serviceInfo)

		database = serviceInfo.credentials.name
		nodeId = serviceInfo.credentials.node_id
		userName = serviceInfo.credentials.user
	}

	String getUrl() { "jdbc:mysql://$host:$port/$database" }

	@Override
	String toString() {
		"${super.toString()}, database: $database, nodeId: $nodeId, userName: $userName"
	}
}
