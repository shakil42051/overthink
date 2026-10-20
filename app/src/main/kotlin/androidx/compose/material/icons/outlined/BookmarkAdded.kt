/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.BookmarkAdded: ImageVector
    get() {
        if (_bookmarkAdded != null) {
            return _bookmarkAdded!!
        }
        _bookmarkAdded = materialIcon(name = "Outlined.BookmarkAdded") {
            materialPath {
                moveTo(17.0f, 11.0f)
                verticalLineToRelative(6.97f)
                lineToRelative(-5.0f, -2.14f)
                lineToRelative(-5.0f, 2.14f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(6.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(7.0f)
                curveTo(5.9f, 3.0f, 5.0f, 3.9f, 5.0f, 5.0f)
                verticalLineToRelative(16.0f)
                lineToRelative(7.0f, -3.0f)
                lineToRelative(7.0f, 3.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(17.0f)
                close()
                moveTo(17.83f, 9.0f)
                lineTo(15.0f, 6.17f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(3.54f, -3.54f)
                lineToRelative(1.41f, 1.41f)
                lineTo(17.83f, 9.0f)
                close()
            }
        }
        return _bookmarkAdded!!
    }

private var _bookmarkAdded: ImageVector? = null
