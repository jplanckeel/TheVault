package com.thevault.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object BrandIcons {

    /** PlayStation logo — Font Awesome 6 Free Brands fa-playstation (576×512). */
    fun PlayStation(color: Color): ImageVector =
        ImageVector.Builder(
            name = "PlayStation",
            defaultWidth = 576.dp,
            defaultHeight = 512.dp,
            viewportWidth = 576f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(color), pathFillType = PathFillType.NonZero) {
            // Path 1 — right pillar (P mark)
            moveTo(414.9f, 24.0f)
            curveTo(361.8f, 4.0f, 304.0f, -3.9f, 256.1f, 2.7f)
            curveTo(224.4f, 7.0f, 192.6f, 16.1f, 192.6f, 16.1f)
            lineTo(192.6f, 80.8f)
            lineTo(255.1f, 80.8f)
            lineTo(255.1f, 96.0f)
            lineTo(192.6f, 96.0f)
            lineTo(192.6f, 128.0f)
            lineTo(255.1f, 128.0f)
            lineTo(255.1f, 160.0f)
            lineTo(192.6f, 160.0f)
            lineTo(192.6f, 192.0f)
            lineTo(255.1f, 192.0f)
            lineTo(255.1f, 224.0f)
            lineTo(192.6f, 224.0f)
            lineTo(192.6f, 256.0f)
            lineTo(255.1f, 256.0f)
            lineTo(255.1f, 288.0f)
            lineTo(192.6f, 288.0f)
            lineTo(192.6f, 320.0f)
            lineTo(255.1f, 320.0f)
            lineTo(255.1f, 352.0f)
            lineTo(192.6f, 352.0f)
            lineTo(192.6f, 384.0f)
            lineTo(255.1f, 384.0f)
            lineTo(255.1f, 416.0f)
            lineTo(192.6f, 416.0f)
            lineTo(192.6f, 448.0f)
            lineTo(255.1f, 448.0f)
            lineTo(255.1f, 480.0f)
            lineTo(192.6f, 480.0f)
            lineTo(192.6f, 512.0f)
            lineTo(255.1f, 512.0f)
            lineTo(255.1f, 480.0f)
            lineTo(319.5f, 480.0f)
            curveTo(346.9f, 480.0f, 370.8f, 460.5f, 370.8f, 432.0f)
            lineTo(370.8f, 384.0f)
            curveTo(370.8f, 355.5f, 346.9f, 336.0f, 319.5f, 336.0f)
            lineTo(255.1f, 336.0f)
            lineTo(255.1f, 304.0f)
            lineTo(319.5f, 304.0f)
            curveTo(346.9f, 304.0f, 370.8f, 284.5f, 370.8f, 256.0f)
            lineTo(370.8f, 208.0f)
            curveTo(370.8f, 179.5f, 346.9f, 160.0f, 319.5f, 160.0f)
            lineTo(255.1f, 160.0f)
            lineTo(255.1f, 128.0f)
            lineTo(319.5f, 128.0f)
            curveTo(346.9f, 128.0f, 370.8f, 108.5f, 370.8f, 80.0f)
            lineTo(370.8f, 32.0f)
            curveTo(370.8f, 27.6f, 370.1f, 23.4f, 368.8f, 19.5f)
            curveTo(384.5f, 20.8f, 399.9f, 22.9f, 414.9f, 24.0f)
            close()
            // Path 2 — left pillar
            moveTo(128.0f, 0.0f)
            curveTo(57.3f, 0.0f, 0.0f, 57.3f, 0.0f, 128.0f)
            lineTo(0.0f, 384.0f)
            curveTo(0.0f, 454.7f, 57.3f, 512.0f, 128.0f, 512.0f)
            lineTo(192.6f, 512.0f)
            lineTo(192.6f, 0.0f)
            close()
            // Path 3 — right outer pillar
            moveTo(448.0f, 0.0f)
            lineTo(383.4f, 0.0f)
            lineTo(383.4f, 512.0f)
            lineTo(448.0f, 512.0f)
            curveTo(518.7f, 512.0f, 576.0f, 454.7f, 576.0f, 384.0f)
            lineTo(576.0f, 128.0f)
            curveTo(576.0f, 57.3f, 518.7f, 0.0f, 448.0f, 0.0f)
            close()
        }.build()

    /** Xbox logo — Font Awesome 6 Free Brands fa-xbox (512×512). Exact FA6 path. */
    fun Xbox(color: Color): ImageVector =
        ImageVector.Builder(
            name = "Xbox",
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 512f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(color), pathFillType = PathFillType.EvenOdd) {
            // FA6 fa-xbox — single compound path
            moveTo(369.9f, 97.9f)
            lineTo(256f, 128f)
            lineTo(142.1f, 97.9f)
            curveTo(130.3f, 108.4f, 119.7f, 120.2f, 110.5f, 133.1f)
            lineTo(215.9f, 303.4f)
            lineTo(153.3f, 399.8f)
            curveTo(163.1f, 425.1f, 178.4f, 447.7f, 197.7f, 466.2f)
            lineTo(256f, 384f)
            lineTo(314.3f, 466.2f)
            curveTo(333.6f, 447.7f, 348.9f, 425.1f, 358.7f, 399.8f)
            lineTo(296.1f, 303.4f)
            lineTo(401.5f, 133.1f)
            curveTo(392.3f, 120.2f, 381.7f, 108.4f, 369.9f, 97.9f)
            close()
            moveTo(256f, 0f)
            curveTo(114.6f, 0f, 0f, 114.6f, 0f, 256f)
            curveTo(0f, 397.4f, 114.6f, 512f, 256f, 512f)
            curveTo(397.4f, 512f, 512f, 397.4f, 512f, 256f)
            curveTo(512f, 114.6f, 397.4f, 0f, 256f, 0f)
            close()
            moveTo(256f, 48f)
            curveTo(302.6f, 48f, 346.2f, 61.4f, 383.2f, 84.5f)
            lineTo(270.7f, 176f)
            lineTo(256f, 144f)
            lineTo(241.3f, 176f)
            lineTo(128.8f, 84.5f)
            curveTo(165.8f, 61.4f, 209.4f, 48f, 256f, 48f)
            close()
            moveTo(48f, 256f)
            curveTo(48f, 191.4f, 71.9f, 132.3f, 112.3f, 87.3f)
            lineTo(208f, 320f)
            lineTo(112.3f, 424.7f)
            curveTo(71.9f, 379.7f, 48f, 320.6f, 48f, 256f)
            close()
            moveTo(256f, 464f)
            curveTo(209.4f, 464f, 165.8f, 450.6f, 128.8f, 427.5f)
            lineTo(197.7f, 338.2f)
            lineTo(256f, 416f)
            lineTo(314.3f, 338.2f)
            lineTo(383.2f, 427.5f)
            curveTo(346.2f, 450.6f, 302.6f, 464f, 256f, 464f)
            close()
            moveTo(399.7f, 424.7f)
            lineTo(304f, 320f)
            lineTo(399.7f, 87.3f)
            curveTo(440.1f, 132.3f, 464f, 191.4f, 464f, 256f)
            curveTo(464f, 320.6f, 440.1f, 379.7f, 399.7f, 424.7f)
            close()
        }.build()

    /** Nintendo "N" oval mark. */
    fun Nintendo(color: Color): ImageVector =
        ImageVector.Builder(
            name = "Nintendo",
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 512f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(color), pathFillType = PathFillType.EvenOdd) {
            moveTo(256f, 32f)
            curveTo(132.3f, 32f, 32f, 135.0f, 32f, 256f)
            curveTo(32f, 377.0f, 132.3f, 480f, 256f, 480f)
            curveTo(379.7f, 480f, 480f, 377.0f, 480f, 256f)
            curveTo(480f, 135.0f, 379.7f, 32f, 256f, 32f)
            close()
            moveTo(256f, 80f)
            curveTo(353.0f, 80f, 432f, 160.0f, 432f, 256f)
            curveTo(432f, 352.0f, 353.0f, 432f, 256f, 432f)
            curveTo(159.0f, 432f, 80f, 352.0f, 80f, 256f)
            curveTo(80f, 160.0f, 159.0f, 80f, 256f, 80f)
            close()
        }.path(fill = SolidColor(color), pathFillType = PathFillType.NonZero) {
            moveTo(168f, 160f)
            lineTo(168f, 352f)
            lineTo(208f, 352f)
            lineTo(208f, 224f)
            lineTo(304f, 352f)
            lineTo(344f, 352f)
            lineTo(344f, 160f)
            lineTo(304f, 160f)
            lineTo(304f, 288f)
            lineTo(208f, 160f)
            close()
        }.build()

    /** Sega stylised circle mark. */
    fun Sega(color: Color): ImageVector =
        ImageVector.Builder(
            name = "Sega",
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 512f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(color), pathFillType = PathFillType.EvenOdd) {
            moveTo(256f, 48f)
            curveTo(141.1f, 48f, 48f, 141.1f, 48f, 256f)
            curveTo(48f, 370.9f, 141.1f, 464f, 256f, 464f)
            curveTo(370.9f, 464f, 464f, 370.9f, 464f, 256f)
            curveTo(464f, 141.1f, 370.9f, 48f, 256f, 48f)
            close()
            moveTo(256f, 96f)
            curveTo(344.4f, 96f, 416f, 167.6f, 416f, 256f)
            curveTo(416f, 344.4f, 344.4f, 416f, 256f, 416f)
            curveTo(167.6f, 416f, 96f, 344.4f, 96f, 256f)
            curveTo(96f, 167.6f, 167.6f, 96f, 256f, 96f)
            close()
        }.path(fill = SolidColor(color), pathFillType = PathFillType.NonZero) {
            moveTo(310f, 152f)
            lineTo(190f, 152f)
            curveTo(168f, 152f, 150f, 170f, 150f, 192f)
            curveTo(150f, 214f, 168f, 232f, 190f, 232f)
            lineTo(310f, 232f)
            lineTo(310f, 272f)
            lineTo(150f, 272f)
            lineTo(150f, 312f)
            lineTo(322f, 312f)
            curveTo(344f, 312f, 362f, 294f, 362f, 272f)
            curveTo(362f, 250f, 344f, 232f, 322f, 232f)
            lineTo(202f, 232f)
            lineTo(202f, 192f)
            lineTo(362f, 192f)
            lineTo(362f, 152f)
            close()
        }.build()

    /** Generic gamepad icon (Material-style) — fallback. */
    fun Gamepad(color: Color): ImageVector =
        ImageVector.Builder(
            name = "Gamepad",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(fill = SolidColor(color), pathFillType = PathFillType.NonZero) {
            moveTo(15f, 7.5f)
            verticalLineTo(11f)
            horizontalLineTo(11.5f)
            verticalLineTo(13f)
            horizontalLineTo(15f)
            verticalLineTo(16.5f)
            horizontalLineTo(17f)
            verticalLineTo(13f)
            horizontalLineTo(20.5f)
            verticalLineTo(11f)
            horizontalLineTo(17f)
            verticalLineTo(7.5f)
            close()
            moveTo(7.5f, 11f)
            horizontalLineTo(5.5f)
            verticalLineTo(13f)
            horizontalLineTo(7.5f)
            verticalLineTo(15f)
            horizontalLineTo(9.5f)
            verticalLineTo(13f)
            horizontalLineTo(11.5f)
            verticalLineTo(11f)
            horizontalLineTo(9.5f)
            verticalLineTo(9f)
            horizontalLineTo(7.5f)
            close()
            moveTo(21f, 6f)
            horizontalLineTo(3f)
            curveTo(1.9f, 6f, 1f, 6.9f, 1f, 8f)
            verticalLineTo(16f)
            curveTo(1f, 17.1f, 1.9f, 18f, 3f, 18f)
            horizontalLineTo(21f)
            curveTo(22.1f, 18f, 23f, 17.1f, 23f, 16f)
            verticalLineTo(8f)
            curveTo(23f, 6.9f, 22.1f, 6f, 21f, 6f)
            close()
            moveTo(21f, 16f)
            horizontalLineTo(3f)
            verticalLineTo(8f)
            horizontalLineTo(21f)
            close()
        }.build()

    /** Amazon icon. */
    fun Amazon(brandColor: Color, accentColor: Color): ImageVector =
        ImageVector.Builder(
            name = "Amazon",
            defaultWidth = 448.dp,
            defaultHeight = 512.dp,
            viewportWidth = 448f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(brandColor), pathFillType = PathFillType.EvenOdd) {
            moveTo(257.20f, 162.70f)
            curveTo(208.50f, 164.50f, 87.70f, 178.20f, 87.70f, 280.20f)
            curveTo(87.70f, 389.70f, 226.00f, 394.20f, 271.20f, 323.40f)
            curveTo(277.70f, 333.60f, 306.60f, 360.90f, 316.50f, 370.20f)
            lineTo(373.30f, 314.20f)
            curveTo(373.30f, 314.20f, 341.00f, 288.90f, 341.00f, 261.40f)
            verticalLineTo(114.30f)
            curveTo(341.00f, 89.00f, 316.50f, 32.00f, 228.70f, 32.00f)
            curveTo(140.70f, 32.00f, 94.00f, 87.00f, 94.00f, 136.30f)
            lineTo(167.50f, 143.10f)
            curveTo(183.80f, 93.60f, 221.70f, 93.60f, 221.70f, 93.60f)
            curveTo(262.40f, 93.50f, 257.20f, 123.40f, 257.20f, 162.70f)
            close()
            moveTo(257.20f, 249.50f)
            curveTo(257.20f, 329.50f, 173.00f, 317.50f, 173.00f, 266.70f)
            curveTo(173.00f, 219.50f, 223.50f, 210.00f, 257.20f, 208.90f)
            verticalLineTo(249.50f)
            close()
        }.path(fill = SolidColor(accentColor), pathFillType = PathFillType.EvenOdd) {
            moveTo(393.20f, 413.00f)
            curveTo(385.50f, 423.00f, 323.20f, 480.00f, 218.70f, 480.00f)
            curveTo(114.20f, 480.00f, 34.20f, 408.50f, 9.70f, 379.00f)
            curveTo(2.90f, 371.30f, 10.70f, 367.70f, 15.20f, 370.70f)
            curveTo(88.50f, 415.20f, 203.00f, 488.50f, 387.70f, 401.00f)
            curveTo(395.20f, 397.30f, 401.00f, 403.00f, 393.20f, 413.00f)
            close()
            moveTo(433.00f, 415.20f)
            curveTo(426.50f, 431.00f, 417.00f, 442.00f, 411.80f, 446.20f)
            curveTo(406.30f, 450.70f, 402.30f, 448.90f, 405.30f, 442.40f)
            curveTo(408.30f, 435.90f, 424.60f, 395.90f, 418.00f, 387.40f)
            curveTo(411.50f, 379.10f, 381.00f, 383.10f, 370.00f, 384.20f)
            curveTo(359.20f, 385.20f, 357.00f, 386.20f, 356.00f, 383.90f)
            curveTo(353.70f, 378.20f, 377.70f, 368.40f, 393.50f, 366.40f)
            curveTo(409.20f, 364.60f, 434.50f, 365.60f, 439.50f, 372.10f)
            curveTo(443.20f, 377.20f, 439.50f, 399.20f, 433.00f, 415.20f)
            close()
        }.build()

    /** eBay icon. */
    fun Ebay(brandColor: Color, accentColor: Color): ImageVector =
        ImageVector.Builder(
            name = "Ebay",
            defaultWidth = 640.dp,
            defaultHeight = 512.dp,
            viewportWidth = 640f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(brandColor), pathFillType = PathFillType.EvenOdd) {
            moveTo(606.00f, 189.50f)
            lineTo(551.20f, 299.40f)
            lineTo(496.30f, 189.50f)
            horizontalLineTo(458.80f)
            lineTo(469.70f, 210.10f)
            curveTo(458.20f, 191.10f, 433.80f, 184.10f, 406.40f, 184.10f)
            curveTo(374.60f, 184.10f, 338.50f, 192.80f, 334.90f, 227.20f)
            horizontalLineTo(368.60f)
            curveTo(370.00f, 213.40f, 384.30f, 205.40f, 403.60f, 205.40f)
            curveTo(429.60f, 205.40f, 444.60f, 215.00f, 444.60f, 238.40f)
            verticalLineTo(241.80f)
            curveTo(431.90f, 241.80f, 416.60f, 241.90f, 402.90f, 242.20f)
            curveTo(360.50f, 243.10f, 333.30f, 252.20f, 326.20f, 276.60f)
            curveTo(327.20f, 271.40f, 327.70f, 266.00f, 327.70f, 260.40f)
            curveTo(327.70f, 208.30f, 288.00f, 184.20f, 252.30f, 184.20f)
            curveTo(231.00f, 184.20f, 209.30f, 189.70f, 193.60f, 208.40f)
            verticalLineTo(127.80f)
            horizontalLineTo(161.50f)
            verticalLineTo(297.30f)
            curveTo(161.50f, 307.60f, 160.90f, 320.20f, 160.40f, 330.40f)
            horizontalLineTo(191.90f)
            curveTo(192.60f, 324.10f, 193.00f, 317.50f, 193.00f, 310.90f)
            curveTo(206.60f, 327.50f, 228.40f, 335.80f, 251.70f, 335.80f)
            curveTo(288.60f, 335.80f, 316.60f, 313.90f, 325.00f, 281.60f)
            curveTo(324.50f, 284.40f, 324.30f, 287.40f, 324.30f, 290.60f)
            curveTo(324.30f, 314.70f, 345.40f, 335.60f, 384.90f, 335.60f)
            curveTo(411.50f, 335.60f, 430.70f, 329.90f, 446.80f, 310.10f)
            curveTo(446.80f, 316.70f, 447.10f, 323.40f, 447.90f, 330.30f)
            horizontalLineTo(477.70f)
            curveTo(477.00f, 322.10f, 476.70f, 312.80f, 476.70f, 303.50f)
            verticalLineTo(237.90f)
            curveTo(476.70f, 228.60f, 475.00f, 220.70f, 471.90f, 214.10f)
            lineTo(533.40f, 330.20f)
            lineTo(504.90f, 384.30f)
            horizontalLineTo(540.80f)
            lineTo(640.00f, 189.50f)
            close()
            moveTo(243.70f, 313.80f)
            curveTo(214.10f, 313.80f, 193.50f, 292.30f, 193.50f, 260.00f)
            curveTo(193.50f, 227.60f, 214.10f, 206.20f, 243.70f, 206.20f)
            curveTo(273.50f, 206.20f, 293.90f, 227.60f, 293.90f, 260.00f)
            curveTo(293.90f, 292.30f, 273.50f, 313.80f, 243.70f, 313.80f)
            close()
            moveTo(444.60f, 266.50f)
            curveTo(444.60f, 296.50f, 426.70f, 314.90f, 393.00f, 314.90f)
            curveTo(367.90f, 314.90f, 358.00f, 301.50f, 358.00f, 289.10f)
            curveTo(358.00f, 270.00f, 376.10f, 264.70f, 405.20f, 263.80f)
            curveTo(418.30f, 263.30f, 432.80f, 263.20f, 444.60f, 263.20f)
            close()
            moveTo(32.70f, 268.10f)
            horizontalLineTo(161.50f)
            verticalLineTo(259.60f)
            curveTo(161.50f, 207.90f, 128.40f, 184.20f, 83.10f, 184.20f)
            curveTo(26.30f, 184.20f, 0.10f, 215.00f, 0.10f, 261.80f)
            curveTo(0.10f, 304.30f, 25.40f, 335.80f, 82.60f, 335.80f)
            curveTo(114.00f, 335.80f, 150.60f, 324.10f, 157.00f, 289.70f)
            horizontalLineTo(123.90f)
            curveTo(111.90f, 325.50f, 36.20f, 326.40f, 32.70f, 268.10f)
            close()
            moveTo(127.70f, 246.70f)
            horizontalLineTo(33.30f)
            curveTo(40.20f, 190.10f, 125.40f, 192.00f, 127.70f, 246.70f)
            close()
        }.build()

    /** YouTube icon. */
    fun YouTube(brandColor: Color, accentColor: Color): ImageVector =
        ImageVector.Builder(
            name = "YouTube",
            defaultWidth = 576.dp,
            defaultHeight = 512.dp,
            viewportWidth = 576f,
            viewportHeight = 512f,
        ).path(fill = SolidColor(brandColor), pathFillType = PathFillType.EvenOdd) {
            moveTo(549.65f, 124.08f)
            curveTo(543.37f, 100.43f, 524.87f, 81.81f, 501.37f, 75.49f)
            curveTo(458.78f, 64.00f, 288.00f, 64.00f, 288.00f, 64.00f)
            curveTo(288.00f, 64.00f, 117.22f, 64.00f, 74.63f, 75.49f)
            curveTo(51.13f, 81.81f, 32.63f, 100.43f, 26.35f, 124.08f)
            curveTo(14.93f, 166.95f, 14.93f, 256.39f, 14.93f, 256.39f)
            curveTo(14.93f, 256.39f, 14.93f, 345.83f, 26.35f, 388.69f)
            curveTo(32.63f, 412.34f, 51.13f, 430.19f, 74.63f, 436.51f)
            curveTo(117.22f, 448.00f, 288.00f, 448.00f, 288.00f, 448.00f)
            curveTo(288.00f, 448.00f, 458.78f, 448.00f, 501.37f, 436.51f)
            curveTo(524.87f, 430.19f, 543.37f, 412.34f, 549.65f, 388.69f)
            curveTo(561.07f, 345.83f, 561.07f, 256.39f, 561.07f, 256.39f)
            curveTo(561.07f, 256.39f, 561.07f, 166.95f, 549.65f, 124.08f)
            close()
            moveTo(232.14f, 337.59f)
            verticalLineTo(175.19f)
            lineTo(374.88f, 256.39f)
            lineTo(232.15f, 337.59f)
            close()
        }.build()
}
