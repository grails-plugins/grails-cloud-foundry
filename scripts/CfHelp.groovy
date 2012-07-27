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

target(cfHelp: 'Describes usage') {

	println '''
General syntax: grails [environment] <scriptName> [<args>] [command_options]
Use 'grails help <scriptName> for more information.

Currently available commands are:

  Getting Started
    cf-info                                           System and account information

  Applications
    cf-apps                                           List deployed applications

  Application Creation
    cf-push [--appname] [--url] [--memory]            Create, push, map, and start a new application
            [--warfile] [--services] [--no-start]

  Application Operations
    cf-start [--appname]                              Start the application
    cf-stop  [--appname]                              Stop the application
    cf-restart [--appname]                            Restart the application
    cf-delete-app [--appname] [--force]               Delete the application
    cf-delete-all-apps [--force]                      Delete the application
    cf-rename-app <newName> [--appname]               Rename the application

  Application Updates
    cf-update [--appname] [--warfile]                 Update the application bits
    cf-update-memory <memsize> [--appname]            Update the memory reservation for an application
    cf-map <url> [--appname]                          Register the application to the url
    cf-unmap <url> [--appname]                        Unregister the application from the url
    cf-update-instances <number> [--appname]          Scale the application instances up or down

  Application Information
    cf-crashes [--appname]                            List recent application crashes
    cf-crashlogs [--appname] [--instance]             Display log information for crashed applications
    cf-logs [destination] [--appname] [--instance]    Display log information for the application
            [--stderr] [--stdout] [--startup]
    cf-list-files [path] [--appname] [--instance]     Displays the specified file to the console or saves to a file
    cf-get-file <path> [destination] [--appname]
                [--instance]
    cf-stats [--appname]                              Display resource usage for the application
    cf-show-instances [--appname]                     Displays information about the instances of an application

  Application Environment
    cf-env [--appname]                                List application environment variables
    cf-env-add <variable> <value> [--appname]         Add an environment variable to an application
    cf-env-del <variable> [--appname]                 Delete an environment variable from an application

  Services
    cf-services                                       Lists of services available and provisioned
    cf-create-service <vendor> [name] [--bind]        Create a provisioned service
    cf-delete-service <servicename>                   Delete a provisioned service
    cf-bind-service <service> [--appname]             Bind a service to an application
    cf-unbind-service <service> [--appname]           Unbind service from the application
    cf-clone-services <sourceAppName> <destAppName>   Clone service bindings from <sourceAppName> application to <destAppName>

  Administration
    cf-user                                           Display user account information
    cf-add-user [--email] [--passwd]                  Register a new user (requires admin privileges)
    cf-change-password                                Change the password for the current user
    cf-delete-user <email>                            Delete a user and all apps and services
                                                      (requires admin privileges)

  System
    cf-runtimes                                       Display the supported runtimes of the target system
    cf-frameworks                                     Display the recognized frameworks of the target system

  Help
    cf-help                                           Get general help
'''
}

setDefaultTarget cfHelp
