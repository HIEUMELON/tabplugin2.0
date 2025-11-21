package com.example

import com.morpheusdata.core.Plugin
import com.morpheusdata.core.MorpheusContext
import com.example.NonameInstanceTabProvider
import com.example.NonameConsoleTabProvider

class NonamePlugin extends Plugin {

    @Override
    String getCode() {
        // MUST be unique across all plugins installed
        return "noname-plugin"
    }

    @Override
    void initialize() {
        // ✅ Basic metadata
        this.setName("Noname Plugin")
        this.setVersion("1.0.0")
        this.setDescription("Demo plugin: Instance & Console connection tabs")

        // ✅ Get MorpheusContext for providers
        MorpheusContext morpheusCtx = this.morpheus

        // ✅ Register providers (ensure classes exist under com.example)
        this.registerProvider(new NonameInstanceTabProvider(this, morpheusCtx))
        this.registerProvider(new NonameConsoleTabProvider(this, morpheusCtx))

        // (Optional: you can add other providers later)
        // this.pluginProviders = [
        //   new NonameInstanceTabProvider(this, morpheusCtx),
        //   new NonameConsoleTabProvider(this, morpheusCtx)
        // ]
    }

    @Override
    void onDestroy() {
        // Cleanup if you spin up threads/resources
        // Otherwise safe to leave empty
    }
}
