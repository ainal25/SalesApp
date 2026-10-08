package com.salesapp.android

import android.app.Application
import com.salesapp.android.data.Repository

class SalesApplication : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repository(this)
    }
}
