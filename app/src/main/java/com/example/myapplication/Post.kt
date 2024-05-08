package com.example.myapplication

import com.google.firebase.database.DatabaseReference
import android.os.Parcel
import android.os.Parcelable

data class Post(
    val id: String = "",
    var content: String = "",
    var location: String = "",
    var time: String = "",
    var imageUrl: String = "",
    var numberOfParticipants: Int = 0,
    val participants: MutableList<String> = mutableListOf(),
    val postCreator: String = "",


) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.createStringArrayList()!!,
        parcel.readString()!!

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(content)
        parcel.writeString(location)
        parcel.writeString(time)
        parcel.writeString(imageUrl)
        parcel.writeInt(numberOfParticipants)
        parcel.writeStringList(participants)
        parcel.writeString(postCreator)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}


