package com.vortexai.android.core.common.di

import com.vortexai.android.core.common.dispatcher.CoroutineDispatcherProvider
import com.vortexai.android.core.common.dispatcher.DefaultCoroutineDispatcherProvider
import com.vortexai.android.core.common.result.ErrorHandler
import com.vortexai.android.core.common.result.DefaultErrorHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindCoroutineDispatcherProvider(
        dispatcherProvider: DefaultCoroutineDispatcherProvider
    ): CoroutineDispatcherProvider

    @Binds
    @Singleton
    abstract fun bindErrorHandler(
        errorHandler: DefaultErrorHandler
    ): ErrorHandler
} 