import io.vertx.groovy.ext.web.handler.StaticHandler
import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.ext.web.handler.BodyHandler
import io.vertx.core.json.Json
import io.vertx.groovy.core.Vertx

def config = Vertx.currentContext().config()
if(!config.mail || !config.mongo)
  throw new RuntimeException("Cannot run withouit config, check https://github.com/makingdevs/emailer-app/wiki/Emailer-App")

  //configuracion externalizada
  options =[
  "config":config
  ]

  //routers
  def server = vertx.createHttpServer()
  def router = Router.router(vertx)
router.route().handler(BodyHandler.create())

//Route to Index
router.route("/static/*").handler(
  StaticHandler.create().setCachingEnabled(false)
)

//Add new Email
router.post("/newEmail").handler { routingContext ->
  def params = routingContext.request().params()
  def email = [
  subject:params.subjectEmail,
  content:params.contentEmail,
  dateCreated:new Date().time,
  lastUpdate:new Date().time,
  version:1
  ]
  vertx.eventBus().send("com.makingdevs.emailer.new", email, { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "text/html; charset=utf-8")
      .end("Email Agregado Exitosamente.")
    }
  })
}

//Show all emails
router.route("/show").handler({ routingContext ->
  vertx.eventBus().send("com.makingdevs.emailer.show.total", "Show me", { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(reply.result().body()))
    }

  })
})

//Remove an email
router.post("/remove").handler { routingContext ->
  def emailRemove= routingContext.request().getParam("idEmail")
  def query = ["_id":emailRemove]
  vertx.eventBus().send("com.makingdevs.emailer.remove", query, { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "text/html; charset=utf-8")
      .end("Email Eliminado Exitosamente ${reply.result().body()}")
    }
  })
}

//Count all of emails
router.route("/countTotal").handler({ routingContext ->
  vertx.eventBus().send("com.makingdevs.emailer.count", "Dame el conteo", { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(reply.result().body()))
    }
  })
})

//Show one Email
router.post("/showEmail").handler { routingContext ->
  def emailId= routingContext.request().getParam("idEmail")
  def query = ["_id":emailId]
  vertx.eventBus().send("com.makingdevs.emailer.show.one", query, { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "text/html; charset=utf-8")
      .end(reply.result().body())
    }
  })
}

//Show set of emails
router.post("/showSet").handler { routingContext ->

  def setValue=0
  setValue= routingContext.request().getParam("setValue")
  def options=[
  limit:10,
  skip:setValue.toInteger()
  ]
  //comunicate with verticle
  vertx.eventBus().send("com.makingdevs.emailer.show.set", options, { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(reply.result().body()))
    }
  })
}

//Actualizar un email
router.post("/update").handler { routingContext ->
  //Obtener datos del update
  def paramsUpdate=routingContext.request().params()
  //mensaje que actualizará los datos, seguir el orden
  def message=[
  paramsUpdate.email_id,//id del email a actualizar
  paramsUpdate.subjectEmail,//subject
  paramsUpdate.contentEmail,//contenido
  paramsUpdate.versionEmail.toInteger()+1,//Version
  new Date().time//fecha nueva en que esta siendo actualizado
  ]

  vertx.eventBus().send("com.makingdevs.emailer.update", message, { reply ->
    if (reply.succeeded()) {
      routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end("Update [ok]")
    }
  })
}

//Mandar el preview a un email
router.post("/send").handler { routingContext ->

  def idTemplate= routingContext.request().getParam("email_id")
  def emailToSend= routingContext.request().getParam("emailPreview")
  def message=[
  idTemplate,//id del email a enviar
  emailToSend//Email receiver
  ]
  vertx.eventBus().send("com.makingdevs.emailer.send", message)

  routingContext.response()
  .setStatusCode(201)
  .putHeader("content-type", "application/json; charset=utf-8")
  .end("Solicitud Enviada Coolicitud Enviada Correctamente..")
}

//Route para el servicio Web
router.post("/serviceEmail").handler { routingContext ->

  if(routingContext.getBody().length()){
    def jsonResponse=routingContext.getBodyAsJson()
    vertx.eventBus().send("com.makingdevs.emailer.service", jsonResponse)
    routingContext.response()
    .setStatusCode(201)
    .putHeader("Content-Type", "text/html; charset=utf-8")
    .end("Solicitud enviada correctamente. Espere felizmente.")

  }else{
    //response
    routingContext.response()
    .setStatusCode(400)
    .putHeader("Content-Type", "text/html; charset=utf-8")
    .end("I can't do my job, please send me something please.")
  }
}

server.requestHandler(router.&accept).listen(8080)

//deploy verticles
vertx.deployVerticle("emailerVerticle.groovy", options)
vertx.deployVerticle("senderVerticle.groovy", options)
