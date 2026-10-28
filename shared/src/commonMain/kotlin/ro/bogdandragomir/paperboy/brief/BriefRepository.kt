package ro.bogdandragomir.paperboy.brief

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import ro.bogdandragomir.paperboy.data.local.BriefLocalDataSource
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import ro.bogdandragomir.paperboy.data.remote.BriefApi

/** The topics the reader follows. Replaced by the onboarding choices in the next step. */
fun interface FollowedTopics {
    suspend fun ids(): List<String>
}

interface BriefRepository {
    /** The brief stored on the device. This is what the UI shows, online or offline. */
    fun observeBrief(): Flow<CachedBrief?>

    /** Downloads a fresh brief and stores it. On failure the stored brief stays as it was. */
    suspend fun refresh(): Result<Unit>
}

class OfflineFirstBriefRepository(
    private val api: BriefApi,
    private val local: BriefLocalDataSource,
    private val followedTopics: FollowedTopics,
    private val nowEpochMillis: () -> Long,
) : BriefRepository {
    override fun observeBrief(): Flow<CachedBrief?> = local.observe()

    override suspend fun refresh(): Result<Unit> =
        try {
            val brief = api.brief(followedTopics.ids(), BRIEF_SIZE)
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
