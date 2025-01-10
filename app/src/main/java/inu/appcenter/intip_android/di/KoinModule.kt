package inu.appcenter.intip_android.di

import inu.appcenter.intip_android.local.DataStoreManager
import inu.appcenter.intip_android.network.RetrofitAPI
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val viewModelModule = module {
    single { DataStoreManager(androidContext()) }
    single { RetrofitAPI(get<DataStoreManager>()) }
    single { get<RetrofitAPI>().apiService }

}