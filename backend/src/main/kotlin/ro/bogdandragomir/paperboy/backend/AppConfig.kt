package ro.bogdandragomir.paperboy.backend

import java.time.Duration

/** Settings from environment variables. Defaults match docker-compose.yml for local development. */
data class AppConfig(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val ingestionEnabled: Boolean,
    val ingestionInterval: Duration,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()) =
            AppConfig(
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                databaseUrl = env["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/paperboy",
                databaseUser = env["DATABASE_USER"] ?: "paperboy",
                databasePassword = env["DATABASE_PASSWORD"] ?: "paperboy",
                ingestionEnabled = env["INGESTION_ENABLED"]?.toBooleanStrictOrNull() ?: true,
                ingestionInterval = Duration.ofMinutes(env["INGESTION_INTERVAL_MINUTES"]?.toLongOrNull() ?: 15),
            )
    }
}
