package com.practicum.plantcontrol

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MainActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://192.168.18.242/") // Используем https
            .client(getSecureOkHttpClient().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val esp32Api = retrofit.create(ESP32Api::class.java)
        fetchStatus(esp32Api)
    }

    private fun fetchStatus(api: ESP32Api) {
        api.getStatus().enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful && response.body() != null) {
                    statusTextView.text = response.body()
                } else {
                    statusTextView.text = "Error: ${response.code()}"
                }
                Log.d("MainActivity", "Response: ${response.message()}")
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                statusTextView.text = "Failed to connect: ${t.message}"
                Log.e("MainActivity", "Failed to connect", t)
            }
        })
    }

    private fun getSecureOkHttpClient(): OkHttpClient.Builder {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val inputStream = resources.openRawResource(R.raw.server) // Ваш сертификат в res/raw/server.crt
            val ca = certificateFactory.generateCertificate(inputStream)

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null) // Загрузка пустого KeyStore без пароля
            keyStore.setCertificateEntry("ca", ca)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            val trustManagers = trustManagerFactory.trustManagers

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagers, null)

            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManagers[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing OkHttpClient", e)
            throw RuntimeException(e)
        }
    }
}
