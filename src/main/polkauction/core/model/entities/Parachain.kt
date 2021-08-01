package polkauction.core.model.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Parachains : IntIdTable() {
    val parachainId = integer("parachainId" ).uniqueIndex()
    val parachainName = varchar("ChainName", 255)
    val relayChain = reference("RelayChain", RelayChains)
    val website = varchar("Website", 255)

    init {
        index(true, parachainId, relayChain)
    }
}

class ParachainEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ParachainEntity>(Parachains)

    var parachainId by Parachains.parachainId
    var parachainName by Parachains.parachainName
    var relayChain by RelayChainEntity referencedOn Parachains.relayChain
    var website by Parachains.website

    fun toParachain() = Parachain(parachainId, parachainName, relayChain.toRelayChain(), website)
}

data class Parachain(
    val parachainId: Int,
    val parachainName: String,
    val relayChain: RelayChain,
    val website: String
)