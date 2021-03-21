package com.yuwin.fileconverterpro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.yuwin.fileconverterpro.Util.Companion.observeOnce
import com.yuwin.fileconverterpro.databinding.FragmentDashboardBinding
import com.yuwin.fileconverterpro.databinding.FragmentMainScreenBinding

private const val MEDIA_LOCATION_PERMISSION_REQUEST_CODE = 999
private const val IMAGE_REQUEST_CODE = 200
private const val PDF_REQUEST_CODE = 500

class DashboardFragment : Fragment() {

    private var convertAll = true
    private var convertInto = "png"
    private var mergePdf = false
    private var mergeImagesIntoPdf = false
    private var singleImageIntoPdf = false
    private var pdfIntoImages = false

    private val mainViewModel: MainViewModel by viewModels()
    var imgLimit = 5

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as MainActivity).requestInterstitial()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        _binding?.lifecycleOwner = viewLifecycleOwner

        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.imgtojpgcardView?.setOnClickListener {
            convertInto = ".jpeg"
            chooseImageIfPermissionGranted()
        }
        binding?.imgtopngcardView?.setOnClickListener {
            convertInto = ".png"
            chooseImageIfPermissionGranted()
        }
        binding?.imgtowebpcardView?.setOnClickListener {
            convertInto = ".webp"
            chooseImageIfPermissionGranted()
        }
        binding?.mergeImagestopdfcardView?.setOnClickListener {
            mergeImagesIntoPdf = true
            convertInto = ".pdf"
            chooseImageIfPermissionGranted()
        }
        binding?.imgtopdfcardView?.setOnClickListener {
            convertInto = ".pdf"
            singleImageIntoPdf = true
            chooseImageIfPermissionGranted()
        }
        binding?.pdfToPngCardView?.setOnClickListener {
            convertInto = ".png"
            pdfIntoImages = true
            choosePdfIfPermissionGranted()
        }
        binding?.pdfToWebpCardView?.setOnClickListener {
            convertInto = ".webp"
            pdfIntoImages = true
            choosePdfIfPermissionGranted()
        }
        binding?.pdfToJpgCardView?.setOnClickListener {
            convertInto = ".jpg"
            pdfIntoImages = true
            choosePdfIfPermissionGranted()
        }
        binding?.mergePdfcardView?.setOnClickListener {
            mergePdf = true
            convertInto = ".pdf"
            choosePdfIfPermissionGranted()
        }


    }

    private fun choosePdf() {
        val intent = Intent()
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        imgLimit = 2
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PDF_REQUEST_CODE)
    }

    private fun chooseImages() {
        mainViewModel.readIsPremium.observeOnce(this, {
            imgLimit = if (it == 1) {
                Constants.PREMIUM_IMAGE_LIMIT
            } else {
                Constants.FREE_IMAGE_LIMIT
            }
        })
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_REQUEST_CODE)
    }

    private fun choosePdfIfPermissionGranted() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            choosePdf()
        } else {
            if (isTherePermissionForMediaAccess(requireContext())) {
                choosePdf()
            } else {
                requestPermissionForMediaAccess(requireContext())
            }
        }
    }

    private fun chooseImageIfPermissionGranted() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            chooseImages()
        } else {
            if (isTherePermissionForMediaAccess(requireContext())) {
                chooseImages()
            } else {
                requestPermissionForMediaAccess(requireContext())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isTherePermissionForMediaAccess(context: Context): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        return permission == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermissionForMediaAccess(context: Context) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(
                android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            MEDIA_LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MEDIA_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var uriList = mutableListOf<Uri>()
        if (data != null && (requestCode == IMAGE_REQUEST_CODE || requestCode == PDF_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uriList.add(imageUri)
                }
            } else {
                val imageUri = data.data!!
                uriList.add(imageUri)
            }

            if (requestCode == PDF_REQUEST_CODE) {
                imgLimit = 2
            }

            if (uriList.size > imgLimit) {
                uriList = uriList.take(imgLimit).toMutableList()
                Toast.makeText(
                    requireContext(),
                    "Max Convert Limit $imgLimit reached",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

            val newUriList = UriList(uriList)


            val action = DashboardFragmentDirections.actionDashboardFragmentToConvert(
                newUriList,
                convertAll,
                convertInto,
                mergePdf,
                mergeImagesIntoPdf,
                singleImageIntoPdf,
                pdfIntoImages
            )
            findNavController().navigate(action)
        } else if (requestCode == IMAGE_REQUEST_CODE || requestCode == PDF_REQUEST_CODE) {
            findNavController().navigate(R.id.dashboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }

}