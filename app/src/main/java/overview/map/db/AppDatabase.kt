package overview.map.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.fstyle.library.helper.AssetSQLiteOpenHelperFactory


@Database(entities = [Programmer::class, Point::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun programmerDao(): ProgrammerDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "overview.db"
                    )
                        .openHelperFactory(AssetSQLiteOpenHelperFactory())
                        //.allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
