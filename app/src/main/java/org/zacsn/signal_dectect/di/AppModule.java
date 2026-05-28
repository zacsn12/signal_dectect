package org.zacsn.signal_dectect.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

/**
 * Hilt module for providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    
    /**
     * Provides the application context.
     * This is a simple example to verify Hilt configuration.
     */
    @Provides
    @Singleton
    public Context provideApplicationContext(@ApplicationContext Context context) {
        return context;
    }
}
