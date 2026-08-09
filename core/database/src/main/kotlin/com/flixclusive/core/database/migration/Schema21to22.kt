package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.core.database.entity.media.MAX_PLAUSIBLE_RELEASE_MILLIS

/**
 * Rescales `media.releaseDate` values that were stored as microseconds back to milliseconds.
 *
 * `toDBMedia` used to normalise a provider's epoch to milliseconds and then hand the result to
 * `Instant.ofEpochSecond`, so what landed in the column was the real timestamp multiplied by a
 * thousand — microseconds wearing a milliseconds column's clothes. Reading a row back yields
 * `Date.time`, milliseconds again, so a title that was re-saved from what the database already held
 * picked up another factor of a thousand each time.
 *
 * Dividing is the exact inverse: the stored value is always a whole multiple of 1000, so this
 * recovers the original instant rather than approximating it. Every row in `media` is swept, not
 * just movies — shows were written through the same conversion and are inflated identically.
 *
 * Absent dates are cleared in the same sweep. The old conversion had no null path — a title the
 * provider gave no date for was written as epoch 0 — so those rows read as 1 Jan 1970. `toDBMedia`
 * now maps 0 back to null, and this brings what is already stored in line with it.
 *
 * Data-only; the schema is byte-for-byte identical to 21.
 */
internal object Schema21to22 : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Three is provably enough rather than merely generous: the widest value the column can
        // hold is Long.MAX_VALUE (~9.2e18), and three divisions take that to ~9.2e9 — inside the
        // plausible window. Each pass is guarded, so rows already in range are never touched.
        repeat(3) {
            db.execSQL(
                """
                UPDATE `media`
                SET `releaseDate` = `releaseDate` / 1000
                WHERE `releaseDate` > $MAX_PLAUSIBLE_RELEASE_MILLIS
                   OR `releaseDate` < -$MAX_PLAUSIBLE_RELEASE_MILLIS
                """.trimIndent(),
            )
        }

        db.execSQL("UPDATE `media` SET `releaseDate` = NULL WHERE `releaseDate` = 0")
    }
}
