package com.example

import com.morpheusdata.core.Plugin

class NonamePlugin extends Plugin {

  @Override String getCode() { "noname-plugin" }
  @Override String getName() { "Noname Plugin" }
  @Override String getDescription() { "Show connection details on Instance tab (Morpheus 8.0.9)" }

  @Override
  void initialize() {
    this.registerProvider(new NonameInstanceTabProvider(this, this.morpheus))
  }

  @Override
  void onDestroy() {
    // no-op
  }
}
