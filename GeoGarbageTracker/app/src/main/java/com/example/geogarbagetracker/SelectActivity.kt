package com.example.geogarbagetracker

import android.app.Dialog
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class SelectActivity : AppCompatActivity() {
    private lateinit var locations: DatabaseReference
    private lateinit var storageRef: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        val database = Firebase.database.reference
        locations = database.child("locations")
        storageRef = Firebase.storage("gs://geogarbagetracker.appspot.com").reference


        val uploadButton: Button = findViewById(R.id.uploadButton)
        uploadButton.setOnClickListener {
            selectImage()
        }
        val viewMapButton: Button = findViewById(R.id.mapButton)
        viewMapButton.setOnClickListener {
            val intent = Intent(this@SelectActivity, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image file"), 1)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            val exif = contentResolver.openInputStream(data.data!!)?.let { ExifInterface(it) }
            var latLong:FloatArray = FloatArray(2)
            var hasLatLng:Boolean = exif!!.getLatLong(latLong)
            if(hasLatLng) {
                uploadImgFile(data.getData()!!,latLong.get(0),latLong.get(1))
            }else{
              var latlng = getLatLng()
                uploadImgFile(data.getData()!!,latlng.get(0),latlng.get(1))
            }
        }
    }

    private fun uploadImgFile(data: Uri,lat:Float,lng:Float) {
        val reference: StorageReference =
            storageRef.child("uploads/").child(data.lastPathSegment!!)
        reference.putFile(data)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.getDownloadUrl()
                    .addOnSuccessListener(OnSuccessListener<Uri> { uri ->
                        val downloadUrl = uri.toString()
                        Log.i("downloadUrl", downloadUrl)
                        val loc = locations.push()
                        loc.child("lon").setValue(lng)
                        loc.child("lat").setValue(lat)
                        loc.child("title").setValue(loc.key)
                        loc.child("url").setValue(downloadUrl)
                    })


                Toast.makeText(
                    this@SelectActivity,
                    "File uploaded successfully",
                    Toast.LENGTH_SHORT
                ).show()


            }
    }
    private fun getLatLng():FloatArray{
        var latlng:FloatArray = FloatArray(2)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@SelectActivity)
        builder.setTitle("Enter lat and lng")
        val layout = LinearLayout(this@SelectActivity)
        layout.orientation = LinearLayout.VERTICAL
        val titleBox = EditText(this@SelectActivity)
        titleBox.setText("Latitude")
        layout.addView(titleBox) // Notice this is an add method

        val descriptionBox = EditText(this@SelectActivity)
        descriptionBox.setText("Longitude")
        layout.addView(descriptionBox) // Another add method
        builder.setView(layout)
        builder.setPositiveButton(
            "Update"
        ) { dialog, which ->
            latlng.set(0,titleBox.text.toString().toFloat())
            latlng.set(1,descriptionBox.text.toString().toFloat())
            Toast.makeText(this@SelectActivity, "Updated Successfully", Toast.LENGTH_SHORT)
                .show()

            dialog.cancel()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        val dialog: Dialog = builder.create()
        dialog.show()
        return latlng
    }


}