package inu.appcenter.intip_android.di

import inu.appcenter.intip_android.local.DataStoreManager
import inu.appcenter.intip_android.network.RetrofitAPI
import inu.appcenter.intip_android.repository.member.MemberRepository
import inu.appcenter.intip_android.repository.member.MemberRepositoryImpl
import inu.appcenter.intip_android.ui.login.AuthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { DataStoreManager(androidContext()) }
    single { RetrofitAPI(get<DataStoreManager>()) }
    single { get<RetrofitAPI>().apiService }

    single<MemberRepository> { MemberRepositoryImpl(get()) }

    viewModel{ AuthViewModel(get(), get()) }
}