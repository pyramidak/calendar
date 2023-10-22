package cz.pyramidak.kalendar.drive

import com.google.android.gms.drive.DriveFolder
import com.google.api.client.util.DateTime
import java.time.LocalDateTime

var TYPE_AUDIO = "application/vnd.google-apps.audio"
var TYPE_GOOGLE_DOCS = "application/vnd.google-apps.document"
var TYPE_GOOGLE_DRAWING = "application/vnd.google-apps.drawing"
var TYPE_GOOGLE_DRIVE_FILE = "application/vnd.google-apps.file"
var TYPE_GOOGLE_DRIVE_FOLDER: String = DriveFolder.MIME_TYPE
var TYPE_GOOGLE_FORMS = "application/vnd.google-apps.form"
var TYPE_GOOGLE_FUSION_TABLES = "application/vnd.google-apps.fusiontable"
var TYPE_GOOGLE_MY_MAPS = "application/vnd.google-apps.map"
var TYPE_PHOTO = "application/vnd.google-apps.photo"
var TYPE_GOOGLE_SLIDES = "application/vnd.google-apps.presentation"
var TYPE_GOOGLE_APPS_SCRIPTS = "application/vnd.google-apps.script"
var TYPE_GOOGLE_SITES = "application/vnd.google-apps.site"
var TYPE_GOOGLE_SHEETS = "application/vnd.google-apps.spreadsheet"
var TYPE_UNKNOWN = "application/vnd.google-apps.unknown"
var TYPE_VIDEO = "application/vnd.google-apps.video"
var TYPE_3_RD_PARTY_SHORTCUT = "application/vnd.google-apps.drive-sdk"

class GoogleDriveFileHolder {
    var id: String? = null
    var name: String? = null
    var modifiedTime: DateTime? = null
    var size: Long = 0
    var createdTime: DateTime? = null
    var starred: Boolean? = null
}

