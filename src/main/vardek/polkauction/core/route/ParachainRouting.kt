package vardek.polkauction.core.route

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import vardek.polkauction.core.service.ParachainService
import vardek.polkauction.core.service.sidecar.SidecarClient

fun Route.parachainRouting() {
    route("/parachain") {
        get("{chain}") {
            //TODO IoC
            val chain = call.parameters["chain"] ?: return@get call.respondText(
                "Missing or malformed chain",
                status = HttpStatusCode.BadRequest
            )
            val sidecarClient = SidecarClient(chain)
            val parachainService = ParachainService(sidecarClient)
            call.respond(parachainService.GetAllCurrentParachains())
        }
    }
}

fun Application.registerParachainRoutes() {
    routing {
        parachainRouting()
    }
}