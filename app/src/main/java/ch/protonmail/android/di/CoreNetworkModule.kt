package ch.protonmail.android.di

import android.content.Context
import ch.protonmail.android.api.ProtonMailApiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.auth.domain.ClientSecret
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.humanverification.data.utils.NetworkRequestOverriderImpl
import me.proton.core.humanverification.domain.utils.NetworkRequestOverrider
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.data.NetworkManager
import me.proton.core.network.data.NetworkPrefs
import me.proton.core.network.data.ProtonCookieStore
import me.proton.core.network.data.client.ClientIdProviderImpl
import me.proton.core.network.data.client.ExtraHeaderProviderImpl
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkPrefs
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.network.domain.humanverification.HumanVerificationListener
import me.proton.core.network.domain.humanverification.HumanVerificationProvider
import me.proton.core.network.domain.server.ServerTimeListener
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @ClientSecret
    fun provideClientSecret(): String = ""

    @Provides
    @Singleton
    fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager =
        NetworkManager(context)

    @Provides
    @Singleton
    fun provideNetworkPrefs(@ApplicationContext context: Context) =
        NetworkPrefs(context)

    @Provides
    @Singleton
    fun provideApiManagerFactory(
        apiClient: ApiClient,
        clientIdProvider: ClientIdProvider,
        serverTimeListener: ServerTimeListener,
        networkManager: NetworkManager,
        networkPrefs: NetworkPrefs,
        protonCookieStore: ProtonCookieStore,
        sessionProvider: SessionProvider,
        sessionListener: SessionListener,
        humanVerificationProvider: HumanVerificationProvider,
        humanVerificationListener: HumanVerificationListener,
        @DefaultApiPins defaultApiPins: Array<String>,
        @AlternativeApiPins alternativeApiPins: List<String>,
        @BaseUrl baseUrl: String
    ): ApiManagerFactory = ApiManagerFactory(
        baseUrl,
        apiClient,
        clientIdProvider,
        serverTimeListener,
        networkManager,
        networkPrefs,
        sessionProvider,
        sessionListener,
        humanVerificationProvider,
        humanVerificationListener,
        protonCookieStore,
        CoroutineScope(Job() + Dispatchers.Default),
        defaultApiPins,
        alternativeApiPins,
        apiConnectionListener = null
    )

    @Provides
    @Singleton
    fun provideApiProvider(apiManagerFactory: ApiManagerFactory, sessionProvider: SessionProvider): ApiProvider =
        ApiProvider(apiManagerFactory, sessionProvider)

    @Provides
    @Singleton
    fun provideProtonCookieStore(@ApplicationContext context: Context): ProtonCookieStore =
        ProtonCookieStore(context)

    @Provides
    @Singleton
    fun provideClientIdProvider(protonCookieStore: ProtonCookieStore, @BaseUrl baseUrl: String): ClientIdProvider =
        ClientIdProviderImpl(baseUrl, protonCookieStore)

    @Provides
    @Singleton
    fun provideServerTimeListener(
        context: CryptoContext
    ) = object : ServerTimeListener {
        override fun onServerTimeUpdated(epochSeconds: Long) {
            // Update Core PGPCrypto time.
            context.pgpCrypto.updateTime(epochSeconds)
            // Updating deprecated openPGP time is not needed as the underlining call,
            // com.proton.gopenpgp.crypto.Crypto.updateTime() is the same
            // for both Core PGPCrypto and deprecated OpenPGP.
            // openPGP.updateTime(epochSeconds)
        }
    }

    @Provides
    @Singleton
    fun provideExtraHeaderProvider(): ExtraHeaderProvider = ExtraHeaderProviderImpl().apply {
        // BuildConfig.PROXY_TOKEN?.takeIfNotBlank()?.let { addHeaders("X-atlas-secret" to it) }
    }

    @Provides
    fun provideNetworkRequestOverrider(): NetworkRequestOverrider = NetworkRequestOverriderImpl(OkHttpClient())
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {

    @Binds
    abstract fun provideApiClient(apiClient: ProtonMailApiClient): ApiClient
}
