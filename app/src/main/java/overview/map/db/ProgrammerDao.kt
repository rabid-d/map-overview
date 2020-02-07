package overview.map.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ProgrammerDao {
    @Query("SELECT id, lat, lon FROM programmers WHERE lat is not null AND lon is not null")
    fun getAllPoints(): List<Point>

    @Query("SELECT * FROM programmers WHERE id = :id")
    fun getProgrammer(id: Int): Programmer

    @Query("SELECT * FROM programmers WHERE id IN (:ids)")
    fun getBunchOfProgrammers(ids: List<Int>): List<Programmer>

    @Query("SELECT * FROM programmers WHERE fio LIKE '%' || :fioQuery || '%' ORDER BY INSTR(fio, :fioQuery), fio LIMIT 10;")
    fun searchByFio(fioQuery: String): List<Programmer>
}
