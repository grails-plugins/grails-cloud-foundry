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

import grails.util.GrailsUtil

import org.apache.log4j.Logger
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.core.Ordered

/**
 * Updates beans with connection information from the VCAP_SERVICES environment variable.
 *
 * @author Burt Beckwith
 */
class AppCloudServiceBeanPostprocessor implements BeanDefinitionRegistryPostProcessor, Ordered {

	protected Logger log = Logger.getLogger(getClass())

	int getOrder() { 100 }

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(
	 * 	org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		log.info 'postProcessBeanDefinitionRegistry'
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(
	 * 	org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

		log.info 'postProcessBeanFactory start'

		def appConfig = beanFactory.parentBeanFactory.getBean('grailsApplication').config

		AppCloudEnvironment env = new AppCloudEnvironment()
		if (!env.isAvailable()) {
			log.info 'Not in CF environment, not processing'
			return
		}

		def groovyClassLoader = new GroovyClassLoader(getClass().classLoader)

		try {
			if (beanFactory.containsBean('dataSourceUnproxied')) {
				fixDataSource beanFactory.getBean('dataSourceUnproxied'), env, appConfig
			}
			else if (beanFactory.containsBean('dataSource')) {
				fixDataSource beanFactory.getBean('dataSource'), env, appConfig
			}
		}
		catch (Throwable e) {
			handleError e, 'Problem updating DataSource'
		}

		try {
			if (beanFactory.containsBean('rabbitMQConnectionFactory')) {
				fixRabbit env, beanFactory, groovyClassLoader, appConfig
			}
		}
		catch (Throwable e) {
			handleError e, 'Problem updating Rabbit'
		}

		try {
			if (beanFactory.containsBean('mongo')) {
				fixMongo env, beanFactory, groovyClassLoader
			}
		}
		catch (Throwable e) {
			handleError e, 'Problem updating MongoDB'
		}

		try {
			if (beanFactory.containsBean('compass')) {
				fixCompass env, beanFactory.getBeanDefinition('compass'), appConfig
			}
		}
		catch (Throwable e) {
			handleError e, 'Problem updating Searchable'
		}

		try {
			fixRedis env, beanFactory, groovyClassLoader, appConfig
		}
		catch (Throwable e) {
			handleError e, 'Problem updating Redis'
		}
	}

	protected void fixDataSource(bean, AppCloudEnvironment env, appConfig) {
		MysqlServiceInfo serviceInfo = env.getServiceByVendor('mysql')
		if (!serviceInfo) {
			return
		}

		bean.driverClassName = 'com.mysql.jdbc.Driver'
		bean.url = serviceInfo.url
		bean.username = serviceInfo.userName
		bean.password = serviceInfo.password
		log.debug "Updated DataSource from VCAP_SERVICES: $serviceInfo"

		configureDataSourceTimeout bean, appConfig
	}

	protected void configureDataSourceTimeout(bean, appConfig) {
		if (!bean.getClass().name.equals('org.apache.commons.dbcp.BasicDataSource')) {
			log.debug "Not configuring DataSource connection checking - datasource isn't a BasicDataSource"
			return
		}

		if (appConfig.grails.plugin.cloudfoundry.datasource.disableTimeoutAutoconfiguration) {
			log.debug "Not configuring DataSource connection checking - disableTimeoutAutoconfiguration is true"
			return
		}

		bean.removeAbandoned = true
		bean.removeAbandonedTimeout = 300 // 5 minutes
		bean.testOnBorrow = true
		bean.validationQuery = '/* ping */ SELECT 1'
		log.debug "Configured DataSource connection checking"
	}

	protected void fixRedis(AppCloudEnvironment env, ConfigurableListableBeanFactory beanFactory,
				ClassLoader groovyClassLoader, appConfig) {

		RedisServiceInfo serviceInfo = env.getServiceByVendor('redis')
		if (!serviceInfo) {
			return
		}

		def clazz = groovyClassLoader.loadClass('org.grails.plugins.redis.RedisDatastoreFactoryBean')
		def bean = clazz.newInstance()
		bean.mappingContext = beanFactory.getBean('redisDatastoreMappingContext')
		bean.pluginManager = beanFactory.getBean('pluginManager')

		def newConfig = [:]
		def config = appConfig.grails.redis
		config.each { key, value -> newConfig[key] = value?.toString() }
		newConfig.host = serviceInfo.host
		newConfig.password = serviceInfo.password
		newConfig.port = serviceInfo.port.toString()
		bean.config = newConfig

		beanFactory.registerSingleton 'redisDatastore', bean
		log.debug "Updated Redis from VCAP_SERVICES: $serviceInfo"
	}

	protected void fixRabbit(AppCloudEnvironment env, ConfigurableListableBeanFactory beanFactory,
			ClassLoader groovyClassLoader, appConfig) {

		RabbitServiceInfo serviceInfo = env.getServiceByVendor('rabbitmq')
		if (!serviceInfo) {
			return
		}

		// TODO this needs to keep in sync with rabbitmq plugin
		def config = appConfig.rabbitmq.connectionfactory
		def className = config.className ?: 'org.springframework.amqp.rabbit.connection.CachingConnectionFactory'
		def clazz = groovyClassLoader.loadClass(className)
		def connectionFactory = clazz.newInstance(serviceInfo.host)
		connectionFactory.username = serviceInfo.userName
		connectionFactory.password = serviceInfo.password
		connectionFactory.virtualHost = serviceInfo.virtualHost
		connectionFactory.port = serviceInfo.port

		connectionFactory.channelCacheSize = config.channelCacheSize ?: 10

		beanFactory.registerSingleton 'rabbitMQConnectionFactory', connectionFactory
		log.debug "Updated Rabbit from VCAP_SERVICES: $serviceInfo"
	}

	protected void fixMongo(AppCloudEnvironment env, ConfigurableListableBeanFactory beanFactory,
			ClassLoader groovyClassLoader) {

		// nothing to do - config properties are overridden in the plugin descriptor
	}

	protected void fixCompass(AppCloudEnvironment env, BeanDefinition bean, appConfig) {
		ApplicationInstanceInfo cfAppInfo = env.instanceInfo
		if (!cfAppInfo) {
			return
		}

		String indexLocation = System.getenv('HOME') + '/searchable-index'
		appConfig.searchable.compassConnection = indexLocation
		bean.propertyValues.addPropertyValue 'compassConnection', indexLocation
		log.debug "Updated Compass connection details: $indexLocation"
	}

	protected void handleError(Throwable t, String prefix) {
		GrailsUtil.deepSanitize t
		log.error "$prefix: $t.message", t
	}
}
