package polkauction.core.service

import polkauction.core.exception.SidecarGetException
import polkauction.core.model.Parachain
import polkauction.core.model.entities.acceptLeasePeriodsFrom
import polkauction.core.model.mapper.toLease
import polkauction.core.model.mapper.toParachain
import polkauction.core.model.with
import polkauction.core.repository.IParachainRepository
import polkauction.core.service.sidecar.ISidecarClient
import polkauction.core.service.sidecar.ISidecarClientFactory

class ParachainService(
    private val parachainRepository: IParachainRepository,
    private val sidecarClientFactory: ISidecarClientFactory,
    private val leasePeriodService: ILeasePeriodService
) : IParachainService {

    override suspend fun getAllCurrentParachains(chain: String): List<Parachain> {
        val registeredParachains = parachainRepository.getAllFor(chain.toLowerCase().capitalize())
        val sidecarClient = sidecarClientFactory.getSidecarClient(chain)

        return try {
            val parachains = sidecarClient.getParas().paras.map { it.toParachain() }

            parachains.forEach { loadLeases(sidecarClient, it) }

            val allParachainsLeases = parachains.flatMap { it.currentLeases }.map { it.leaseIndexPeriod }.distinct()

            val leasePeriods = leasePeriodService.getFilteredFor(chain, acceptLeasePeriodsFrom(allParachainsLeases))

            parachains.map {
                it.with(
                    registeredParachains.find { rp -> rp.parachainId == it.parachainId.toInt() },
                    leasePeriods
                )
            }
        } catch (e: SidecarGetException) {
            listOf()
        }
    }

    override suspend fun getParachain(chain: String, id: Int): Parachain? {
        val registeredParachain = parachainRepository.getByIdFor(id, chain.toLowerCase().capitalize())
        val sidecarClient = sidecarClientFactory.getSidecarClient(chain.toLowerCase().capitalize())
        val parachains = sidecarClient.getParas().paras.map { it.toParachain() }
        val allParachainsLeases = parachains.flatMap { it.currentLeases }.map { it.leaseIndexPeriod }.distinct()
        val leasePeriods = leasePeriodService.getFilteredFor(chain, acceptLeasePeriodsFrom(allParachainsLeases))

        return try {
            val parachain = parachains.singleOrNull { it.parachainId.toInt() == id }

            if (parachain == null)
                return parachain

            loadLeases(sidecarClient, parachain)

            parachain.with(registeredParachain, leasePeriods)
        } catch (e: SidecarGetException) {
            null
        }

    }

    private suspend fun loadLeases(sidecarClient: ISidecarClient, parachain: Parachain) {
        sidecarClient.getParaLeaseInfo(parachain.parachainId).leases?.forEach { parachain.currentLeases.add(it.toLease()) }
    }
}