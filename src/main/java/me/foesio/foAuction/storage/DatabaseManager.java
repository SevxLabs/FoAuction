package me.foesio.foAuction.storage;

import me.foesio.foAuction.FoAuction;
import me.foesio.foAuction.utils.ColorPalette;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public final class DatabaseManager {
    private static final int BUSY_TIMEOUT_MILLIS = 30_000;
    private static final int BUSY_RETRY_ATTEMPTS = 2;
    private static final long BUSY_RETRY_BACKOFF_MILLIS = 250L;

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T value) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultSetHandler<T> {
        T handle(ResultSet resultSet) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T execute() throws SQLException;
    }

    private final JavaPlugin plugin;
    private final String databaseUrl;
    private final Object accessLock;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseUrl = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/userdata.db";
        this.accessLock = new Object();
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try {
            configureConnection(connection);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    private void configureConnection(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS);
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA synchronous = NORMAL");
        }
    }

    public void initializeTables() {
        try {
            executeWithRetry(() -> {
                try (Connection conn = getConnection()) {
                    configureDatabaseFile(conn);

                    String createPlayersTable = """
                        CREATE TABLE IF NOT EXISTS players (
                            uuid TEXT PRIMARY KEY,
                            sort_mode TEXT NOT NULL DEFAULT 'NEWEST',
                            filter_mode TEXT NOT NULL DEFAULT 'ALL'
                        )
                        """;

                    String createAuctionsTable = """
                        CREATE TABLE IF NOT EXISTS auctions (
                            id TEXT PRIMARY KEY,
                            player_uuid TEXT NOT NULL,
                            item_data TEXT NOT NULL,
                            price REAL NOT NULL,
                            created_time BIGINT NOT NULL,
                            expire_time BIGINT NOT NULL,
                            FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                        )
                        """;

                    String createClaimsTable = """
                        CREATE TABLE IF NOT EXISTS claims (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            claim_data TEXT NOT NULL,
                            created_time BIGINT NOT NULL,
                            FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                        )
                        """;

                    String createSoldHistoryTable = """
                        CREATE TABLE IF NOT EXISTS sold_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            item_data TEXT NOT NULL,
                            price REAL NOT NULL,
                            buyer_name TEXT NOT NULL,
                            sold_time BIGINT NOT NULL,
                            FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                        )
                        """;

                    String createNotificationsTable = """
                        CREATE TABLE IF NOT EXISTS notifications (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            message TEXT NOT NULL,
                            created_time BIGINT NOT NULL,
                            FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                        )
                        """;

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(createPlayersTable);

                        boolean needsMigration = false;
                        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(auctions)")) {
                            while (rs.next()) {
                                String columnName = rs.getString("name");
                                String columnType = rs.getString("type");
                                if ("id".equals(columnName) && "INTEGER".equalsIgnoreCase(columnType)) {
                                    needsMigration = true;
                                    break;
                                }
                            }
                        }

                        if (needsMigration) {
                            FoAuction.fileLogger().info("Legacy auctions table detected; migrating integer IDs to UUID IDs.");
                            migrateAuctionsTable(conn);
                        } else {
                            stmt.execute(createAuctionsTable);
                        }

                        stmt.execute(createClaimsTable);
                        stmt.execute(createSoldHistoryTable);
                        stmt.execute(createNotificationsTable);
                        createIndexes(stmt);
                        FoAuction.fileLogger().info("Database tables and indexes verified.");
                    }
                }

                return null;
            });
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, ColorPalette.log("Failed to initialize database tables"), e);
            FoAuction.fileLogger().error("Failed to initialize database tables.", e);
        }
    }

    public void closeConnection() {
        // No-op: each operation owns and closes its own connection.
    }

    public <T> T withConnection(SqlFunction<T> function) throws SQLException {
        return executeWithRetry(() -> {
            try (Connection connection = getConnection()) {
                return function.apply(connection);
            }
        });
    }

    public void executeStatement(String sql, Object... params) throws SQLException {
        executeWithRetry(() -> {
            try (Connection connection = getConnection()) {
                executeStatement(connection, sql, params);
            }
            return null;
        });
    }

    public void executeStatement(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);
            stmt.execute();
        }
    }

    public <T> T executeQuery(String sql, ResultSetHandler<T> handler, Object... params) throws SQLException {
        return executeWithRetry(() -> {
            try (Connection connection = getConnection()) {
                return executeQuery(connection, sql, handler, params);
            }
        });
    }

    public <T> T executeQuery(Connection connection, String sql, ResultSetHandler<T> handler, Object... params)
            throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet resultSet = stmt.executeQuery()) {
                return handler.handle(resultSet);
            }
        }
    }

    public int executeUpdate(String sql, Object... params) throws SQLException {
        return executeWithRetry(() -> {
            try (Connection connection = getConnection()) {
                return executeUpdate(connection, sql, params);
            }
        });
    }

    public int executeUpdate(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    public void runInTransaction(SqlConsumer<Connection> action) throws SQLException {
        executeWithRetry(() -> {
            try (Connection connection = getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    action.accept(connection);
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    rollbackQuietly(connection);
                    throw e;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
            return null;
        });
    }

    private void bindParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            Level level = isBusyException(rollbackException) ? Level.FINE : Level.WARNING;
            plugin.getLogger().log(level, ColorPalette.log("Failed to roll back database transaction"), rollbackException);
            FoAuction.fileLogger().error("Failed to roll back database transaction.", rollbackException);
        }
    }

    private void configureDatabaseFile(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA wal_autocheckpoint = 1000");
        }
    }

    private void createIndexes(Statement stmt) throws SQLException {
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_player_uuid ON auctions(player_uuid)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_expire_time ON auctions(expire_time)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_claims_player_uuid ON claims(player_uuid)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sold_history_player_uuid ON sold_history(player_uuid)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_player_uuid ON notifications(player_uuid)");
    }

    private <T> T executeWithRetry(SqlOperation<T> operation) throws SQLException {
        SQLException lastException = null;
        for (int attempt = 1; attempt <= BUSY_RETRY_ATTEMPTS; attempt++) {
            synchronized (accessLock) {
                try {
                    return operation.execute();
                } catch (SQLException exception) {
                    if (!isBusyException(exception) || attempt == BUSY_RETRY_ATTEMPTS) {
                        throw exception;
                    }
                    lastException = exception;
                }
            }

            sleepBeforeBusyRetry(attempt, lastException);
        }

        throw lastException;
    }

    private void sleepBeforeBusyRetry(int attempt, SQLException lastException) throws SQLException {
        try {
            Thread.sleep(BUSY_RETRY_BACKOFF_MILLIS * attempt);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw lastException;
        }
    }

    public static boolean isBusyException(SQLException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                int errorCode = sqlException.getErrorCode();
                if (errorCode == 5 || errorCode == 6) {
                    return true;
                }
            }

            String message = current.getMessage();
            if (message != null) {
                String loweredMessage = message.toLowerCase(java.util.Locale.ROOT);
                if (loweredMessage.contains("sqlite_busy")
                        || loweredMessage.contains("sqlite_locked")
                        || loweredMessage.contains("database is locked")) {
                    return true;
                }
            }

            current = current.getCause();
        }
        return false;
    }

    private void migrateAuctionsTable(Connection connection) throws SQLException {
        String createNewAuctionsTable = """
            CREATE TABLE auctions (
                id TEXT PRIMARY KEY,
                player_uuid TEXT NOT NULL,
                item_data TEXT NOT NULL,
                price REAL NOT NULL,
                created_time BIGINT NOT NULL,
                expire_time BIGINT NOT NULL,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;

        String insertAuctionSql = """
            INSERT INTO auctions (id, player_uuid, item_data, price, created_time, expire_time)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE auctions RENAME TO auctions_old");
                stmt.execute(createNewAuctionsTable);
                stmt.execute("""
                    INSERT OR IGNORE INTO players (uuid, sort_mode, filter_mode)
                    SELECT DISTINCT player_uuid, 'NEWEST', 'ALL'
                    FROM auctions_old
                    WHERE player_uuid IS NOT NULL AND player_uuid <> ''
                    """);

                try (PreparedStatement insertStmt = connection.prepareStatement(insertAuctionSql);
                     ResultSet rs = stmt.executeQuery(
                         "SELECT player_uuid, item_data, price, created_time, expire_time FROM auctions_old")) {
                    while (rs.next()) {
                        insertStmt.setString(1, UUID.randomUUID().toString());
                        insertStmt.setString(2, rs.getString("player_uuid"));
                        insertStmt.setString(3, rs.getString("item_data"));
                        insertStmt.setDouble(4, rs.getDouble("price"));
                        insertStmt.setLong(5, rs.getLong("created_time"));
                        insertStmt.setLong(6, rs.getLong("expire_time"));
                        insertStmt.executeUpdate();
                    }
                }

                stmt.execute("DROP TABLE auctions_old");
            }
            connection.commit();
            FoAuction.fileLogger().info("Legacy auctions table migration complete.");
        } catch (SQLException exception) {
            rollbackQuietly(connection);
            FoAuction.fileLogger().error("Legacy auctions table migration failed.", exception);
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
