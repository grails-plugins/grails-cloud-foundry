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

import grails.plugin.cloudsupport.AbstractCloudBeanPostprocessor

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry

/**
 * Updates beans with connection information from the VCAP_SERVICES environment variable.
 *
 * @author Burt Beckwith
 */
class AppCloudServiceBeanPostprocessor extends AbstractCloudBeanPostprocessor {

	@Override
	protected boolean isAvailable(ConfigurableListableBeanFactory beanFactory, ConfigObject appConfig) {
		if (!new AppCloudEnvironment().isAvailable()) {
			log.info 'Not in CF environment, not processing'
			return false
		}

		true
	}

	@Override
	protected Map findDataSourceValues(ConfigurableListableBeanFactory beanFactory, ConfigObject appConfig) {
		AppCloudEnvironment env = new AppCloudEnvironment()
		MysqlServiceInfo mysqlServiceInfo = env.getServiceByVendor('mysql')
		PostgresqlServiceInfo postgresqlServiceInfo = env.getServiceByVendor('postgresql')
		if (!mysqlServiceInfo && !postgresqlServiceInfo) {
			log.debug "No MySQL or PostgreSQL service configured"
			return null
		}

		AbstractDatabaseServiceInfo serviceInfo
		String driverClassName
		String dialectClassName

		if (mysqlServiceInfo && postgresqlServiceInfo) {
			if (appConfig.dataSource.url.contains('postgresql')) {
				serviceInfo = postgresqlServiceInfo
				driverClassName = DEFAULT_POSTGRES_DRIVER
				dialectClassName = DEFAULT_POSTGRES_DIALECT
				log.debug "Both MySQL or PostgreSQL services configured; using PostgreSQL based on JDBC URL"
			}
			else {
				if (appConfig.dataSource.url.contains('mysql')) {
					log.debug "Both MySQL or PostgreSQL services configured; using MySQL based on JDBC URL"
				}
				else {
					log.warn "You have both MySQL and PostgreSQL services bound but it's not clear which " +
					         "one you want to use as your Grails DataSource; defaulting to MySQL but you " +
					         "can choose PostgreSQL by configuring a PostgreSQL JDBC URL in DataSource.groovy"
				}
				serviceInfo = mysqlServiceInfo
				driverClassName = DEFAULT_MYSQL_DRIVER
				dialectClassName = DEFAULT_MYSQL_DIALECT
			}
		}
		else {
			if (mysqlServiceInfo) {
				serviceInfo = mysqlServiceInfo
				driverClassName = DEFAULT_MYSQL_DRIVER
				dialectClassName = DEFAULT_MYSQL_DIALECT
				log.debug "Configuring DataSource for MySQL"
			}
			else {
				serviceInfo = postgresqlServiceInfo
				driverClassName = DEFAULT_POSTGRES_DRIVER
				dialectClassName = DEFAULT_POSTGRES_DIALECT
				log.debug "Configuring DataSource for PostgreSQL"
			}
		}

		[driverClassName: driverClassName,
		 url: serviceInfo.url,
		 userName: serviceInfo.userName,
		 password: serviceInfo.password,
		 dialectClassName: dialectClassName]
	}

	@Override
	protected boolean shouldConfigureDatasourceTimeout(ConfigObject appConfig) {
		def disable = appConfig.grails.plugin.cloudfoundry.datasource.disableTimeoutAutoconfiguration
		if (disable instanceof Boolean) {
			return !disable
		}
		true
	}

	@Override
	protected Map findRedisValues(ConfigurableListableBeanFactory beanFactory, ConfigObject appConfig) {
		RedisServiceInfo serviceInfo = new AppCloudEnvironment().getServiceByVendor('redis')
		if (!serviceInfo) {
			return null
		}

		[host: serviceInfo.host,
		 password: serviceInfo.password,
		 port: serviceInfo.port.toString()]
	}

	@Override
	protected Map findRabbitValues(ConfigurableListableBeanFactory beanFactory, ConfigObject appConfig) {
		RabbitServiceInfo serviceInfo = new AppCloudEnvironment().getServiceByVendor('rabbitmq')
		if (!serviceInfo) {
			return null
		}

		[host: serviceInfo.host,
		 userName: serviceInfo.userName,
		 password: serviceInfo.password,
		 virtualHost: serviceInfo.virtualHost,
		 port: serviceInfo.port]
	}

	@Override
	protected String getCompassIndexRootLocation(ConfigObject appConfig) {
		System.getenv('HOME')
	}

	@Override
	protected Map findMemcachedValues(ConfigurableListableBeanFactory beanFactory,
	                                  ConfigObject appConfig) {
		null
	}
}
