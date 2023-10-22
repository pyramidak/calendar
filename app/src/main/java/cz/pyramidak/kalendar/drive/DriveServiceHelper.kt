package cz.pyramidak.kalendar.drive

import com.google.android.gms.drive.DriveFolder
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.api.client.http.FileContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DriveServiceHelper(private val mDriveService: Drive) {
    private val mExecutor: Executor = Executors.newSingleThreadExecutor()

    fun searchFile(fileName: String, folderID: String? = "root"): Task<List<GoogleDriveFileHolder>> {
        return Tasks.call(mExecutor) {
            val result = mDriveService.files().list()
                .setQ("name = '$fileName' and '$folderID' in parents and trashed=false")
                .setFields("files(id,name,size,createdTime,modifiedTime,starred)")
                .setSpaces("drive").execute()
            val files = mutableListOf<GoogleDriveFileHolder>()
            result.files.forEach {
                val googleDriveFileHolder = GoogleDriveFileHolder()
                googleDriveFileHolder.id = it.id
                googleDriveFileHolder.name = it.name
                googleDriveFileHolder.modifiedTime = it.modifiedTime
                googleDriveFileHolder.size = it.getSize()
                files.add(googleDriveFileHolder)
            }
            files
        }
    }


    fun searchFolder(folderName: String): Task<GoogleDriveFileHolder> {
        return Tasks.call(mExecutor) {
            // Retrive the metadata as a File object.
            val result = mDriveService.files().list()
                .setQ("mimeType = '" + DriveFolder.MIME_TYPE.toString() + "' and name = '" + folderName + "' ")
                .setSpaces("drive")
                .execute()
            val googleDriveFileHolder = GoogleDriveFileHolder()
            if (result.files.size > 0) {
                googleDriveFileHolder.id = result.files[0].id
                googleDriveFileHolder.name = result.files[0].name
            }
            googleDriveFileHolder
        }
    }

    fun queryFiles(folderId: String? = "root"): Task<List<GoogleDriveFileHolder>> {
        return Tasks.call<List<GoogleDriveFileHolder>>(mExecutor, Callable<List<GoogleDriveFileHolder>?> {
            val googleDriveFileHolderList: MutableList<GoogleDriveFileHolder> = ArrayList()
            val result = mDriveService.files().list().setQ("'$folderId' in parents and trashed=false")
                .setFields("files(id,name,size,createdTime,modifiedTime,starred)").setSpaces("drive").execute()
            for (i in result.files.indices) {
                val googleDriveFileHolder = GoogleDriveFileHolder()
                googleDriveFileHolder.id = result.files[i].id
                googleDriveFileHolder.name = result.files[i].name
                if (result.files[i].getSize() != null) {
                    googleDriveFileHolder.size = result.files[i].getSize()
                }
                if (result.files[i].modifiedTime != null) {
                    googleDriveFileHolder.modifiedTime = result.files[i].modifiedTime
                }
                if (result.files[i].createdTime != null) {
                    googleDriveFileHolder.createdTime = result.files[i].createdTime
                }
                if (result.files[i].starred != null) {
                    googleDriveFileHolder.starred = result.files[i].starred
                }
                googleDriveFileHolderList.add(googleDriveFileHolder)
            }
            googleDriveFileHolderList
        }
        )
    }

    fun createFolder(folderName: String?, folderId: String? = null): Task<GoogleDriveFileHolder> {
        return Tasks.call(mExecutor) {
            val googleDriveFileHolder = GoogleDriveFileHolder()
            val root: List<String> = if (folderId == null) {
                Collections.singletonList("root")
            } else {
                Collections.singletonList(folderId)
            }
            val metadata = File()
                .setParents(root)
                .setMimeType(DriveFolder.MIME_TYPE)
                .setName(folderName)
            val googleFile = mDriveService.files().create(metadata).execute()
                ?: throw IOException("Null result when requesting file creation.")
            googleDriveFileHolder.id = googleFile.id
            googleDriveFileHolder
        }
    }

    fun uploadFile(localFile: java.io.File,  folderId: String? = null, mimeType: String? = "text/plain"): Task<GoogleDriveFileHolder?> {
        return Tasks.call(mExecutor) { // Retrieve the metadata as a File object.
            val root: List<String> = if (folderId == null) {
                Collections.singletonList("root")
            } else {
                Collections.singletonList(folderId)
            }
            val metadata = File()
                .setParents(root)
                .setMimeType(mimeType)
                .setName(localFile.name)

            val fileContent = FileContent(mimeType, localFile)
            val fileMeta = mDriveService.files().create(metadata, fileContent).execute()
                ?: throw IOException("Null result when requesting file creation.")

            val googleDriveFileHolder = GoogleDriveFileHolder()
            googleDriveFileHolder.id = fileMeta.id
            googleDriveFileHolder.name = fileMeta.name
            googleDriveFileHolder
        }
    }

    fun downloadFile(targetFile: java.io.File?, fileId: String?): Task<Void?> {
        return Tasks.call(mExecutor) {
            // Retrieve the metadata as a File object.
            val outputStream: OutputStream = FileOutputStream(targetFile)
            mDriveService.files()[fileId].executeMediaAndDownloadTo(outputStream)
            null
        }
    }

    fun deleteFileOrFolder(files: List<GoogleDriveFileHolder>): Task<Void?> {
        return Tasks.call(mExecutor) {
            files.forEach { mDriveService.files().delete(it.id).execute() }
            null
        }
    }
}

