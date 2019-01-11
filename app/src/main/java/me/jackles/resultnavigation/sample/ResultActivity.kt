package me.jackles.resultnavigation.sample

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import me.jackles.resultnavigation.endWithResult

class ResultActivity: AppCompatActivity() {
    data class Test(private val text: String?): Parcelable {
        constructor(parcel: Parcel) : this(parcel.readString())

        override fun writeToParcel(dest: Parcel,
                                   flags: Int) {
            dest.writeString(text)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Test> {
            override fun createFromParcel(parcel: Parcel): Test {
                return Test(parcel)
            }

            override fun newArray(size: Int): Array<Test?> {
                return arrayOfNulls(size)
            }
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(TextView(this).apply {
            text = "Hello\nworld"
            setOnClickListener {
                endWithResult(this@ResultActivity,
                    Test("HI")
                )
            }
        })
    }
}