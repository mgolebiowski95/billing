package com.example.app.ui.common.messager

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.example.app.R

class ToastMessenger(private val context: Context) : Messenger {

    override fun showMessage(message: String, duration: Int) {
        val toast = Toast.makeText(context, message, duration)
        val textView = AppCompatTextView(context)
        textView.text = message
        textView.gravity = Gravity.CENTER
        textView.setTextColor(Color.BLACK)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        textView.maxWidth = (context.resources.displayMetrics.widthPixels * 0.64f).toInt()
        textView.minHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            44f,
            context.resources.displayMetrics
        ).toInt()
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        toast.setMargin(0f, 0.1f)
        textView.layoutParams = lp
        textView.setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                context.resources.displayMetrics
            ).toInt()
        )
        textView.setBackgroundResource(R.drawable.rect_corners_16dp)
        textView.background.setTint(0xf7ffffff.toInt())
        textView.foreground =
            ContextCompat.getDrawable(context, R.drawable.border_rect_1px_corners_16dp)
        textView.foreground.setTint(0x1a000000.toInt())
        toast.view = textView
        toast.show()
    }
}