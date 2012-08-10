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

/**
 * @author Burt Beckwith
 */

import grails.converters.JSON
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil

import java.text.SimpleDateFormat

import org.apache.log4j.Logger
import org.cloudfoundry.client.lib.CloudApplication
import org.cloudfoundry.client.lib.CloudFoundryClient
import org.cloudfoundry.client.lib.CloudFoundryException
import org.cloudfoundry.client.lib.CloudInfo
import org.cloudfoundry.client.lib.CloudService
import org.cloudfoundry.client.lib.ServiceConfiguration
import org.cloudfoundry.client.lib.CloudApplication.AppState
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

includeTargets << grailsScript('_GrailsBootstrap')

target(cfInit: 'General initialization') {
	depends compile, fixClasspath, loadConfig, configureProxy, startLogging

	try {
		GrailsHttpRequestFactory = classLoader.loadClass('grails.plugin.cloudfoundry.GrailsHttpRequestFactory')

		cfConfig = config.grails.plugin.cloudfoundry

		def buildCfConfig = grailsSettings.config.grails.plugin.cloudfoundry
		username = buildCfConfig.username ?: cfConfig.username
		password = buildCfConfig.password ?: cfConfig.password

		if (!username || !password) {
			errorAndDie 'grails.plugin.cloudfoundry.username and grails.plugin.cloudfoundry.password must be set in Config.groovy or in .grails/settings.groovy'
		}

		log = Logger.getLogger('grails.plugin.cloudfoundry.Scripts')

		cfTarget = buildCfConfig.target ?: cfConfig.target ?: 'api.cloudfoundry.com'
		cloudControllerUrl = cfTarget.startsWith('http') ? cfTarget : 'http://' + cfTarget

		createClient username, password, cloudControllerUrl, cfConfig

		hyphenatedScriptName = GrailsNameUtils.getScriptName(scriptName)

		argsList = argsMap.params

		CRASH_LOG_NAMES = ['logs/err.log', 'logs/stderr.log', 'logs/stdout.log', 'logs/startup.log']

		isPush = false
	}
	catch (IllegalArgumentException e) {
		System.exit 1
	}
	catch (e) {
		printStackTrace e
		throw e
	}
}

target(fixClasspath: 'Ensures that the classes directories are on the classpath so Config class is found') {
	rootLoader.addURL grailsSettings.classesDir.toURI().toURL()
	rootLoader.addURL grailsSettings.pluginClassesDir.toURI().toURL()
}

target(loadConfig: 'Ensures that the config is properly loaded') {
	binding.variables.remove 'config'
	createConfig()
}

printStackTrace = { e ->
	GrailsUtil.deepSanitize e

	List<StackTraceElement> newTrace = []
	for (StackTraceElement element : e.stackTrace) {
		if (element.fileName && element.lineNumber > 0 && !element.className.startsWith('gant.')) {
			newTrace << element
		}
	}

	if (newTrace) {
		e.stackTrace = newTrace as StackTraceElement[]
	}

	e.printStackTrace()
}

doWithTryCatch = { Closure c ->

	try {
		String token = client.login()
		if (log.debugEnabled) log.debug 'Login token ' + token
	}
	catch (CloudFoundryException e) {
		error "Problem logging in; please check your username and password\n"
		return
	}

	boolean ok = true
	try {
		c()
	}
	catch (IllegalArgumentException e) {
		ok = false
		// do nothing, usage will be displayed but don't want to System.exit
		// in case we're in interactive
	}
	catch (CloudFoundryException e) {
		ok = false
		error "\n$e.message\n"
		printStackTrace e
	}
	catch (HttpServerErrorException e) {
		ok = false
		error "\n$e.message\n"
		printStackTrace e
	}
	catch (e) {
		ok = false
		if (e instanceof ResourceAccessException && e.cause instanceof IOException) {
			if (e.cause instanceof ConnectException) {
				error "\nUnable to connect to API server - check that grails.plugin.cloudfoundry.target is set correctly and that the server is available\n"
			}
			else if (e.cause instanceof EOFException) {
				error "\nEOFException - check that grails.plugin.cloudfoundry.target is set correctly and that the server is available\n"
			}
			else {
				error "\n$e.message\n"
			}
		}
		else {
			error "\n$e.message\n"
		}
		if (cfConfig.showStackTrace) {
			printStackTrace e
		}
	}

	if (!ok && !isInteractive) {
		exit 1
	}
}

errorAndDie = { String message ->
	error message
	throw new IllegalArgumentException()
}

error = { String message ->
	event('StatusError', [message])
}

getRequiredArg = { int index = 0 ->
	String value = validateStringValue(argsList[index])
	if (value) {
		return value
	}
	errorAndDie "\nUsage (optionals in square brackets):\n$USAGE"
}

prettySize = { long size, int precision = 1 ->
	if (size < 1024) {
		return "${size}B"
	}
	if (size < 1024*1024) {
		return String.format("%.${precision}fK", size / 1024.0)
	}
	if (size < 1024*1024*1024) {
		return String.format("%.${precision}fM", size/ (1024.0 * 1024.0))
	}
	return String.format("%.${precision}fG", size/ (1024.0 * 1024.0 * 1024.0))
}

ask = { String question, String answers = null, String defaultIfMissing = null ->
	String propName = 'cf.ask.' + System.currentTimeMillis()

	def args = [addProperty: propName, message: question]
	if (answers) {
		args.validargs = answers
		if (defaultIfMissing) {
			args.defaultvalue = defaultIfMissing
		}
	}

	ant.input args
	ant.antProject.properties[propName] ?: defaultIfMissing
}

getApplication = { String name = getAppName(), boolean nullIfMissing = false ->
	try {
		return client.getApplication(name)
	}
	catch (CloudFoundryException e) {
		if (e.statusCode == HttpStatus.NOT_FOUND) {
			if (nullIfMissing) {
				return null
			}
			errorAndDie "Application '$name' does not exist."
		}
	}
}

getFile = { int instanceIndex, String path ->
	try {
		client.getFile getAppName(), instanceIndex, path
	}
	catch (ResourceAccessException e) {
		if (!(e.cause instanceof IOException)) {
			throw e
		}
		''
	}
}

formatDate = { Date date, String format = 'MM/dd/yyyy hh:mma' ->
	new SimpleDateFormat(format).format(date)
}

displayLog = { String logPath, int instanceIndex, boolean showError, String destination = null ->
	try {
		String content = getFile(instanceIndex, logPath)
		if (content) {
			if (destination) {
				new File(destination).withWriter { it.write content }
				println "\nWrote $logPath to $destination\n"
			}
			else {
				println "==== $logPath ====\n"
				println content
				println ''
			}
		}
	}
	catch (e) {
		if (showError && logPath.indexOf('startup.log') == -1) {
			error "\nThere was an error retrieving $logPath, please try again"
		}
	}
}

describeHealth = { CloudApplication application ->

	if (!application || !application.state) return 'N/A'

	float runningInstances = application.runningInstances
	float instances = application.instances

	if (application.state == AppState.STARTED && instances > 0) {
		def health = (runningInstances / instances).round(3)
		if (health == 1) {
			return 'RUNNING'
		}
		return "${Math.round(health * 100)}%"
	}

	application.state == AppState.STOPPED ? 'STOPPED' : 'N/A'
}

deleteApplication = { boolean force, String name = getAppName() ->
	CloudApplication application = getApplication(name)

	def servicesToDelete = []
	for (String service in application.services) {
		if (force) {
			servicesToDelete << service
		}
		else {
			String answer = ask("Application '$application.name' uses '$service' service, would you like to delete it?",
				'y,n', 'y')
			if ('y'.equalsIgnoreCase(answer)) {
				servicesToDelete << service
			}
		}
	}

	client.deleteApplication application.name
	println "\nApplication '$application.name' deleted.\n"

	for (String service in servicesToDelete) {
		client.deleteService service
		println "Service '$service' deleted."
	}
}

checkHasCapacityFor = { int memWanted ->
	CloudInfo cloudInfo = client.getCloudInfo()

	if (!cloudInfo.limits || !cloudInfo.usage) {
		return
	}

	int availableForUse = cloudInfo.limits.maxTotalMemory - cloudInfo.usage.totalMemory
	if (availableForUse < memWanted) {
		String totalMemory = prettySize(cloudInfo.limits.maxTotalMemory * 1024 * 1024)
		String usedMemory = prettySize(cloudInfo.usage.totalMemory * 1024 * 1024)
		String available = prettySize(availableForUse * 1024 * 1024)
		errorAndDie "Not enough capacity for operation.\nCurrent Usage: ($usedMemory of $totalMemory total, $available available for use)"
	}
}

buildWar = { ->
	File warfile
	if (validateString('warfile')) {
		warfile = new File(argsMap.warfile)
		if (warfile.exists()) {
			println "Using war file $argsMap.warfile"
		}
		else {
			errorAndDie "War file $argsMap.warfile not found"
		}
	}
	else {
		warfile = new File(grailsSettings.projectTargetDir, 'cf-temp-' + System.currentTimeMillis() + '.war')
		warfile.deleteOnExit()

		println 'Building war file'
		argsList.clear()
		argsList << warfile.path
		buildExplodedWar = false
		war()
	}

	warfile
}

memoryToMegs = { String memory ->
	if (memory.toLowerCase().endsWith('m')) {
		return memory[0..-2].toInteger()
	}

	if (memory.toLowerCase().endsWith('g')) {
		return memory[0..-2].toInteger() * 1024
	}

	memory.toInteger()
}

checkDevelopmentEnvironment = { ->
	if ('development'.equals(grailsEnv) && !validateString('warfile')) {
		String answer = ask(
			"\nYou're running in the development environment but haven't specified a war file, so one will be built with development settings. Are you sure you want to do proceed?",
			'y,n', 'y')
		if ('n'.equalsIgnoreCase(answer)) {
			return false
		}
	}
	true
}

getAppName = { -> validateString('appname') ?: cfConfig.appname ?: grailsAppName }

displayInBanner = { names, things, renderClosures, lineBetweenEach = true ->

	def output = new StringBuilder()

	def maxLengths = []
	for (name in names) {
		maxLengths << name.length()
	}

	for (thing in things) {
		renderClosures.eachWithIndex { render, index ->
			maxLengths[index] = Math.max(maxLengths[index], render(thing).toString().length())
		}
	}

	def divider = new StringBuilder()
	divider.append '+'
	maxLengths.each { length ->
		(length + 2).times { divider.append '-' }
		divider.append '+'
	}

	output.append '\n'
	output.append(divider).append('\n')
	names.eachWithIndex { name, i ->
		output.append '| '
		output.append name.padRight(maxLengths[i])
		output.append ' '
	}
	output.append '|\n'
	output.append(divider).append('\n')

	for (thing in things) {
		renderClosures.eachWithIndex { render, index ->
			output.append '| '
			output.append render(thing).toString().padRight(maxLengths[index])
			output.append ' '
		}
		output.append '|\n'
		if (lineBetweenEach) {
			output.append(divider).append('\n')
		}
	}
	if (!lineBetweenEach) {
		output.append(divider).append('\n')
	}

	output.append '\n'
	println output
}

createService = { ServiceConfiguration configuration, String serviceName = null ->
	if (!serviceName) {
		serviceName = "$configuration.vendor-${fastUuid()[0..6]}"
	}

	// TODO tier: 'free'
	client.createService new CloudService(
		name: serviceName, tier: 'free', type: configuration.type,
		vendor: configuration.vendor, version: configuration.version)

	println "Service '$serviceName' provisioned."

	serviceName
}

askFor = { String question ->
	String answer
	while (!answer) {
		answer = ask(question)
	}
	answer
}

hasConsole = { -> getBinding().variables.containsKey('grailsConsole') }

displayPermanent = { String msg ->
	if (hasConsole()) grailsConsole.addStatus(msg)
	else println msg
}

displayStatusMsg = { String msg ->
	if (hasConsole()) grailsConsole.updateStatus(msg)
	else print msg
}

displayStatusResult = { String msg ->
	if (hasConsole()) grailsConsole.updateStatus(grailsConsole.lastMessage + msg)
	else println msg
}

displayPeriod = {->
	if (hasConsole()) grailsConsole.indicateProgress()
	else println '.'
}

String fastUuid() {
	[0x0010000, 0x0010000, 0x0010000, 0x0010000, 0x0010000, 0x1000000, 0x1000000].collect {
		Integer.toHexString(new Random().nextInt(it))
	}.join('')
}

void createClient(String username, String password, String cloudControllerUrl, ConfigObject cfConfig) {
	realClient = new CloudFoundryClient(username, password, null, new URL(cloudControllerUrl),
		GrailsHttpRequestFactory.newInstance())
	client = new ClientWrapper(realClient, GrailsHttpRequestFactory, cfConfig)
}

validateString = { String argName, boolean warn = false ->
	validateStringValue argsMap[argName], argName, warn
}

validateStringValue = { value, String argName = null, boolean warn = false ->
	if (value == null) {
		return null
	}
	if (!(value instanceof String)) {
		if (warn) {
			String argDesc = argName ? " (for argument '$argName')" : ''
			println "WARNING: Value '$value'$argDesc isn't a String, ignoring (assuming null)"
		}
		value = null
	}
	value
}

validateBoolean = { String argName, boolean warn = true ->
	def value = argsMap[argName]
	if (value == null) {
		return false
	}
	if ((value instanceof String) && (value.toLowerCase() in ['true', 'false', 'y', 'n', '1', '0'])) {
		value = value.toBoolean()
	}
	if (!(value instanceof Boolean)) {
		if (warn) {
			println "WARNING: Value '$value' (for argument '$argName') isn't a boolean, assuming false"
		}
		value = false
	}
	value
}

class ClientWrapper {

	private CloudFoundryClient realClient
	private GrailsHttpRequestFactory
	private ConfigObject cfConfig
	private Logger log = Logger.getLogger('grails.plugin.cloudfoundry.ClientWrapper')

	ClientWrapper(CloudFoundryClient client, Class requestFactoryClass, ConfigObject cfConfig) {
		realClient = client
		GrailsHttpRequestFactory = requestFactoryClass
		this.cfConfig = cfConfig
	}

	def methodMissing(String name, args) {
		if (log.traceEnabled) log.trace "Invoking client method $name with args $args"

		GrailsHttpRequestFactory.resetResponse()
		try {
			if (args) {
				return realClient."$name"(*args)
			}
			else {
				return realClient."$name"()
			}
		}
		finally {
			logResponse()
		}
	}

	def propertyMissing(String name) {
		if (log.traceEnabled) log.trace "Invoking client property $name"

		GrailsHttpRequestFactory.resetResponse()
		try {
			return realClient."$name"
		}
		finally {
			logResponse()
		}
	}

	private void logResponse() {
		def lastResponseBytes = GrailsHttpRequestFactory.lastResponse
		if (!lastResponseBytes || !log.debugEnabled) {
			return
		}

		try {
			String lastResponse = new String(lastResponseBytes)
			boolean prettyPrint = cfConfig.prettyPrintJson instanceof Boolean ? cfConfig.prettyPrintJson : true
			if (prettyPrint) {
				try {
					def json = new JSON(JSON.parse(lastResponse))
					json.prettyPrint = true
					lastResponse = json.toString()
				}
				catch (ignored) {}
			}
			log.debug "Last Request: $lastResponse"
		}
		catch (e) {
			GrailsUtil.deepSanitize e
			log.error e.message, e
		}
	}
}
