package com.sujitech.tessercubecore.common.extension

import android.app.Activity
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.microsoft.appcenter.utils.HandlerUtils
import com.sujitech.tessercubecore.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


fun Context.getClipboardText(): String {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    if (clipboardManager.hasPrimaryClip() && clipboardManager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true) {
        val primaryClip = clipboardManager.primaryClip
        if (primaryClip != null && primaryClip.itemCount > 0) {
            val firstItem = primaryClip.getItemAt(0)
            return firstItem.text.toString()
        }
    }
    return ""
}

fun Context.task(block: suspend CoroutineScope.() -> Unit) {
    val progress = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false).apply {
            }.create().apply {
                setCanceledOnTouchOutside(false)
            }
    progress.show()
    GlobalScope.launch {
        kotlin.runCatching {
            block.invoke(this)
        }.onFailure {
            it.printStackTrace()
            HandlerUtils.runOnUiThread {
                it.message?.let { it1 ->
                    Toast.makeText(this@task, it1, Toast.LENGTH_SHORT).show()
                }
            }
        }
        HandlerUtils.runOnUiThread {
            progress.dismiss()
        }
    }
}

fun Context.toast(content: String) {
    Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
}

suspend fun Context.inputAlert(title: String, type: Int = android.text.InputType.TYPE_CLASS_TEXT) = suspendCoroutine<String> { ct ->
    HandlerUtils.runOnUiThread {
        val editText = EditText(this).apply {
            inputType = type
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }
        AlertDialog.Builder(this)
                .setMessage(title)
                .setView(editText)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    ct.resume("")
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ct.resume(editText.text.toString())
                }
                .setOnCancelListener {
                    ct.resume("")
                }
//                .setOnDismissListener {
//                    ct.resume("")
//                }
                .show()
    }
}

suspend fun Context.alert(title: String) = suspendCoroutine<Boolean> { ct ->
    HandlerUtils.runOnUiThread {
        AlertDialog.Builder(this)
                .setMessage(title)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    ct.resume(false)
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ct.resume(true)
                }
                .setOnCancelListener {
                    ct.resume(false)
                }
//                .setOnDismissListener {
//                    ct.resume(false)
//                }
                .show()
    }
}

inline fun <reified T : Activity> Context?.toActivity(intent: Intent? = null) {
    this?.startActivity(Intent(this, T::class.java).apply {
        if (intent != null) {
            putExtras(intent)
        }
    })
}

fun Context.shareText(content: String) {
    val sharingIntent = Intent(Intent.ACTION_SEND)
    sharingIntent.type = "text/plain"
    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, content)
    startActivity(Intent.createChooser(sharingIntent, resources.getString(android.R.string.copy)))
}