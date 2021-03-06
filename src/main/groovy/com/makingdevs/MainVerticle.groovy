package com.makingdevs

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions

class MainVerticle extends AbstractVerticle {
  @Override
  void start(){
    def config = vertx.currentContext().config()
    DeploymentOptions options = new DeploymentOptions().setConfig(config)
    if(!config.map['mail'] || !config.map['mongo'])
      throw new RuntimeException("""\
      Cannot run withouit config, check https://github.com/makingdevs/emailer-app/wiki/Emailer-App
     """)
    vertx.deployVerticle("src/main/groovy/com/makingdevs/Webserver.groovy", options)
  }
}
