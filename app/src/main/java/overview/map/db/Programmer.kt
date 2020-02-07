package overview.map.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull

@Entity(tableName = "programmers")
class Programmer(): Parcelable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    var id: Int = 0

    @NonNull
    @ColumnInfo(name = "fio")
    var fio: String = ""
        set(value) {
            val split = value.split(' ').toMutableList()
            for (i in 0 until split.size) {
                val text = split[i].toLowerCase().toCharArray()
                text[0] = text[0].toUpperCase()
                split[i] = text.joinToString("")
            }
            field = split.joinToString(" ")
        }

    @NonNull
    @ColumnInfo(name = "address")
    var address: String = ""

    @NonNull
    @ColumnInfo(name = "lat")
    var lat: Float = 0.0f

    @NonNull
    @ColumnInfo(name = "lon")
    var lng: Float = 0.0f

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        fio = parcel.readString()!!
        address = parcel.readString()!!
        lat = parcel.readFloat()
        lng = parcel.readFloat()
    }

    override fun writeToParcel(parcel: Parcel?, p1: Int) {
        parcel?.let {
            it.writeInt(id)
            it.writeString(fio)
            it.writeString(address)
            it.writeFloat(lat)
            it.writeFloat(lng)
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Programmer> {
        override fun createFromParcel(parcel: Parcel): Programmer {
            return Programmer(parcel)
        }

        override fun newArray(size: Int): Array<Programmer?> {
            return arrayOfNulls(size)
        }
    }
}
