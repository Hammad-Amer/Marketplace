package com.shayaankhalid.marketplace

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class EditProfile : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private var base64Image: String? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        val username = findViewById<EditText>(R.id.editUsername)
        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        val editBtn = findViewById<Button>(R.id.btnEditProfile)
        val backBtn = findViewById<ImageView>(R.id.backButton)
        profileImage = findViewById(R.id.profileImage)

        username.setText(sharedPref.getString("user_name", ""))
        email.setText(sharedPref.getString("user_email", ""))
        password.setText(sharedPref.getString("password", ""))
        val pfpBase64 = sharedPref.getString("pfp", null)
        if (pfpBase64 != null && pfpBase64.isNotEmpty()) {
            try {
                val bitmap = decodeBase64ToBitmap(pfpBase64)
                profileImage.setImageBitmap(bitmap)
                base64Image = pfpBase64
            } catch (e: Exception) {
                profileImage.setImageResource(R.drawable.empty_user)  // fallback if decode fails
            }
        } else {
            profileImage.setImageResource(R.drawable.empty_user)  // fallback if no pfp saved
        }

        backBtn.setOnClickListener { finish() }

        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        editBtn.setOnClickListener {
            val nameStr = username.text.toString()
            val emailStr = email.text.toString()
            val passwordStr = password.text.toString()

            val url = "http://10.0.2.2/marketplace/edit_profile.php"

            val stringRequest = object : StringRequest(Method.POST, url,
                Response.Listener { response ->
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val editor = sharedPref.edit()
                        editor.putString("user_name", nameStr)
                        editor.putString("user_email", emailStr)
                        base64Image?.let { editor.putString("pfp", it) }
                        editor.apply()

                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed: ${json.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }) {
                override fun getParams(): Map<String, String> {
                    val params = mutableMapOf<String, String>()
                    params["id"] = userId.toString()
                    params["name"] = nameStr
                    params["email"] = emailStr
                    params["password"] = passwordStr
                    base64Image?.let { params["pfp"] = it }
                    return params
                }
            }

            Volley.newRequestQueue(this).add(stringRequest)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            profileImage.setImageBitmap(bitmap)

            base64Image = encodeImage(bitmap)
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}
private fun decodeBase64ToBitmap(base64Str: String): Bitmap {
    val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}
