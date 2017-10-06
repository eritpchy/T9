package com.fei_ke.t9

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.os.AsyncTask
import android.text.TextUtils
import com.fei_ke.common.model.ListData
import com.t9search.util.PinyinUtil
import com.t9search.util.T9Util
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val allAppList = ArrayList<App>()

    private val appList = MutableLiveData<ListData<App>>()

    private var keywords: String? = null

    private val searchedCache = WeakHashMap<String?, List<App>>()

    private val loading = AtomicBoolean(true)

    init {
        val loadTask = object : AsyncTask<Unit, List<App>, Unit>() {
            override fun doInBackground(vararg params: Unit) {
                val packageManager = application.packageManager
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)

                val activities = packageManager.queryIntentActivities(intent, 0)

                val tempList = ArrayList<App>()

                activities.forEachIndexed { index, info ->
                    val app = App(info.activityInfo.packageName,
                            info.activityInfo.name,
                            info.loadLabel(packageManager).toString(),
                            info.loadIcon(packageManager))

                    PinyinUtil.parse(app.searchUnit)
                    tempList.add(app)
                    if ((index != 0 && index.rem(18) == 0) || index == activities.size - 1) {
                        publishProgress(ArrayList(tempList))
                        tempList.clear()
                    }
                }
            }

            override fun onProgressUpdate(vararg values: List<App>) {
                super.onProgressUpdate(*values)
                val list = values[0]
                allAppList.addAll(list)

                if (TextUtils.isEmpty(keywords)) {
                    appList.postValue(ListData(list, true))
                } else {
                    query(keywords)
                }

            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)
                loading.set(false)
            }

        }

        loadTask.execute()
    }

    fun appList() = appList

    fun query(keywords: String?) {
        if (!loading.get()) {
            if (searchedCache.containsKey(keywords)) {
                appList.postValue(ListData(searchedCache[keywords]!!))
                return
            }
        }

        if (TextUtils.isEmpty(keywords)) {
            appList.postValue(ListData(allAppList))
            searchedCache.put(keywords, allAppList)
            return
        }
        val toSearchList = if (keywords!!.length > 1) {
            searchedCache[keywords.substring(0, keywords.length - 1)] ?: allAppList
        } else {
            allAppList
        }

        val searchedList = toSearchList.filter {
            T9Util.match(it.searchUnit, keywords)
        }.sortedBy {
            it.label.indexOf(it.searchUnit.matchKeyword.toString())
        }


        appList.postValue(ListData(searchedList))

        searchedCache.put(keywords, searchedList)
    }

}
