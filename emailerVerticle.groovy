import io.vertx.core.json.Json
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.ext.mongo.MongoClient

//Configuration of Mongo
def config = Vertx.currentContext().config()
def mongoClient = MongoClient.createShared(vertx, config.mongo)

//Event Bus Born
def eb = vertx.eventBus()

//Add new Email
eb.consumer("com.makingdevs.emailer.new", { message ->
    mongoClient.save("email_storage", message.body(), { id ->
      if (id.succeeded()) {
        message.reply("[ok]")
      } else {
        res.cause().printStackTrace()
      }
    })
})

//Show all emails
eb.consumer("com.makingdevs.emailer.show.total", { message ->
    def query=[:]
    mongoClient.find("email_storage", query, { res ->
      if (res.succeeded()) {
        message.reply(res.result())
      } else {
        res.cause().printStackTrace()
      }
    })
})

//Remove Email
eb.consumer("com.makingdevs.emailer.remove", { message ->
  mongoClient.remove("email_storage", message.body(), { res ->
    if (res.succeeded()) {
      message.reply("[ok]")
    } else {
      res.cause().printStackTrace()
    }
  })
})

//Count total of emails
eb.consumer("com.makingdevs.emailer.count", { message ->
  def query=[:]
  mongoClient.count("email_storage",query,{res ->
    if(res.succeeded()){
      message.reply(res.result())
    }
  })
})

//Show one Email
eb.consumer("com.makingdevs.emailer.show.one", { message ->
  mongoClient.find("email_storage", message.body(), { res ->
    if (res.succeeded()) {
      res.result().each { json ->
        def jsonEmail =groovy.json.JsonOutput.toJson(json)
        message.reply(jsonEmail)
      }
    } else {
      res.cause().printStackTrace()
    }
  })
})

//Show set of emails
eb.consumer("com.makingdevs.emailer.show.set", { message ->
  def query=[:]
  mongoClient.findWithOptions("email_storage", query, message.body(), { res ->
    if (res.succeeded()) {
      message.reply(res.result().reverse())
    } else {
      res.cause().printStackTrace()
    }
  })
})

//Update an email
eb.consumer("com.makingdevs.emailer.update", { message ->
  //Armando variables con los datos del mensaje
  def query=["_id": message.body().getAt(0)]//id del email
  def update=[
    $set:[
       subject:message.body().getAt(1),
       content:message.body().getAt(2),
       version:message.body().getAt(3),
       lastUpdate:message.body().getAt(4),
    ]
  ]
  //Haciendo el update
  mongoClient.update("email_storage", query, update, { res ->
    if (res.succeeded()) {
      message.reply("[ok]")
      println "Updated·"
    } else {
      res.cause().printStackTrace()
    }
  })
})

//Send Email Preview
eb.consumer("com.makingdevs.emailer.send", { message ->
  def query=["_id":message.body().getAt(0)]
  def receiver=message.body().getAt(1)

  //Buscando email
  mongoClient.find("email_storage", query, { res ->
    if (res.succeeded()) {
      println "Email encontrado :D"
      res.result().each { json ->
        def jsonEmail =groovy.json.JsonOutput.toJson(json)
        message.reply(jsonEmail)
        vertx.eventBus().send("com.makingdevs.emailer.send.email", "Hola, mandame un email no? a me@hi.com")
       //Mandando a otro verticle
      }
    } else {
      res.cause().printStackTrace()
    }
  })

})