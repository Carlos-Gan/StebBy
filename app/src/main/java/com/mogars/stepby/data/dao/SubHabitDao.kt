import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mogars.stepby.data.entity.SubHabitEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface SubHabitDao {

    @Query("SELECT * FROM sub_habits WHERE habit_id = :habitId")
    fun getSubHabitsForHabit(habitId: Long): Flow<List<SubHabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subHabit: SubHabitEntity)

    @Update
    suspend fun update(subHabit: SubHabitEntity)

    @Delete
    suspend fun delete(subHabit: SubHabitEntity)
}
