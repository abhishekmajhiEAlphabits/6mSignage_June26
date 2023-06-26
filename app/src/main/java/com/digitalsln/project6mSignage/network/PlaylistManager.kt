package com.digitalsln.project6mSignage.network

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.digitalsln.project6mSignage.model.FileDescriptors
import com.digitalsln.project6mSignage.model.PlaylistData
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class PlaylistManager(context: Context) {
    private var TAG = "abhi"
    val context = context
    private var mediaSourceUrls = ArrayList<PlaylistData>()
    private var playlistSize: Int? = null
    private var fileDescriptors = ArrayList<FileDescriptors>()
    private var staticDurations = longArrayOf(4000, 3000, 3000, 3000, 3000, 8000, 2000)

    private val _fileDescriptorData = MutableLiveData<String>()
    val fileDescriptorData: LiveData<String> = _fileDescriptorData


    fun getPlayListData() {
        try {
            var i = 0
            var nativeScreenCode =
                AppPreference(context).retrieveValueByKey(
                    Constants.nativeScreenCode,
                    Constants.defaultNativeScreenCode
                )
            ApiClient.client().create(ApiInterface::class.java)
                .getPlayList(nativeScreenCode).enqueue(object : Callback<List<PlaylistData>> {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onResponse(
                        call: Call<List<PlaylistData>>,
                        response: Response<List<PlaylistData>>
                    ) {
                        if (response.isSuccessful) {
//                            Toast.makeText(context, "" + response, Toast.LENGTH_LONG).show()

                            if (response.body() != null) {
                                Log.d(TAG, "${response.body()}")

                                if (response.body()!!.size != null) {
                                    AppPreference(context).saveKeyValue(
                                        response.body()!!.size.toString(),
                                        "$nativeScreenCode-PLAYLIST_SIZE"
                                    )
                                    mediaSourceUrls.clear()
                                    for (data in response.body()!!) {
                                        var id = data.id
                                        var contentType = data.contentType
                                        var slideContentUrl = data.slideContentUrl
                                        var autoReplayVideo = data.autoReplayVideo
                                        var interval = data.interval
                                        var fromDate = data.fromDate
                                        var toDate = data.toDate
                                        var fromTime = data.fromTime
                                        var toTime = data.toTime
                                        var days = data.days

                                        if (id != null && contentType != null && slideContentUrl != null && interval != null) {
                                            mediaSourceUrls.add(
                                                PlaylistData(
                                                    id,
                                                    contentType,
                                                    slideContentUrl,
                                                    autoReplayVideo,
                                                    interval,
                                                    fromDate,
                                                    toDate,
                                                    fromTime,
                                                    toTime,
                                                    days
                                                )
                                            )

                                            AppPreference(context).saveKeyValue(
                                                id.toString(),
                                                "$nativeScreenCode-$i-ID"
                                            )
                                            AppPreference(context).saveKeyValue(
                                                contentType.toString(),
                                                "$nativeScreenCode-$i-CONTENT_TYPE"
                                            )
                                            AppPreference(context).saveKeyValue(
                                                slideContentUrl,
                                                "$nativeScreenCode-$i-CONTENT_URL"
                                            )
                                            AppPreference(context).saveKeyValue(
                                                autoReplayVideo.toString(),
                                                "$nativeScreenCode-$i-AUTO_REPLAY"
                                            )
                                            AppPreference(context).saveKeyValue(
                                                interval.toString(),
                                                "$nativeScreenCode-$i-INTERVAL"
                                            )
                                            if (fromDate != null) {
                                                AppPreference(context).saveKeyValue(
                                                    fromDate,
                                                    "$nativeScreenCode-$i-FROM_DATE"
                                                )
                                            }
                                            if (toDate != null) {
                                                AppPreference(context).saveKeyValue(
                                                    toDate,
                                                    "$nativeScreenCode-$i-TO_DATE"
                                                )
                                            }
                                            if (fromTime != null) {
                                                AppPreference(context).saveKeyValue(
                                                    fromTime,
                                                    "$nativeScreenCode-$i-FROM_TIME"
                                                )
                                            }
                                            if (toTime != null) {
                                                AppPreference(context).saveKeyValue(
                                                    toTime,
                                                    "$nativeScreenCode-$i-TO_TIME"
                                                )
                                            }
                                            AppPreference(context).saveKeyValue(
                                                days.toString(),
                                                "$$nativeScreenCode-$i-DAYS"
                                            )

                                        }
                                        i++
                                        Log.d(TAG, "${response.body()!!.size}")
                                    }
                                }
                            }

                        } else {
                            Toast.makeText(
                                context,
                                "Failed api call",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "Failed")
                        }
                        getFileUri()
                        readData()
                    }

                    override fun onFailure(call: Call<List<PlaylistData>>, t: Throwable) {
                        getFileUri()
                        readData()
                        Log.d(TAG, "$t")
                    }
                })
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }

    }

    private fun getFileUri() {
        try {
            var nativeScreenCode =
                AppPreference(context).retrieveValueByKey(
                    Constants.nativeScreenCode,
                    Constants.defaultNativeScreenCode
                )
            val responseSize =
                AppPreference(context).retrieveValueByKey("$nativeScreenCode-PLAYLIST_SIZE", "6")
            for (i in 0 until responseSize.toInt()) {
                var id = AppPreference(context).retrieveValueByKey("$nativeScreenCode-$i-ID", "0")
                var contentType =
                    AppPreference(context).retrieveValueByKey(
                        "$nativeScreenCode-$i-CONTENT_TYPE",
                        "2"
                    )
                var slideContentUrl =
                    AppPreference(context).retrieveValueByKey(
                        "$nativeScreenCode-$i-CONTENT_URL",
                        "NA"
                    )
                if (slideContentUrl != "NA") {
                    val uri: Uri = Uri.parse(slideContentUrl)
                    val filename = slideContentUrl.substring(slideContentUrl.length - 5)
                    Log.d(TAG, "file uri :: $uri :: $filename :: $filename")
                    Log.d(TAG, "file uri :: $contentType :: $id :: $slideContentUrl")
                    if (!fileExistInStorage(filename)) {
                        downloadMedia(uri, id.toInt(), contentType.toInt(), filename)
                        Log.d(TAG, "file downloading.. :: $filename")
                    } else {
                        Log.d(TAG, "file already exists :: $filename")
                    }
                }
            }
            playlistSize = mediaSourceUrls.size
            Log.d(TAG, "$playlistSize")
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    private fun downloadMedia(uri: Uri, fileId: Int, contentType: Int, filename: String) {
        try {
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            request.setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "$filename"
            )
            Log.d(TAG, "files image/video fileDir :: $fileId")


            val reference = downloadManager.enqueue(request)
            val query = DownloadManager.Query()

            query.setFilterById(reference)
            val cursor: Cursor = downloadManager.query(query)
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    private fun readFromStorage(fileId: Int, contentType: Int, interval: Int, filename: String) {
        try {
            var filePath =
                "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/$filename"
            var file = File(filePath)
            if (file.exists()) {
                fileDescriptors.add(FileDescriptors(fileId, contentType, filePath, true, interval))
            } else {
                fileDescriptors.add(FileDescriptors(fileId, contentType, filePath, false, interval))
            }
            Log.d(TAG, "read image/video :: $filePath :: ${file.exists()}")

            _fileDescriptorData.postValue("file descriptor data updated")
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    fun getDownloadedFilePath(): ArrayList<FileDescriptors> {
        readData()
        return fileDescriptors
    }

    private fun readData() {
        try {
            var nativeScreenCode =
                AppPreference(context).retrieveValueByKey(
                    Constants.nativeScreenCode,
                    Constants.defaultNativeScreenCode
                )
            fileDescriptors.clear()
            val responseSize =
                AppPreference(context).retrieveValueByKey("$nativeScreenCode-PLAYLIST_SIZE", "6")
            for (i in 0 until responseSize.toInt()) {
                var id = AppPreference(context).retrieveValueByKey("$nativeScreenCode-$i-ID", "0")
                var contentType =
                    AppPreference(context).retrieveValueByKey(
                        "$nativeScreenCode-$i-CONTENT_TYPE",
                        "2"
                    )
                var slideContentUrl =
                    AppPreference(context).retrieveValueByKey(
                        "$nativeScreenCode-$i-CONTENT_URL",
                        "NA"
                    )
                if (slideContentUrl != "NA") {
                    var interval =
                        AppPreference(context).retrieveValueByKey(
                            "$nativeScreenCode-$i-INTERVAL",
                            "NA"
                        )
                    val filename = slideContentUrl.substring(slideContentUrl.length - 5)
                    readFromStorage(id.toInt(), contentType.toInt(), interval.toInt(), filename)
                    Log.d("abhi", "readData")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    private fun fileExistInStorage(filename: String): Boolean {
        var filePath = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/$filename"
        var file = File(filePath)
        return file.exists()
    }

    fun deleteNDownloadData() {
        var isScreenRegistered = AppPreference(context).isScreenRegistered()
        var isPlaylistBound = AppPreference(context).isPlaylistBound()
        if (isScreenRegistered && isPlaylistBound) {
            getPlayListData()
        }
    }

}