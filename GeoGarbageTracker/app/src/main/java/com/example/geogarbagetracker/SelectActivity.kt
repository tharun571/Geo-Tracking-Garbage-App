package com.example.geogarbagetracker

import android.R.attr.description
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class SelectActivity : AppCompatActivity() {
    private lateinit var locations: DatabaseReference
    private lateinit var storageRef: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        val database = FirebaseDatabase.getInstance().reference
        locations = database.child("locations")
        storageRef = FirebaseStorage.getInstance().reference


        val uploadButton: Button = findViewById(R.id.uploadButton)
        uploadButton.setOnClickListener {
            selectImage()
        }
        val viewMapButton: Button = findViewById(R.id.mapButton)
        viewMapButton.setOnClickListener {
            val intent = Intent(this@SelectActivity, MapActivity::class.java)
            startActivity(intent)
        }

        val cameraButton: Button = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {
            val intent = Intent(this@SelectActivity, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image file"), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadImgFile(data.getData()!!);
        }
    }

    private fun uploadImgFile(data: Uri) {
        val reference: StorageReference =
            storageRef.child("uploads/").child(data.lastPathSegment!!)
        reference.putFile(data)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.getDownloadUrl()
                    .addOnSuccessListener(OnSuccessListener<Uri> { uri ->
                        val downloadUrl = uri.toString()
                        Log.i("downloadUrl", downloadUrl)
                        val loc = locations.push()
                        loc.child("key").setValue(loc.key)
                        loc.child("url").setValue(downloadUrl)
                    })


                Toast.makeText(
                    this@SelectActivity,
                    "File uploaded successfully",
                    Toast.LENGTH_SHORT
                ).show()


            }
    }


}