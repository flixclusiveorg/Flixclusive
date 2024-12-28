package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal class UserPinUpdateSchema5to6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                UPDATE User
                SET userId = 0
                WHERE userId = 1 AND NOT EXISTS (
                    SELECT 1 FROM User WHERE userId = 0
                ) AND (SELECT COUNT(*) FROM User) = 1;
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS User_new (
                    userId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    image INTEGER NOT NULL,
                    pin TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent()
        )
        db.execSQL(
            """
                INSERT INTO User_new (userId, name, image)
                SELECT userId, name, image FROM User
            """.trimIndent()
        )
        db.execSQL("DROP TABLE User")
        db.execSQL("ALTER TABLE User_new RENAME TO User")
    }
}