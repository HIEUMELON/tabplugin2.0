package com.example

import com.morpheusdata.core.InstanceTabProvider
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.model.Instance
import com.morpheusdata.model.User
import com.morpheusdata.model.Account
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.Renderer
import com.morpheusdata.views.HandlebarsRenderer

class NonameConsoleTabProvider implements InstanceTabProvider {

    Plugin plugin
    MorpheusContext morpheusContext

    NonameConsoleTabProvider(Plugin plugin, MorpheusContext morpheusContext) {
        this.plugin = plugin
        this.morpheusContext = morpheusContext
    }

    @Override
    String getCode() {
        return "noname-console-tab"
    }

    @Override
    String getName() {
        return "Console Tab"
    }

    @Override
    Boolean show(Instance instance, User user, Account account) {
        def groupName = instance?.site?.name
        def isDatabase = instance?.name?.toLowerCase()?.contains('database')

        // Hide tab if instance belongs to "DB-Only" group and is a database
        if (groupName == 'DB-Only' && isDatabase) {
            return false
        }

        return true
    }

    @Override
    HTMLResponse renderTemplate(Instance instance) {
        def model = [instance: instance]
        def html = plugin.templateEngine.render("noname-console-tab", model)
        return new HTMLResponse(html)
    }

    // ✅ Required by InstanceTabProvider
    @Override
    Renderer getRenderer() {
        // Use built-in Handlebars renderer from plugin context
        return new HandlebarsRenderer(plugin, "hbs")
    }

    @Override
    Plugin getPlugin() {
        return plugin
    }

    @Override
    MorpheusContext getMorpheus() {
        return morpheusContext
    }
}
