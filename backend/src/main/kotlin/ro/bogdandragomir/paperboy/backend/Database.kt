package ro.bogdandragomir.paperboy.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun createDataSource(
    url: String,
    user: String,
    password: String,
): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = 10
        },
    )

/** Applies the SQL files in resources/db/migration that haven't run yet. */
fun migrate(dataSource: DataSource) {
    Flyway
        .configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}
