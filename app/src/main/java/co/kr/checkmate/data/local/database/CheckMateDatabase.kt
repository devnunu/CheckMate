package co.kr.checkmate.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import co.kr.checkmate.data.local.dao.MemoDao
import co.kr.checkmate.data.local.dao.TodoDao
import co.kr.checkmate.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity.TodoEntity::class, TaskEntity.MemoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CheckMateDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun memoDao(): MemoDao

    companion object {
        const val DATABASE_NAME = "checkmate_db"
    }
}