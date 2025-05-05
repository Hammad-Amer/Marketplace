package com.shayaankhalid.marketplace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val signup = findViewById<TextView>(R.id.signup)
        signup.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            val url = "http://10.0.2.2/marketplace/login.php"

            val stringRequest = object : StringRequest(Method.POST, url,
                Response.Listener { response ->
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val user = json.getJSONObject("user")
                        val userId = user.getInt("id")
                        val userName = user.getString("name")
                        val userEmail = user.getString("email")
                        val pfp = user.getString("pfp")

                        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
                        sharedPref.edit().putInt("user_id", userId).apply()
                        sharedPref.edit().putString("user_name", userName).apply()
                        sharedPref.edit().putString("user_email", userEmail).apply()
                        sharedPref.edit().putString("pfp", pfp).apply()

                        startActivity(Intent(this, Homescreen::class.java))
                        finish()
                    } else {
                        val message = json.optString("message", "Login failed")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }) {
                override fun getParams(): Map<String, String> {
                    return mapOf("email" to email, "password" to password)
                }
            }

            Volley.newRequestQueue(this).add(stringRequest)
        }
    }
}
