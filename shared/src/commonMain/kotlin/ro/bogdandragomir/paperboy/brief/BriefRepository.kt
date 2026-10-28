package ro.bogdandragomir.paperboy.brief

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ro.bogdandragomir.paperboy.data.local.BriefLocalDataSource
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import ro.bogdandragomir.paperboy.data.local.InterestsRepository
import ro.bogdandragomir.paperboy.data.remote.PaperboyApi

interface BriefRepository {
    /** The brief stored on the device. This is what the UI shows, online or offline. */
    fun observeBrief(): Flow<CachedBrief?>

    /** Downloads a fresh brief for the followed topics and stores it. On failure the stored brief stays as it was. */
    suspend fun refresh(): Result<Unit>
}

class OfflineFirstBriefRepository(
    private val api: PaperboyApi,
    private val local: BriefLocalDataSource,
    private val interests: InterestsRepository,
    private val nowEpochMillis: () -> Long,
) : BriefRepository {
    override fun observeBrief(): Flow<CachedBrief?> = local.observe()

    override suspend fun refresh(): Result<Unit> =
        try {
            val topics = interests.followedTopicIds.first().orEmpty()
            check(topics.isNotEmpty()) { "No topics followed yet" }
            val brief = api.brief(topics.sorted(), BRIEF_SIZE)
            local.replace(brief, nowEpochMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Brief refresh failed" }
            Result.failure(e)
        }

    private companion object {
        const val BRIEF_SIZE = 10
    }
}
