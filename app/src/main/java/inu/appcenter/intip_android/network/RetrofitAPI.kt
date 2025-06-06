package inu.appcenter.intip_android.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import inu.appcenter.intip_android.local.DataStoreManager
import inu.appcenter.intip_android.utils.K
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.component.KoinComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitAPI(private val dataStoreManager: DataStoreManager) : KoinComponent {

    private var gson: Gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(dataStoreManager))
        .addInterceptor(loggingInterceptor)
        // java.net.SocketTimeoutException
        .connectTimeout(15, TimeUnit.SECONDS)  // 연결 타임아웃
        .readTimeout(15, TimeUnit.SECONDS)     // 읽기 타임아웃
        .writeTimeout(15, TimeUnit.SECONDS)    // 쓰기 타임아웃
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(K.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: APIService by lazy {
        retrofit.create(APIService::class.java)
    }
}