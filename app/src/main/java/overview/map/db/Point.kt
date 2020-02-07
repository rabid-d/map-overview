package overview.map.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import net.sharewire.googlemapsclustering.ClusterItem

@Entity
class Point() : ClusterItem, Parcelable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    var id: Int = 0

    @NonNull
    @ColumnInfo(name = "lat")
    var lat: Float = 0.0f

    @NonNull
    @ColumnInfo(name = "lon")
    var lng: Float = 0.0f

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        lat = parcel.readFloat()
        lng = parcel.readFloat()
    }

    override fun getLatitude(): Double = lat.toDouble()
    override fun getLongitude(): Double = lng.toDouble()
    override fun getSnippet(): String? = null
    override fun getTitle(): String? = null

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel?, p1: Int) {
        parcel?.let {
            it.writeInt(id)
            it.writeFloat(lat)
            it.writeFloat(lng)
        }
    }

    companion object CREATOR : Parcelable.Creator<Point> {
        override fun createFromParcel(parcel: Parcel): Point {
            return Point(parcel)
        }

        override fun newArray(size: Int): Array<Point?> {
            return arrayOfNulls(size)
        }
    }
}
