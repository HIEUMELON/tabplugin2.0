package com.example

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.*
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import groovy.util.logging.Slf4j

@Slf4j
class NonameInstanceTabProvider extends AbstractInstanceTabProvider {
    final Plugin plugin
    final MorpheusContext morpheus

    NonameInstanceTabProvider(Plugin plugin, MorpheusContext context) {
        this.plugin  = plugin
        this.morpheus = context
    }

    @Override
    String getCode() { 'noname-instance-tab' }

    @Override
    String getName() { 'Connection Details' }

    @Override
    Boolean show(Instance instance, User user, Account account) {
        return true
    }

    // helper: convert evar value -> String
    /*private String evarToString(Object val) {
        if(val == null)
            return null
        if(val instanceof String)
            return (String)val
        if(val instanceof char[])
            return new String((char[])val)
        if(val instanceof byte[])
            return new String((byte[])val, 'UTF-8')
        return val.toString()
    }*/

    @Override
    HTMLResponse renderTemplate(Instance instance) {
        ViewModel model = new ViewModel()
        try {
            TaskConfig cfg = morpheus
                .buildInstanceConfig(instance, [:], null, [], [:])
                .blockingGet()

            TaskConfig.InstanceConfig ic = cfg?.instance

            TaskConfig.ServerConfig serverCfg = ic?.containers
                ?.collect { it?.server }
                ?.find { it != null }

            ComputeServer hypervisor = null
            ComputeServer vmServer   = null

            if(serverCfg != null) {
                if(serverCfg.parentServerId != null) {
                    hypervisor = morpheus.services.computeServer
                        .get(serverCfg.parentServerId as Long)
                }
                if(serverCfg.id != null) {
                    vmServer = morpheus.services.computeServer
                        .get(serverCfg.id as Long)
                }
            }

            String hypervisorHost =
                    hypervisor?.externalIp ?:
                    hypervisor?.internalIp ?:
                    hypervisor?.name ?:
                    'N/A'

            String sshHost =
                    vmServer?.sshHost ?:
                    vmServer?.externalIp ?:
                    vmServer?.internalIp ?:
                    hypervisorHost

            Integer sshPort = vmServer?.sshPort ?: 22
            String sshUsername = vmServer?.sshUsername ?: 'cloud'

            // ===== EVARS =====
            Map evars = cfg?.getEvars() ?: [:]

            // CUSTOM OPTIONS (catalog for DEMO)
            Map customOptions = cfg?.getCustomOptions() ?: [:]
            log.debug("NonameInstanceTab: customOptions=${customOptions}")
            String optUsername = (customOptions['username'] ?: null) as String
            String optPassword = (customOptions['password'] ?: null) as String
            String optDatabase = (customOptions['database'] ?: null) as String
            String optHost     = (customOptions['host'] ?: null) as String
            String optPort     = (customOptions['port'] ?: null) as String
            /*String evarUsername = evarToString(evars['username']?.value)
            String evarPassword = evarToString(evars['password']?.value)
            String evarDatabase = evarToString(evars['database']?.value)
*/
            /*String username = evarUsername ?: 'N/A'
            String password = evarPassword ?: 'N/A'
            String database = evarDatabase ?: 'N/A'
*/
            String username = optUsername ?: 'N/A'
            String password = optPassword ?: 'N/A'
            String database = optDatabase ?: 'N/A'
            String dbHost   = optHost ?: sshHost
            String dbPort   = optPort ?: sshPort.toString()
            Map data = [
                hypervisorHost: hypervisorHost,
                sshHost       : sshHost,
                sshPort       : sshPort,
                sshUsername   : sshUsername,

                username      : username,
                password      : password,
                database      : database,
                host          : dbHost,
                port          : dbPort,

                instanceName  : instance?.name ?: 'Instance'
            ]

            model.object = [instance: instance, data: data]
            return getRenderer().renderTemplate('hbs/instanceTab', model)

        } catch (Throwable ex) {
            log.error('NonameInstanceTab: render error', ex)
            model.object = [instance: instance, data: [:]]
            return getRenderer().renderTemplate('hbs/instanceNotFoundTab', model)
        }
    }

    @Override
    ContentSecurityPolicy getContentSecurityPolicy() {
        new ContentSecurityPolicy()
    }
}
