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

public val Icons.Outlined.ArrowCircleRight: ImageVector
    get() {
        if (_arrowCircleRight != null) {
            return _arrowCircleRight!!
        }
        _arrowCircleRight = materialIcon(name = "Outlined.ArrowCircleRight") {
            materialPath {
                moveTo(22.0f, 12.0f)
                curveToRelative(0.0f, -5.52f, -4.48f, -10.0f, -10.0f, -10.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                close()
                moveTo(4.0f, 12.0f)
                curveToRelative(0.0f, -4.42f, 3.58f, -8.0f, 8.0f, -8.0f)
                curveToRelative(4.42f, 0.0f, 8.0f, 3.58f, 8.0f, 8.0f)
                reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
                curveTo(7.58f, 20.0f, 4.0f, 16.42f, 4.0f, 12.0f)
                close()
                moveTo(16.0f, 12.0f)
                lineToRelative(-4.0f, 4.0f)
                lineToRelative(-1.41f, -1.41f)
                lineTo(12.17f, 13.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.17f)
                lineToRelative(-1.59f, -1.59f)
                lineTo(12.0f, 8.0f)
                lineTo(16.0f, 12.0f)
                close()
            }
        }
        return _arrowCircleRight!!
    }

private var _arrowCircleRight: ImageVector? = null
