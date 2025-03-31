/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuaweiStressHRVCalculation {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiStressHRVCalculation.class);

    /*
       This file contain realization of mostly standard time-domain and frequency-domain HRV analysis methods.
       With parameters and restrictions specific for Huawei devices.
     */

    private int calculateAndRemoveOutliers(int[] rriData, byte[] outliers) {
        int dataSum = 0;
        int outlierCount = 0;

        for (int i = 0; i < rriData.length; i++) {
            if ((rriData[i] <= 400) || (rriData[i] >= 1400)) {
                outliers[i] = 1;
                outlierCount++;
            } else {
                dataSum = dataSum + rriData[i];
            }
        }
        if (rriData.length == outlierCount) {
            return -1;
        }
        int firstNotOutlierIndex = -1;
        float averageValue = (float) dataSum / (float) (rriData.length - outlierCount);
        for (int i = 0; i < rriData.length; i++) {
            if (outliers[i] == 0) {
                float currentRri = (float) rriData[i];
                if (currentRri <= averageValue * 1.2 && currentRri >= averageValue * 0.8) {
                    if (firstNotOutlierIndex < 0) {
                        firstNotOutlierIndex = i;
                    }
                } else {
                    outliers[i] = 1;
                }
            }
        }
        if (firstNotOutlierIndex < 0) {
            return -1;
        }
        int prevValue = rriData[firstNotOutlierIndex];
        for (int i = firstNotOutlierIndex + 1; i < rriData.length; i++) {
            if (outliers[i] == 0) {
                float currentRri = (float) rriData[i];
                if (currentRri <= prevValue * 0.8 || currentRri >= prevValue * 1.2) {
                    outliers[i] = 1;
                }
                prevValue = rriData[i];
            }
        }
        return firstNotOutlierIndex;
    }

    public static class TimeDomainData {
        public short medianNNICount = 0;
        public float sdHR = 0;
        public short rangeNNI = 0;
        public float rmsSD = 0;
        public short medianNNI = 0;
        public float normVar = 0;
        public short rriCount = 0;
        public float meanHR = 0;
        public float pNN50 = 0;
        public float sdNN = 0;
    }

    private TimeDomainData calculateTimeDomain(int[] rriData, int firstNotOutlierIndex, byte[] outliers) {

        TimeDomainData result = new TimeDomainData();

        int rriMax = rriData[firstNotOutlierIndex];
        int rriMin = rriData[firstNotOutlierIndex];

        int rri50Count = 0;
        int rriCount = 0;
        int rriDiffSum = 0;
        int rriDiffSqrtSum = 0;
        int rriSum = 0;
        float hrSum = 0;

        int previousValue = 0;
        boolean isFirstValue = true;
        for (int i = 0; i < rriData.length; i++) {
            if ((outliers[i] == 0) && (rriData[i] != 0)) {
                rriSum += rriData[i];
                hrSum += (60000.0F / (float) rriData[i]);
                rriMax = Math.max(rriData[i], rriMax);
                rriMin = Math.min(rriData[i], rriMin);
                if (!isFirstValue) {
                    rriCount++;
                    int diff = rriData[i] - previousValue;
                    rriDiffSum += diff;
                    if (Math.abs(diff) > 50) {
                        rri50Count++;
                    }
                    rriDiffSqrtSum += diff * diff;
                }
                previousValue = rriData[i];
                isFirstValue = false;
            }
        }

        if (rriCount == 0) {
            LOG.error("rriCount is zero");
            return null;
        }
        result.rriCount = (short) (rriCount + 1);
        result.meanHR = (hrSum / (float) (rriCount + 1));
        result.pNN50 = ((float) rri50Count / (float) rriCount) * 100.0F;

        int rriMinBy20 = (rriMin / 20) * 20;
        int rriMaxBy20 = (rriMax / 20) * 20;
        int categoriesNumber = ((rriMaxBy20 - rriMinBy20) / 20) + 1;
        if (categoriesNumber >= 50) {
            return null;
        }

        int[] categoriesBy20 = new int[categoriesNumber + 1];
        byte[] categoriesCount = new byte[categoriesNumber + 1];
        for (int i = 0; i <= categoriesNumber; i++) {
            categoriesBy20[i] = rriMinBy20 + (i * 20);
        }

        float hrDiffSqrtSum = 0.0F;
        float rrDiffSqrtSum = 0.0F;

        float avrRR = rriSum / (float) result.rriCount;
        for (int i = 0; i < rriData.length; i++) {
            if ((outliers[i] == 0) && (rriData[i] != 0)) {
                float rrDiff = (float) rriData[i] - avrRR;
                rrDiffSqrtSum += (rrDiff * rrDiff);
                double hrDiff = (60000.0F / (double) rriData[i]) - result.meanHR;
                hrDiffSqrtSum += (float) (hrDiff * hrDiff);
                for (int j = 0; j < categoriesNumber; j++) {
                    if ((categoriesBy20[j] <= rriData[i]) && (rriData[i] < categoriesBy20[j] + 20)) {
                        categoriesCount[j]++;
                        break;
                    }
                }
            }
        }
        result.sdHR = (float) Math.sqrt(hrDiffSqrtSum / (float) (rriCount + 1));
        result.sdNN = (float) Math.sqrt(rrDiffSqrtSum / (float) (rriCount + 1));
        result.rmsSD = (float) Math.sqrt((float) rriDiffSqrtSum / (float) rriCount);

        float avgDiffSqrt = (float) (rriDiffSum * rriDiffSum) / rriCount;
        float varSum = (float) (Math.sqrt(((float) rriDiffSqrtSum - avgDiffSqrt) / (float) rriCount) / 1.4142135F); // Math.sqrt(2) = 1.4142135F
        float varDiff = (rrDiffSqrtSum / (float) rriCount) * 2 - (varSum * varSum);
        if ((float) Math.sqrt(varDiff) == 0.0) {
            return null;
        }
        result.normVar = varSum / (float) Math.sqrt(varDiff);

        int sectionSum = 0;
        int sectionCount = 0;
        int maxValue = 0;
        for (int i = 0; i < categoriesNumber; i++) {
            if (categoriesCount[i] == 0) {
                continue;
            }
            if (categoriesCount[i] > maxValue) {
                maxValue = categoriesCount[i];
                sectionCount = 1;
                sectionSum = i;
                continue;
            }
            if (categoriesCount[i] == maxValue) {
                sectionCount++;
                sectionSum += i;
            }
        }
        if (sectionCount == 0) {
            return null;
        }
        int meanCategoriesIndex = (int) ((float) sectionSum / (float) sectionCount + 0.5);
        result.medianNNICount = categoriesCount[meanCategoriesIndex];
        result.medianNNI = (short) categoriesBy20[meanCategoriesIndex];
        result.rangeNNI = (short) (rriMaxBy20 - rriMinBy20);
        return result;
    }

    private int interpolateRriData(int[] rriData, int[] rriCumulativeSum, float baseValue, float[] rriResample, int maxSamples, byte[] outliers) {
        float targetValue = 0;
        float previousCumulative = 0;
        float lastRri = 0;
        int currIdx = 0;
        int resampleIdx = 0;
        for (int i = 0; i < rriData.length; i++) {
            if (outliers[i] != 0) {
                continue;
            }
            if (resampleIdx > 129) {
                return 0;
            }
            if (currIdx == 0) {
                rriResample[resampleIdx] = rriData[i];
                targetValue = (baseValue + 0.5F);
                previousCumulative = baseValue;
                lastRri = (float) rriData[i];
                resampleIdx++;
                currIdx = 1;
            } else {
                float currentCumulative = (float) (rriCumulativeSum[i] / 1000.0);
                float cumulativeDelta = currentCumulative - targetValue;
                if (cumulativeDelta >= -0.0001) {
                    if (currentCumulative == previousCumulative) {
                        return 0;
                    }
                    rriResample[resampleIdx] = (lastRri +
                            (((float) rriData[i] - lastRri) / (currentCumulative - previousCumulative)) *
                                    (targetValue - previousCumulative));
                    resampleIdx++;
                    currIdx++;
                    if (currIdx == maxSamples) {
                        break;
                    }
                    targetValue = baseValue + ((float) currIdx * 0.5F);
                    for (int j = currIdx; j < maxSamples; j++) {
                        if ((currentCumulative == previousCumulative) || (((currentCumulative - targetValue) < -0.0001)))
                            break;
                        if (resampleIdx > 129) {
                            return 0;
                        }
                        rriResample[resampleIdx] =
                                (lastRri + (((float) rriData[i] - lastRri) / (currentCumulative - previousCumulative)) *
                                        (targetValue - previousCumulative));

                        resampleIdx++;
                        currIdx++;
                        if (currIdx == maxSamples) break;
                        targetValue = baseValue + ((float) currIdx * 0.5F);
                    }
                    lastRri = (float) rriData[i];
                    previousCumulative = currentCumulative;
                    if (currIdx == maxSamples) break;
                } else if ((cumulativeDelta < -0.0001)) {
                    lastRri = rriData[i];
                    previousCumulative = currentCumulative;
                }
            }
        }
        return resampleIdx;
    }

    private boolean applyHanningWindow(float[] rriResample, int maxLimit, int length) {
        if ((length >= maxLimit) || (length == 0) || (rriResample == null) || (maxLimit <= 128)) {
            return false;
        }

        final int halfLength = length / 2;
        if ((halfLength >= maxLimit)) {
            return false;
        }

        final boolean isLengthEven = (length % 2) == 0;

        if (((length > 255) && isLengthEven) || ((length > 256) && (!isLengthEven))) {
            for (int n = 0; n < 128; n++) {
                double angle = ((double) (n + 1) * 2.0 * Math.PI) / (double) (length + 1);
                rriResample[n] = rriResample[n] * (float) ((1.0 - Math.cos(angle)) * 0.5);
            }
            return true;
        }
        if (halfLength != 0) {
            int n2 = length - 1;
            for (int n = 0; n < halfLength; n++) {
                double angle = ((double) (n + 1) * 2.0 * Math.PI) / (double) (length + 1);
                float window = (float) ((1.0 - Math.cos(angle)) * 0.5);
                rriResample[n] = rriResample[n] * window;
                if (n2 < 0) {
                    return true;
                }
                if (length < 129) {
                    rriResample[n2] = rriResample[n2] * window;
                } else {
                    if ((length - 128) <= n) {
                        rriResample[n2] = rriResample[n2] * window;
                    }
                }
                n2--;
            }
        }
        if (isLengthEven) {
            return true;
        }
        int midIndex = ((length + 1) / 2) - 1;
        if ((midIndex >= 0) && (midIndex < maxLimit)) {
            double angle = ((double) (midIndex + 1) * 2.0 * Math.PI) / (double) (length + 1);
            rriResample[midIndex] = (float) ((double) rriResample[midIndex] * (1.0 - Math.cos(angle)) * 0.5);
            return true;
        }
        return false;
    }

    private int prepareFrequencyDomainData(int firstNotOutlierIndex, int[] rriData, int[] rriCumulativeSum, float[] rriResample, byte[] outliers) {

        int baseSumValue = rriCumulativeSum[0];
        if (firstNotOutlierIndex > 0) {
            baseSumValue = rriCumulativeSum[firstNotOutlierIndex];
        }
        double timeSpan = (double) (rriCumulativeSum[rriCumulativeSum.length - 1] - baseSumValue) / 1000.0;

        int resampleSize = interpolateRriData(rriData, rriCumulativeSum, (float) (baseSumValue / 1000.0), rriResample, (int) (timeSpan * 2) + 1, outliers);
        if ((resampleSize == 0) || (resampleSize >= rriResample.length)) {
            LOG.error("interpolateRriData error");
            return 0;
        }

        // Detrending  data
        float sumValues = 0.0F;
        int sumSqrtIdx = 0;
        int sumIdx = 0;
        float sumIdxValues = 0.0F;
        for (int i = 0; i < resampleSize; i++) {
            sumIdx += i;
            sumSqrtIdx += (i * i);
            sumValues += rriResample[i];
            sumIdxValues += (i * rriResample[i]);
        }

        final float meanIndex = sumIdx / (float) resampleSize;
        float varianceTerm = sumSqrtIdx - ((float) resampleSize * meanIndex * meanIndex);
        if (varianceTerm == 0.0) {
            LOG.error("varianceTerm is zero");
            return 0;
        }

        final float meanValue = sumValues / (float) resampleSize;
        final float slope = (sumIdxValues - ((float) resampleSize * meanIndex * meanValue)) / varianceTerm;
        final float intercept = meanValue - (slope * meanIndex);

        // Apply detrending to data
        for (int i = 0; i < resampleSize; i++) {
            rriResample[i] -= (intercept + slope * i);
        }

        if (!applyHanningWindow(rriResample, rriResample.length, resampleSize)) {
            LOG.error("applyHanningWindow error");
            return 0;
        }
        return resampleSize;
    }

    public static class FrequencyDomainData {
        float freqStep;
        byte lfCount;
        byte vlfCount;
        byte hfCount;
        float totalPsd;
        float peakPower;
        float peakFrequency;
        float[] vlfSamples = new float[10];
        float[] hfSamples = new float[40];

        float vlfPsd;
        float hfPsd;
        float lfPsd;
        float lfHfVlfTotalPsd;

        int vlfPeakIndex;
        int hfPeakIndex;
    }


    private boolean transformComplexComponents(float[] fftData, int size, int stepSize) {
        if ((fftData == null) || (size >= 129)) {
            return false;
        }
        for (int i = 0; i < size; i += (stepSize * 2)) {
            float rP1 = fftData[i * 2];
            float iP1 = fftData[(i * 2) + (stepSize * 2)];
            float rP2 = fftData[(i * 2) + 1];
            float iP2 = fftData[(i * 2) + (stepSize * 2) + 1];
            fftData[(i * 2) + (stepSize * 2)] = rP1 - iP1;
            fftData[(i * 2) + (stepSize * 2) + 1] = rP2 - iP2;
            fftData[i * 2] = rP1 + iP1;
            fftData[(i * 2) + 1] = rP2 + iP2;
        }
        return true;
    }

    private boolean computeFFTStage(int stageIndex, int butterflyStep, float[] fftData, int maxIndex, float[] sinCos) {
        if ((maxIndex >= 129) || (fftData == null)) {
            return false;
        }
        int sinCosIdx = (stageIndex - 1) * 2;
        for (int i = 1; i < butterflyStep; i++) {
            for (int j = i; j < maxIndex; j += (butterflyStep * 2)) {
                float iPCurr = fftData[(j * 2) + 1];
                float iPNext = fftData[(j * 2) + (butterflyStep * 2) + 1];
                float rPCurr = fftData[j * 2];
                float rpNext = fftData[(j * 2) + (butterflyStep * 2)];
                float rDiff = rPCurr - rpNext;
                float iDiff = iPCurr - iPNext;
                fftData[(j * 2) + (butterflyStep * 2)] = ((sinCos[sinCosIdx] * rDiff) - (sinCos[sinCosIdx + 1] * iDiff));
                fftData[(j * 2) + (butterflyStep * 2) + 1] = (sinCos[sinCosIdx + 1] * rDiff) + (sinCos[sinCosIdx] * iDiff);
                fftData[j * 2] = rPCurr + rpNext;
                fftData[(j * 2) + 1] = iPCurr + iPNext;
            }
            sinCosIdx += (stageIndex * 2);
        }
        return true;
    }

    private void swapComplexComponents(float[] data, int indexA, int indexB) {
        float tempR = data[indexB * 2];
        float tempI = data[(indexB * 2) + 1];
        data[indexB * 2] = data[indexA * 2];
        data[(indexB * 2) + 1] = data[(indexA * 2) + 1];
        data[indexA * 2] = tempR;
        data[(indexA * 2) + 1] = tempI;
    }

    private boolean performFFT(float[] fftData, int currentSize) {
        int halfSize = currentSize / 2;
        int halfSize1 = halfSize - 1;

        if (currentSize == 0 || fftData == null || (halfSize * 2) == 0) {
            return false;
        }

        float[] sinCos = new float[halfSize1 * 2];

        double x = (Math.PI / (double) halfSize);
        float cosX = (float) Math.cos(x);
        float sinX = -(float) Math.sin(x);
        float cosCurr = cosX;
        float sinCurr = sinX;
        for (int i = 0; i < halfSize1; i++) {
            sinCos[i * 2] = cosCurr;
            sinCos[(i * 2) + 1] = sinCurr;
            float tmp = cosCurr * sinX;
            cosCurr = ((cosCurr * cosX) - (sinCurr * sinX));
            sinCurr = (sinCurr * cosX) + tmp;
        }

        // FFT decomposition
        int partitionSize = currentSize;
        int butterflyStep = 1;
        for (int stageCount = 0; stageCount < currentSize; stageCount++) {
            partitionSize /= 2;

            if (!transformComplexComponents(fftData, currentSize, partitionSize)) {
                return false;
            }
            if (!computeFFTStage(butterflyStep, partitionSize, fftData, currentSize, sinCos)) {
                return false;
            }
            if (partitionSize == 1) break;

            butterflyStep *= 2;
        }

        // Bit-reversal
        int swapBoundary = currentSize / 2;
        int sIdx = 0;

        for (int tIdx = 1; tIdx < currentSize - 1; tIdx++) {
            int n1 = swapBoundary;
            while (sIdx >= n1) {
                sIdx = sIdx - n1;
                n1 = n1 / 2;
            }
            sIdx = (n1 + sIdx);
            if (sIdx > tIdx) {
                swapComplexComponents(fftData, tIdx, sIdx);
            }
        }
        return true;
    }

    private boolean calculateFFT(float[] rriResample, int length) {

        final int maxLength = rriResample.length - 2;

        float[] fftData = new float[maxLength * 2];

        int curLen = Math.min(length, maxLength);

        int j = 0;
        for (int i = 0; i < (curLen * 2); i += 2) {
            fftData[i] = rriResample[j]; // real part
            fftData[i + 1] = 0.0F; // imagine part
            j++;
        }
        if (!performFFT(fftData, maxLength)) {
            LOG.error("performFFT error");
            return false;
        }

        Arrays.fill(rriResample, 0, length, 0);

        int maxHalfLength = (maxLength / 2) + 1;

        if (maxHalfLength == 0) {
            return false;
        }

        int n = 0;
        for (int i = 0; i < maxHalfLength; i++) {
            rriResample[i] = ((fftData[n + 1] * fftData[n + 1] + fftData[n] * fftData[n]) * 2) / ((float) maxLength * 0.5F);
            n += 2;
        }

        rriResample[0] = (fftData[1] * fftData[1] + fftData[0] * fftData[0]) / ((float) maxLength * 0.5F);
        int li = ((maxHalfLength - 1) * 2); //last index
        rriResample[maxHalfLength - 1] = (fftData[li + 1] * fftData[li + 1] + fftData[li] * fftData[li]) / ((float) maxLength * 0.5F);

        return true;
    }


    private boolean calculateFrequencyDomain(float[] rriResample, int length, FrequencyDomainData fdData) {

        // from 0.0 to 1.0 with freqStep
        fdData.freqStep = 0.015625F;
        for (int n = 0; n < 65; n++) {
            float currentFrequency = fdData.freqStep * n;
            rriResample[n] = (float) (((rriResample[n] * 64.0) / (float) length) * 0.5);
            fdData.totalPsd += fdData.freqStep * rriResample[n];

            if ((fdData.vlfCount >= fdData.vlfSamples.length) || (fdData.hfCount >= fdData.hfSamples.length)) {
                LOG.error("vlfCount or hfCount too big");
                return false;
            }
            // VLF: 0.0 - 0.04 Hz
            // LF: 0.04 - 0.15 Hz
            // HF: 0.15 - 0.4 Hz
            if (currentFrequency <= 0.4F) {
                fdData.lfHfVlfTotalPsd += fdData.freqStep * rriResample[n];

                if ((currentFrequency >= 0.0F) && (currentFrequency < 0.04F)) {
                    fdData.vlfPsd += fdData.freqStep * rriResample[n];
                    fdData.vlfSamples[fdData.vlfCount] = rriResample[n];
                    fdData.vlfCount++;
                } else if ((currentFrequency >= 0.04F) && (currentFrequency < 0.15F)) {
                    fdData.lfPsd += (fdData.freqStep * rriResample[n]);
                    fdData.lfCount++;
                } else if (currentFrequency >= 0.15F && currentFrequency <= 0.4F) {
                    fdData.hfPsd += fdData.freqStep * rriResample[n];
                    fdData.hfSamples[fdData.hfCount] = rriResample[n];
                    fdData.hfCount++;
                }

                if (currentFrequency >= 0.15F) {
                    if (fdData.peakPower < rriResample[n]) {
                        fdData.peakPower = rriResample[n];
                        fdData.peakFrequency = currentFrequency;
                    }
                } else {
                    fdData.peakPower = 0;
                    fdData.peakFrequency = 0;
                }
            }
        }

        if ((fdData.hfPsd == 0.0) || (fdData.totalPsd == 0.0) || (fdData.lfHfVlfTotalPsd == fdData.vlfPsd) || (fdData.lfHfVlfTotalPsd == 0.0)) {
            LOG.error("Invalid PSD values");
            return false;
        }
        return true;
    }

    private int processDataTrends(float[] data, int dataSize, int[] outputIndices, int maxOutputSize) {

        final int trendCount = dataSize - 1;
        int[] trends = new int[trendCount];

        // Detect initial trends
        for (int i = 0; i < trendCount; i++) {
            trends[i] = Float.compare(data[i], data[i + 1]); //(data[i] > data[i + 1]) ? 1 : (data[i] < data[i + 1]) ? -1 : 0;
        }

        // Process trend transitions
        int processedIndicesCount = 0;
        for (int i = 0; i < (dataSize - 2) && processedIndicesCount < maxOutputSize; i++) {
            final int currentTrend = trends[i];
            final int nextTrend = trends[i + 1];

            if (nextTrend > currentTrend) {
                trends[i] = 2;
            } else if (nextTrend != currentTrend) {
                trends[i] = 0;
            } else {
                trends[i] = 1;
            }

            if (trends[i] != 0) {
                outputIndices[processedIndicesCount++] = i + 1;
            }
        }

        return processedIndicesCount;
    }

    private int findDominantPeak(float[] samples, byte length) {
        if ((samples == null || length == 0)) {
            return -1;
        }

        float maxValue = 0.0F;
        int maxValueIndex = 0;
        for (int i = 0; i < length; i++) {
            if (samples[i] > maxValue) {
                maxValue = samples[i];
                maxValueIndex = (byte) i;
            }
        }
        if (length < 3) {
            return maxValueIndex;
        }

        int[] processedIndices = new int[40];
        int validIndicesCount = processDataTrends(samples, length, processedIndices, processedIndices.length);
        if (validIndicesCount < 0) {
            LOG.error("findDominantPeak processDataTrends error");
            return -1;
        }
        if (validIndicesCount == 0) {
            return maxValueIndex;
        }

        float peakValue = 0.0F;
        int peakIndex = 0;

        for (int i = 0; i < validIndicesCount; i++) {
            final int index = processedIndices[i];
            if (index >= length) {
                LOG.error( "Invalid index: {}", index);
                return -1;
            }
            if (samples[index] > peakValue) {
                peakIndex = index;
                peakValue = samples[index];
            }
        }
        return peakIndex;
    }


    private FrequencyDomainData calculateFrequencyDomainData(float[] rriResample, int length) {

        FrequencyDomainData result = new FrequencyDomainData();

        if (!calculateFFT(rriResample, length)) {
            LOG.error("calculateFFT error");
            return null;
        }
        if (!calculateFrequencyDomain(rriResample, length, result)) {
            LOG.error( "calculateFrequencyDomainParameters error");
            return null;
        }

        if (result.freqStep == 0.0) {
            LOG.error("Invalid frequency step");
            return null;
        }

        int peakEnd = (int) ((result.peakFrequency * 1.2) / result.freqStep);
        if ((peakEnd >= length) | (result.totalPsd == 0.0)) {
            LOG.error( "Invalid peak frequency");
            return null;
        }

        int vlfPeakIndex = findDominantPeak(result.vlfSamples, result.vlfCount);
        int hfPeakIndex = findDominantPeak(result.hfSamples, result.hfCount);
        if (hfPeakIndex < 0 || vlfPeakIndex < 0) {
            LOG.error("Peak detection failed (VLF: {}, HF: {})", vlfPeakIndex, hfPeakIndex);
            return null;
        }
        result.vlfPeakIndex = vlfPeakIndex;
        result.hfPeakIndex = hfPeakIndex;
        return result;
    }

    private FrequencyDomainData calculateFrequencyDomain(int[] rriData, int[] rriCumulativeSum, int firstNotOutlierIndex, byte[] outliers) {

        final float[] rriResample = new float[130];
        int resampleSize = prepareFrequencyDomainData(firstNotOutlierIndex, rriData, rriCumulativeSum, rriResample, outliers);
        if (resampleSize == 0) {
            LOG.error("prepareFrequencyDomainData error");
            return null;
        }

        return calculateFrequencyDomainData(rriResample, resampleSize);
    }


    private float[] calculateHRV(int[] rriData, int[] rriCumulativeSum) {

        byte[] outliers = new byte[rriData.length];

        int firstNotOutlierIndex = calculateAndRemoveOutliers(rriData, outliers);
        if (firstNotOutlierIndex < 0 || firstNotOutlierIndex > rriData.length) {
            LOG.error( "Error calculate outliers");
            return null;
        }

        TimeDomainData timeDomainData = calculateTimeDomain(rriData, firstNotOutlierIndex, outliers);
        if (timeDomainData == null) {
            LOG.error( "Time Domain error");
            return null;
        }

        FrequencyDomainData frequencyDomainData = calculateFrequencyDomain(rriData, rriCumulativeSum, firstNotOutlierIndex, outliers);
        if (frequencyDomainData == null) {
            LOG.error( "Frequency Domain error");
            return null;
        }

        float pVlf = (float) ((frequencyDomainData.vlfPsd / frequencyDomainData.lfHfVlfTotalPsd) * 100.0);
        float pLf = (float) ((frequencyDomainData.lfPsd / frequencyDomainData.lfHfVlfTotalPsd) * 100.0);

        boolean isMedianNNIInRange = timeDomainData.medianNNICount >= 0 && timeDomainData.medianNNICount <= 600;
        boolean isSDHRInRange = timeDomainData.sdHR >= 1.0 && timeDomainData.sdHR <= 20.0;
        boolean isRangeNNILessInRange = timeDomainData.rangeNNI >= 0 && timeDomainData.rangeNNI <= 1000;
        boolean isRMSDInRange = timeDomainData.rmsSD >= 0.0 && timeDomainData.rmsSD <= 200.0;
        boolean isNormalizedVarianceInRange = timeDomainData.normVar >= 0.0 && timeDomainData.normVar <= 2.0;
        boolean isPVlfInRange = pVlf >= 0.0 && pVlf <= 100.0;
        boolean isNNIDifferenceInRange = timeDomainData.medianNNI >= 300 && timeDomainData.medianNNI <= 1500;


        if (isMedianNNIInRange && isSDHRInRange && isRangeNNILessInRange
                && isRMSDInRange && isNNIDifferenceInRange && isNormalizedVarianceInRange && isPVlfInRange) {
            float[] result = new float[10];
            result[0] = timeDomainData.medianNNICount;
            result[1] = timeDomainData.sdHR;
            result[2] = timeDomainData.rangeNNI;
            result[3] = pVlf;
            result[4] = timeDomainData.rmsSD;
            result[5] = timeDomainData.medianNNI;
            result[6] = timeDomainData.normVar;
            result[7] = frequencyDomainData.hfPsd;
            result[8] = frequencyDomainData.lfPsd;
            result[9] = pLf;
            return result;
        }
        LOG.error( "validation fail");
        return null;
    }

    public float[] calculateStressHRVParameters(List<Integer> deviceRriData, List<Integer> deviceSqiData, int signalTime) {

        if ((signalTime <= 50) || (signalTime >= 70)) {
            return null;
        }

        if ((deviceRriData.size() < 30) || (deviceRriData.size() > 210) || (deviceRriData.size() > deviceSqiData.size())) {
            return null;
        }

        List<Integer> tmpRri = new ArrayList<>();
        List<Integer> tmpRriCumulativeSum = new ArrayList<>();

        int sum = 0;
        for (int i = 0; i < deviceRriData.size(); i++) {
            sum = sum + deviceRriData.get(i);
            if (deviceSqiData.get(i) == 100) {
                tmpRri.add(deviceRriData.get(i));
                tmpRriCumulativeSum.add(sum);
            }
        }

        if (tmpRri.isEmpty() || tmpRri.size() < 30) {
            LOG.error("No valid RRI signals");
            return null;
        }

        int total = 0;
        int validCount = 0;

        int[] filteredRri = new int[tmpRri.size()];
        int[] rriCumulativeSum = new int[tmpRriCumulativeSum.size()];
        for (int i = 0; i < tmpRri.size(); i++) {
            filteredRri[i] = tmpRri.get(i);
            rriCumulativeSum[i] = tmpRriCumulativeSum.get(i);

            if ((filteredRri[i] >= 400) && (filteredRri[i] <= 1400)) {
                total += filteredRri[i];
                validCount += 1;
            }
        }

        if (rriCumulativeSum[rriCumulativeSum.length - 1] > 65000) {
            LOG.error( "rriCumulativeSum is long");
            return null;
        }

        if ((validCount == 0) || (total == 0)) {
            LOG.error("validCount or total is 0");
            return null;
        }

        float sqi = (60.0F / (((float) total / validCount) / 1000.0F)) * ((float) signalTime / 60.0F);
        if (sqi < 0.2) {
            LOG.error("sqi is low");
            return null;
        }

        return calculateHRV(filteredRri, rriCumulativeSum);
    }
}
