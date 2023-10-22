package cz.pyramidak.kalendar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.WriterException
import cz.pyramidak.kalendar.databinding.ActivityQrcodeBinding


class QRcodeActivity : AppCompatActivity() {
    lateinit var binding: ActivityQrcodeBinding
    private val app: MyApplication
        get() = (application as MyApplication)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val codeQR = intent.extras!!.getString("CODE")
        val height = resources.displayMetrics.heightPixels
        val width = resources.displayMetrics.widthPixels

        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4

        val qrgEncoder = QRGEncoder(codeQR, null, QRGContents.Type.TEXT, dimen)
        try {
            binding.imgQRcode.setImageBitmap(qrgEncoder.getBitmap(0))
        } catch (e: WriterException) {
            Toast.makeText(this, "QR generator error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        binding.tvCode.text = codeQR

        binding.btnShare.setOnClickListener{
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + getString(R.string.qrcode))
            sharingIntent.putExtra(Intent.EXTRA_TEXT, codeQR)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)))
        }

        binding.btnClose.setOnClickListener{
            finish()
        }
    }




}

