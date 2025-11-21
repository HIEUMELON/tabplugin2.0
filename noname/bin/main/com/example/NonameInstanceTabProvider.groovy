package com.example

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.Instance
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel


class NonameInstanceTabProvider extends AbstractInstanceTabProvider {

    protected final MorpheusContext morpheusContext
    protected final Plugin plugin

    NonameInstanceTabProvider(Plugin plugin, MorpheusContext morpheusContext) {
        this.morpheusContext = morpheusContext
        this.plugin = plugin
    }
    /**
	 * A unique shortcode used for referencing the provided provider. Make sure this is going to be unique as any data
	 * that is seeded or generated related to this provider will reference it by this code.
	 * @return short code string that should be unique across all other plugin implementations.
	 */
    @Override
    String getCode() {
        // MUST be unique within your plugin
        return "noname-connection-details"
    }

    @Override
    String getName() {
        return "Connection Details"
    }

    String getDescription() {
        return "Shows database connection parameters and a ready-to-copy connection string."
    }

    /**
	 * Provide logic when tab should be displayed. This logic is checked after permissions are validated.
	 *
	 * @param instance Instance details
	 * @param user current User details
	 * @param account Account details
	 * @return whether the tab should be displayed
	 */
    @Override
    Boolean show(Instance instance, User user, Account account) {
        return true
    }
    /**
	 * Returns the Morpheus Context for interacting with data stored in the Main Morpheus Application
	 *
	 * @return an implementation of the MorpheusContext for running Future based rxJava queries
	 */
    @Override
    MorpheusContext getMorpheus() {
        return this.morpheusContext
    }
    /**
	 * Returns the instance of the Plugin class that this provider is loaded from
	 * @return Plugin class contains references to other providers
	 */
    @Override
    Plugin getPlugin() {
        return this.plugin
    }

	@Override
	HTMLResponse renderTemplate(Instance instance) {
		final Map cfg = (instance?.configMap ?: [:]) as Map
		final String dbType   = (cfg.type ?: "postgres").toString()
		final String username = (cfg.username ?: "cloud").toString()
		final String password = (cfg.password ?: "abc123").toString()
		final String database = (cfg.database ?: "demodb").toString()
		final Integer port    = (cfg.port ?: defaultPort(dbType)) as Integer

        // Determine host: prefer cfg.host, then instance.externalIp, instance.internalIp, instance.ipAddress, instance.server.internalIp, instance.server.externalIp
		final String host = resolveHostSafe(instance, cfg)

		final String dsn = buildDsn(dbType, username, password, host, port, database)

		def data = [
        type            : dbType,
        username        : username,
        password        : password,
        host            : host,
        port            : port,
        database        : database,
        connectionString: dsn
    ]
		def model = new ViewModel()  
    model.object = data           
    return getRenderer().renderTemplate("hbs/instanceTab", model)
	}	

	private static String resolveHostSafe(def instance, Map cfg) {
    	if (cfg?.host) return cfg.host.toString()

    	def ip = tryProp(instance, 'externalIp')
          	?: tryProp(instance, 'internalIp')
          	?: tryProp(instance, 'ipAddress')
    	if (ip) return ip

    	def server = tryProp(instance, 'server')
		if (server) {
			def sip = tryProp(server, 'externalIp') ?: tryProp(server, 'internalIp')
			if (sip) return sip
		}
    	return "unknown"
	}

	private static String tryProp(def obj, String name) {
		if (!obj) return null
		def mp = obj.metaClass?.hasProperty(obj, name)
		if (!mp) return null
		def v = obj."$name"
		return v != null ? v.toString() : null
		}


    private static int defaultPort(String type) {
        switch (type?.toLowerCase()) {
            case "postgres":
            case "postgresql":
                return 5432
            case "mysql":
                return 3306
            case "mariadb":
                return 3306
            case "mssql":
            case "sqlserver":
                return 1433
            case "oracle":
                return 1521
            default:
                return 5432
        }
    }

    private static String buildDsn(String type, String user, String pass, String host, int port, String db) {
        switch (type?.toLowerCase()) {
            case "mysql":
            case "mariadb":
                return "mysql://${user}:${pass}@${host}:${port}/${db}"
            case "mssql":
            case "sqlserver":
                // Basic form; adjust to your driver style if needed
                return "sqlserver://${user}:${pass}@${host}:${port};databaseName=${db}"
            case "oracle":
                return "jdbc:oracle:thin:${user}/${pass}@${host}:${port}:${db}"
            case "postgres":
            case "postgresql":
            default:
                return "postgresql://${user}:${pass}@${host}:${port}/${db}"
        }
    }
}
    