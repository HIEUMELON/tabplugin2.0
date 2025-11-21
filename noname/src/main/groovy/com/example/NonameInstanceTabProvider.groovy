package com.example

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.*
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import groovy.util.logging.Slf4j

/**
 * Tab "Connection Details" cho Instance:
 * - Lấy host hypervisor + SSH info từ ComputeServer
 * - Username / Password / Database lấy từ evars của TaskConfig
 */
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
        // Nếu muốn ẩn tab trong một số trường hợp thì xử lý ở đây
        return true
    }

    private String evarToString(Object val) {
        if(val == null)
            return null
        if(val instanceof String)
            return (String)val
        if(val instanceof char[])
            return new String((char[])val)
        if(val instanceof byte[])
            return new String((byte[])val, 'UTF-8')
        return val.toString()
    }

    @Override
    HTMLResponse renderTemplate(Instance instance) {
        ViewModel model = new ViewModel()
        try {
            // Build TaskConfig cho instance
            TaskConfig cfg = morpheus
                .buildInstanceConfig(instance, [:], null, [], [:])
                .blockingGet()

            TaskConfig.InstanceConfig ic = cfg?.instance

            // Lấy ServerConfig đầu tiên trong containers
            TaskConfig.ServerConfig serverCfg = ic?.containers
                ?.collect { it?.server }
                ?.find { it != null }

            ComputeServer hypervisor = null
            ComputeServer vmServer   = null

            if (serverCfg != null) {
                // Hypervisor host
                if (serverCfg.parentServerId != null) {
                    hypervisor = morpheus.services.computeServer
                        .get(serverCfg.parentServerId as Long)
                }
                // Guest VM
                if (serverCfg.id != null) {
                    vmServer = morpheus.services.computeServer
                        .get(serverCfg.id as Long)
                }
            }

            // HOST hiển thị giống cột “Host” ngoài Instances
            String hypervisorHost =
                    hypervisor?.externalIp ?:
                    hypervisor?.internalIp ?:
                    hypervisor?.name ?:
                    'N/A'

            // Host dùng để SSH vào VM
            String sshHost =
                    vmServer?.sshHost ?:
                    vmServer?.externalIp ?:
                    vmServer?.internalIp ?:
                    hypervisorHost

            Integer sshPort = vmServer?.sshPort ?: 22
            String sshUsername = vmServer?.sshUsername ?: 'cloud'

            // ===== LẤY GIÁ TRỊ TỪ EVARS =====
            // cfg.getEvars() trả về Map<String, TaskConfig.Evar>
            Map evars = cfg?.getEvars() ?: [:]

            String evarUsername = evarToString(evars['username']?.value)
            String evarPassword = evarToString(evars['password']?.value)
            String evarDatabase = evarToString(evars['database']?.value)

            // Helper lấy evar theo tên, có thì trả value.toString(), không thì null
            def getEvarVal = { String name ->
                def ev = evars[name]
                ev ? ev.value?.toString() : null
            }

            // Ưu tiên các tên chuẩn DB_*, fallback sang tên thường nếu có
            String username =
                    getEvarVal('DB_USERNAME') ?:
                    getEvarVal('USERNAME') ?:
                    getEvarVal('username') ?:
                    'N/A'

            String password =
                    getEvarVal('DB_PASSWORD') ?:
                    getEvarVal('PASSWORD') ?:
                    getEvarVal('password') ?:
                    'N/A'

            String database =
                    getEvarVal('DB_DATABASE') ?:
                    getEvarVal('DATABASE') ?:
                    getEvarVal('database') ?:
                    'N/A'

            Map data = [
                hypervisorHost: hypervisorHost,
                sshHost       : sshHost,
                sshPort       : sshPort,
                sshUsername   : sshUsername,
                username      : username,
                password      : password,
                database      : database,
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
