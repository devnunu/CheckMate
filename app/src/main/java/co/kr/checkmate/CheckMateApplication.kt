package co.kr.checkmate

import android.app.Application
import co.kr.checkmate.di.appModule
import com.jakewharton.threetenabp.AndroidThreeTen
import com.ramcosta.composedestinations.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

class CheckMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Timber 초기화
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // ThreeTenABP 초기화 (날짜/시간 라이브러리)
        AndroidThreeTen.init(this)

        // Koin 초기화
        startKoin {
            androidLogger()
            androidContext(this@CheckMateApplication)
            modules(appModule)
        }
    }
}