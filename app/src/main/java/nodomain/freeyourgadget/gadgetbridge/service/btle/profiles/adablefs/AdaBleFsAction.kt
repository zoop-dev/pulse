/*  Copyright (C) 2025 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.adablefs

/**
 * Represents an action to perform on the Ada BLE filesystem.
 * Can be a file upload, delete, or other FS operations.
 */
data class AdaBleFsAction(
    val method: Method,
    val filenameorpath: String,
    val data: ByteArray = ByteArray(0),
    val secondFilenameorpath: String = ""
) {
    enum class Method {
        UPLOAD,
        DELETE,
        MAKE_DIRECTORY,
        MOVE,
        LIST_DIRECTORY
    }
}
