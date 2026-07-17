package com.cjwilliams.pottytraining.di

import javax.inject.Qualifier

/**
 * The client without [com.cjwilliams.pottytraining.data.auth.AuthInterceptor] attached.
 * Refreshing tokens has to use this one, otherwise a failing refresh would trigger the
 * interceptor that asked for it.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedApollo
