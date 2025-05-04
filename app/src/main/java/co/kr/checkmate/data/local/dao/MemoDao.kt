package co.kr.checkmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import co.kr.checkmate.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos WHERE date = :date ORDER BY id DESC")
    fun getMemosByDate(date: Long): Flow<List<TaskEntity.MemoEntity>>

    @Query("SELECT * FROM memos ORDER BY date DESC")
    fun getAllMemos(): Flow<List<TaskEntity.MemoEntity>>

    @Query("SELECT * FROM memos WHERE date BETWEEN :startDate AND :endDate ORDER BY id DESC")
    fun getMemosByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity.MemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: TaskEntity.MemoEntity): Long

    @Update
    suspend fun update(memo: TaskEntity.MemoEntity)

    @Query("DELETE FROM memos WHERE id = :memoId")
    suspend fun delete(memoId: Long)
}