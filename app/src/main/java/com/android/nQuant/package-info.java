/**
 * All files under this package were adapted from https://github.com/mcychan/nQuant.android
 * at 12822f15c92695136f1761b6b72c9bd18dc70c46
 * <p>
 * Changes done:
 * - Update PnnQuantizer to allow for access to the palette after convertion
 * - Update PnnLABQuantizer and PnnQuantizer to allow instantiation from a Bitmap and not a file
 * - Update GilbertCurve to work on SDK21, due to usage of PriorityQueue, by making
 *   ErrorBox comparable
 * - Replace all Integer[] with int[]
 */
package com.android.nQuant;
